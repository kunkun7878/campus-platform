package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.MiscDao
import com.campus.platform.data.local.mapper.AnnouncementDto
import com.campus.platform.data.local.mapper.CouponDto
import com.campus.platform.data.local.mapper.FeedbackDto
import com.campus.platform.data.local.mapper.UserCouponDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IMiscRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class AnnouncementApiDto(
    val id: String,
    val title: String,
    val content: String? = null,
    @SerialName("school_id") val schoolId: String? = null,
    @SerialName("published_by") val publishedBy: String,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    val priority: String = "normal",
    val status: String = "published",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class CouponApiDto(
    val id: String,
    val title: String,
    val type: String,
    val value: Int,
    @SerialName("min_amount") val minAmount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("used_count") val usedCount: Int = 0,
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("end_at") val endAt: String? = null,
    @SerialName("school_id") val schoolId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class UserCouponApiDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("coupon_id") val couponId: String,
    val status: String = "unused",
    @SerialName("used_at") val usedAt: String? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class FeedbackApiDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val content: String,
    val contact: String? = null,
    val images: String = "[]",
    val status: String = "pending",
    val reply: String? = null,
    @SerialName("school_id") val schoolId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun AnnouncementApiDto.toMapperDto() = AnnouncementDto(
    id, title, content, schoolId, publishedBy, isPinned, priority, status, createdAt, updatedAt,
)

private fun AnnouncementDto.toApiDto() = AnnouncementApiDto(
    id, title, content, schoolId, publishedBy, isPinned, priority, status, createdAt, updatedAt,
)

private fun CouponApiDto.toMapperDto() = CouponDto(
    id, title, type, value, minAmount, totalCount, usedCount,
    startAt, endAt, schoolId, isActive, createdAt, updatedAt,
)

private fun UserCouponApiDto.toMapperDto() = UserCouponDto(
    id, userId, couponId, status, usedAt, orderId, createdAt, updatedAt,
)

private fun FeedbackApiDto.toMapperDto() = FeedbackDto(
    id, userId, type, content, contact, images, status, reply,
    schoolId, createdAt, updatedAt,
)

private fun FeedbackDto.toApiDto() = FeedbackApiDto(
    id, userId, type, content, contact, images, status, reply,
    schoolId, createdAt, updatedAt,
)

@Singleton
class MiscRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val miscDao: MiscDao,
) : IMiscRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Announcements ──────────────────────────────────────────

    override fun getAnnouncements(schoolId: String): Flow<List<AnnouncementDto>> {
        scope.launch { refreshAnnouncements(schoolId) }
        return miscDao.getAnnouncementsBySchoolId(schoolId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun getAnnouncementById(id: String): AnnouncementDto? {
        return miscDao.getAnnouncementById(id)?.toDto()
    }

    override suspend fun refreshAnnouncements(schoolId: String) {
        try {
            val result = supabase.postgrest
                .from("announcements")
                .select { filter { or { eq("school_id", schoolId); filter("school_id", FilterOperator.IS, null) } } }
                .decodeList<AnnouncementApiDto>()
            miscDao.upsertAllAnnouncements(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }

    override suspend fun upsertAnnouncement(announcement: AnnouncementDto) {
        val apiDto = announcement.toApiDto()
        // Check if exists; Supabase Kotlin SDK doesn't have upsert with onConflict.
        // Use insert if new, update if existing.
        val existing = miscDao.getAnnouncementById(announcement.id)
        val result = if (existing != null) {
            supabase.postgrest
                .from("announcements")
                .update(apiDto) {
                    filter { eq("id", announcement.id) }
                    select()
                }
                .decodeSingle<AnnouncementApiDto>()
        } else {
            supabase.postgrest
                .from("announcements")
                .insert(apiDto) { select() }
                .decodeSingle<AnnouncementApiDto>()
        }
        miscDao.upsertAnnouncement(result.toMapperDto().toEntity())
    }

    override suspend fun deleteAnnouncement(announcementId: String) {
        supabase.postgrest
            .from("announcements")
            .delete { filter { eq("id", announcementId) } }
        val local = miscDao.getAnnouncementById(announcementId)
        if (local != null) {
            miscDao.upsertAnnouncement(local.copy(status = "deleted"))
        }
    }

    // ── Coupons ────────────────────────────────────────────────

    override fun getActiveCoupons(schoolId: String): Flow<List<CouponDto>> {
        scope.launch { refreshCoupons(schoolId) }
        return miscDao.getActiveCouponsBySchoolId(schoolId).map { it.map { e -> e.toDto() } }
    }

    override fun getUserCoupons(userId: String, status: String?): Flow<List<UserCouponDto>> {
        scope.launch { refreshUserCoupons(userId) }
        val daoFlow = if (status != null) {
            miscDao.getUserCouponsByStatus(userId, status)
        } else {
            miscDao.getAllUserCoupons(userId)
        }
        return daoFlow.map { it.map { e -> e.toDto() } }
    }

    override suspend fun claimCoupon(userCoupon: UserCouponDto) {
        val apiDto = userCoupon.copy(id = userCoupon.id.ifBlank { UUID.randomUUID().toString() })
        val api = UserCouponApiDto(
            id = apiDto.id, userId = userCoupon.userId, couponId = userCoupon.couponId,
            status = userCoupon.status, usedAt = userCoupon.usedAt,
            orderId = userCoupon.orderId, createdAt = userCoupon.createdAt,
            updatedAt = userCoupon.updatedAt,
        )
        supabase.postgrest
            .from("user_coupons")
            .insert(api)
        miscDao.upsertUserCoupon(api.toMapperDto().toEntity())
    }

    override suspend fun refreshCoupons(schoolId: String) {
        try {
            val result = supabase.postgrest
                .from("coupons")
                .select { filter { or { eq("school_id", schoolId); filter("school_id", FilterOperator.IS, null) } } }
                .decodeList<CouponApiDto>()
            miscDao.upsertAllCoupons(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }

    override suspend fun refreshUserCoupons(userId: String) {
        try {
            val result = supabase.postgrest
                .from("user_coupons")
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserCouponApiDto>()
            miscDao.upsertAllUserCoupons(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }

    // ── Feedback ───────────────────────────────────────────────

    override fun getFeedbacks(userId: String): Flow<List<FeedbackDto>> {
        return miscDao.getFeedbacksByUserId(userId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun submitFeedback(feedback: FeedbackDto) {
        val result = supabase.postgrest
            .from("feedbacks")
            .insert(feedback.toApiDto()) { select() }
            .decodeSingle<FeedbackApiDto>()
        miscDao.upsertFeedback(result.toMapperDto().toEntity())
    }
}
