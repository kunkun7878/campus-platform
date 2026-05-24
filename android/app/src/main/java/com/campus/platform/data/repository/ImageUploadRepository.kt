package com.campus.platform.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.campus.platform.data.auth.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * 图片压缩+上传 Supabase Storage 仓库。
 *
 * 上传流程: Uri → 压缩(1200px/75%JPEG) → 生成存储路径 → upload → signedUrl
 * 存储路径规范: {school_id}/{resource_id}_{random6}.jpg
 * 签名 URL 有效期: 30 天（满足帖子长期展示 + 聊天近期查看）
 *
 * 安全:
 * - 路径含 school_id，通过 Storage RLS 实现学校隔离
 * - 仅同校用户可读取本校目录下的图片
 */
@Singleton
class ImageUploadRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) {

    /** 单张上传: 压缩 → 上传 → 返回 signedUrl */
    suspend fun uploadImage(
        uri: Uri,
        bucket: String,
        resourceId: String,
    ): String {
        val bytes = compressImage(uri)
        val path = buildPath(bucket, resourceId)
        val signedUrl = uploadToStorage(bucket, path, bytes)
        Log.d(TAG, "uploadImage ok path=$path")
        return signedUrl
    }

    /** 批量上传: 逐张压缩+上传, 返回 signedUrl 列表 */
    suspend fun uploadImages(
        uris: List<Uri>,
        bucket: String,
        resourceIdPrefix: String,
    ): List<String> {
        return uris.mapIndexed { index, uri ->
            uploadImage(uri, bucket, "${resourceIdPrefix}_$index")
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Internal helpers
    // ═══════════════════════════════════════════════════════════

    /** 压缩图片: 最大边长 1200px, JPEG quality 75 */
    private suspend fun compressImage(uri: Uri): ByteArray =
        withContext(Dispatchers.IO) {
            // Decode bounds first to calculate inSampleSize
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val maxDimension = 1200
            val sampleSize = calculateInSampleSize(
                options.outWidth, options.outHeight, maxDimension
            )
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: throw IllegalStateException("无法读取图片: $uri")

            // Scale down further if still too large
            val scaled = scaleBitmap(bitmap, maxDimension)
            if (scaled !== bitmap) bitmap.recycle()

            // Compress to JPEG
            val output = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, output)
            scaled.recycle()

            output.toByteArray()
        }

    /** 生成存储路径: {schoolId}/{resourceId}_{random6}.jpg */
    private suspend fun buildPath(bucket: String, resourceId: String): String {
        val profile = authRepository.getProfile()
            ?: throw IllegalStateException("用户信息未加载")
        val schoolId = profile.schoolId
            ?: throw IllegalStateException("请先选择学校")
        val random6 = (100000..999999).random().toString()
        return "$schoolId/${resourceId}_$random6.jpg"
    }

    /** 上传 bytes 到 Supabase Storage 并返回 signedUrl */
    private suspend fun uploadToStorage(
        bucket: String,
        path: String,
        bytes: ByteArray,
    ): String {
        val bucketApi = supabase.storage.from(bucket)
        // Upload to private bucket — SDK handles auth via session
        bucketApi.upload(
            path = path,
            data = bytes,
        ) {
            upsert = false
        }

        // Create signed URL (30 days) for Coil loading without auth headers
        val signedUrl = bucketApi.createSignedUrl(
            path = path,
            expiresIn = 30.days,
        )
        return signedUrl
    }
}

// ═══════════════════════════════════════════════════════════════
// Top-level helpers
// ═══════════════════════════════════════════════════════════════

private const val TAG = "ImgUploadRepo"

/** 计算 BitmapFactory 的 inSampleSize 参数 */
private fun calculateInSampleSize(
    width: Int,
    height: Int,
    maxDimension: Int,
): Int {
    var sampleSize = 1
    if (width > maxDimension || height > maxDimension) {
        val halfW = width / 2
        val halfH = height / 2
        while ((halfW / sampleSize) >= maxDimension && (halfH / sampleSize) >= maxDimension) {
            sampleSize *= 2
        }
    }
    return sampleSize.coerceAtLeast(1)
}

/** 等比缩放到 maxDimension 以内 */
private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDimension && h <= maxDimension) return bitmap

    val scale = if (w > h) {
        maxDimension.toFloat() / w
    } else {
        maxDimension.toFloat() / h
    }
    val newW = (w * scale).toInt()
    val newH = (h * scale).toInt()
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
}
