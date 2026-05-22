package com.campus.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String? = null,
    val schoolId: String? = null,
    val publishedBy: String,
    val isPinned: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val value: Int,
    val minAmount: Int = 0,
    val totalCount: Int = 0,
    val usedCount: Int = 0,
    val startAt: String? = null,
    val endAt: String? = null,
    val schoolId: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val TYPE_FIXED = "fixed"
        const val TYPE_PERCENTAGE = "percentage"
    }
}

@Entity(tableName = "user_coupons")
data class UserCouponEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val couponId: String,
    val status: String = STATUS_UNUSED,
    val usedAt: String? = null,
    val orderId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val STATUS_UNUSED = "unused"
        const val STATUS_USED = "used"
        const val STATUS_EXPIRED = "expired"
    }
}

@Entity(tableName = "feedbacks")
data class FeedbackEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val content: String,
    val contact: String? = null,
    val images: String = "[]",
    val status: String = STATUS_PENDING,
    val reply: String? = null,
    val schoolId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val TYPE_BUG = "bug"
        const val TYPE_SUGGESTION = "suggestion"
        const val TYPE_COMPLAINT = "complaint"
        const val TYPE_OTHER = "other"
        const val STATUS_PENDING = "pending"
        const val STATUS_PROCESSING = "processing"
        const val STATUS_RESOLVED = "resolved"
        const val STATUS_CLOSED = "closed"
    }
}
