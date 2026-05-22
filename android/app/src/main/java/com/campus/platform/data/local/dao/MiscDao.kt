package com.campus.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campus.platform.data.local.entity.AnnouncementEntity
import com.campus.platform.data.local.entity.CouponEntity
import com.campus.platform.data.local.entity.UserCouponEntity
import com.campus.platform.data.local.entity.FeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MiscDao {

    // ── Announcement ─────────────────────────────────────────

    @Upsert
    suspend fun upsertAnnouncement(announcement: AnnouncementEntity)

    @Upsert
    suspend fun upsertAllAnnouncements(announcements: List<AnnouncementEntity>)

    @Query("SELECT * FROM announcements WHERE schoolId IS NULL OR schoolId = :schoolId ORDER BY isPinned DESC, createdAt DESC")
    fun getAnnouncementsBySchoolId(schoolId: String): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE id = :id")
    suspend fun getAnnouncementById(id: String): AnnouncementEntity?

    @Query("DELETE FROM announcements WHERE createdAt < :before")
    suspend fun deleteAnnouncementsOlderThan(before: String)

    @Query("DELETE FROM announcements")
    suspend fun deleteAllAnnouncements()

    // ── Coupon ───────────────────────────────────────────────

    @Upsert
    suspend fun upsertCoupon(coupon: CouponEntity)

    @Upsert
    suspend fun upsertAllCoupons(coupons: List<CouponEntity>)

    @Query("SELECT * FROM coupons WHERE (schoolId IS NULL OR schoolId = :schoolId) AND isActive = 1 ORDER BY createdAt DESC")
    fun getActiveCouponsBySchoolId(schoolId: String): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons WHERE id = :id")
    suspend fun getCouponById(id: String): CouponEntity?

    @Query("DELETE FROM coupons")
    suspend fun deleteAllCoupons()

    // ── UserCoupon ───────────────────────────────────────────

    @Upsert
    suspend fun upsertUserCoupon(userCoupon: UserCouponEntity)

    @Upsert
    suspend fun upsertAllUserCoupons(userCoupons: List<UserCouponEntity>)

    @Query("SELECT * FROM user_coupons WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    fun getUserCouponsByStatus(userId: String, status: String): Flow<List<UserCouponEntity>>

    @Query("SELECT * FROM user_coupons WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllUserCoupons(userId: String): Flow<List<UserCouponEntity>>

    @Query("SELECT * FROM user_coupons WHERE id = :id")
    suspend fun getUserCouponById(id: String): UserCouponEntity?

    @Query("DELETE FROM user_coupons WHERE id = :id")
    suspend fun deleteUserCouponById(id: String)

    @Query("DELETE FROM user_coupons")
    suspend fun deleteAllUserCoupons()

    // ── Feedback ─────────────────────────────────────────────

    @Upsert
    suspend fun upsertFeedback(feedback: FeedbackEntity)

    @Query("SELECT * FROM feedbacks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getFeedbacksByUserId(userId: String): Flow<List<FeedbackEntity>>

    @Query("SELECT * FROM feedbacks WHERE id = :id")
    suspend fun getFeedbackById(id: String): FeedbackEntity?

    @Query("DELETE FROM feedbacks WHERE createdAt < :before")
    suspend fun deleteFeedbacksOlderThan(before: String)

    @Query("DELETE FROM feedbacks")
    suspend fun deleteAllFeedbacks()
}
