package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.AnnouncementDto
import com.campus.platform.data.local.mapper.CouponDto
import com.campus.platform.data.local.mapper.FeedbackDto
import com.campus.platform.data.local.mapper.UserCouponDto
import kotlinx.coroutines.flow.Flow

interface IMiscRepository {

    // ── Announcements ──────────────────────────────────────────

    fun getAnnouncements(schoolId: String): Flow<List<AnnouncementDto>>

    suspend fun refreshAnnouncements(schoolId: String)

    // ── Coupons ────────────────────────────────────────────────

    fun getActiveCoupons(schoolId: String): Flow<List<CouponDto>>

    fun getUserCoupons(userId: String, status: String? = null): Flow<List<UserCouponDto>>

    suspend fun claimCoupon(userCoupon: UserCouponDto)

    suspend fun refreshCoupons(schoolId: String)

    suspend fun refreshUserCoupons(userId: String)

    // ── Feedback ───────────────────────────────────────────────

    fun getFeedbacks(userId: String): Flow<List<FeedbackDto>>

    suspend fun submitFeedback(feedback: FeedbackDto)
}
