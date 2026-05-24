package com.campus.platform.ui.viewmodel.community

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.UserPreferencesDataStore
import com.campus.platform.data.local.mapper.CommunityPostDto
import com.campus.platform.domain.repository.IImageUploadRepository
import com.campus.platform.domain.repository.ICommunityRepository
import com.campus.platform.domain.repository.ModerationResult
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG = "PostCreateVM"

@HiltViewModel
class PostCreateViewModel @Inject constructor(
    private val communityRepo: ICommunityRepository,
    private val authRepository: AuthRepository,
    private val prefs: UserPreferencesDataStore,
    private val imageUploadRepository: IImageUploadRepository,
) : ViewModel() {

    // ── 表单字段 ──

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _selectedSection = MutableStateFlow("campus_wall")
    val selectedSection: StateFlow<String> = _selectedSection.asStateFlow()

    // ── 图片选择 ──

    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    private val _isUploadingImages = MutableStateFlow(false)
    val isUploadingImages: StateFlow<Boolean> = _isUploadingImages.asStateFlow()

    // ── 发布状态 ──

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // 审核结果弹框
    private val _blockReason = MutableStateFlow<String?>(null)
    val blockReason: StateFlow<String?> = _blockReason.asStateFlow()

    // Toast 消息
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // 发布成功后的帖子 ID（用于导航到详情页）
    private val _publishedPostId = MutableStateFlow<String?>(null)
    val publishedPostId: StateFlow<String?> = _publishedPostId.asStateFlow()

    // ── Setters ──

    fun setTitle(value: String) {
        _title.value = value
    }

    fun setContent(value: String) {
        _content.value = value
    }

    fun setSection(value: String) {
        _selectedSection.value = value
    }

    fun addImages(uris: List<Uri>) {
        val current = _selectedImages.value.toMutableList()
        current.addAll(uris)
        _selectedImages.value = current.take(9)
    }

    fun removeImage(index: Int) {
        val current = _selectedImages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _selectedImages.value = current
        }
    }

    // ── 提交审核 ──

    fun submit() {
        val titleText = _title.value.trim()
        if (titleText.isBlank()) {
            _toastMessage.value = "请输入标题"
            return
        }
        val contentText = _content.value.trim()
        if (contentText.isBlank()) {
            _toastMessage.value = "请输入内容"
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _toastMessage.value = "请先登录"
                    return@launch
                }
                val schoolId = prefs.schoolId.first()
                if (schoolId.isNullOrBlank()) {
                    _toastMessage.value = "请先选择学校"
                    return@launch
                }

                // Upload images if any selected
                val imageUrls = mutableListOf<String>()
                val selectedUris = _selectedImages.value
                if (selectedUris.isNotEmpty()) {
                    _isUploadingImages.value = true
                    val postId = UUID.randomUUID().toString()
                    for ((index, uri) in selectedUris.withIndex()) {
                        try {
                            val url = imageUploadRepository.uploadImage(
                                uri = uri,
                                bucket = "community-images",
                                resourceId = "${postId}_$index",
                            )
                            imageUrls.add(url)
                        } catch (e: Exception) {
                            Log.e(TAG, "Upload image $index failed", e)
                        }
                    }
                    _isUploadingImages.value = false
                }

                val now = java.time.Instant.now().toString()
                val post = CommunityPostDto(
                    id = UUID.randomUUID().toString(),
                    authorId = userId,
                    section = _selectedSection.value,
                    title = titleText,
                    content = contentText,
                    images = encodeImageUrls(imageUrls),
                    likeCount = 0,
                    commentCount = 0,
                    isPinned = false,
                    status = "published",
                    schoolId = schoolId,
                    viewCount = 0,
                    createdAt = now,
                    updatedAt = now,
                )

                when (val result = communityRepo.publishPostViaModeration(post)) {
                    is ModerationResult.Blocked -> {
                        _blockReason.value = result.reason
                    }
                    is ModerationResult.Reviewing -> {
                        _toastMessage.value = "内容已提交审核"
                        // 提交后回到社区页
                        _publishedPostId.value = "" // 空字符串表示已发布但回退
                    }
                    is ModerationResult.Passed -> {
                        _publishedPostId.value = result.postId
                    }
                    is ModerationResult.Error -> {
                        _toastMessage.value = result.message
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "发布失败", e)
                _toastMessage.value = "发布失败，请重试"
            } finally {
                _isSubmitting.value = false
                _isUploadingImages.value = false
            }
        }
    }

    // ── Event consumers ──

    fun clearBlockReason() {
        _blockReason.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun onPublishedConsumed() {
        _publishedPostId.value = null
    }
}

/** 将图片 URL 列表编码为 JSON 数组字符串 */
private fun encodeImageUrls(urls: List<String>): String {
    if (urls.isEmpty()) return "[]"
    return urls.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }
}
