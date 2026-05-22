package com.campus.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runner_tasks")
data class RunnerTaskEntity(
    @PrimaryKey val id: String,
    val publisherId: String,
    val runnerId: String? = null,
    val type: String,
    val title: String,
    val description: String? = null,
    val pickupAddr: String? = null,
    val deliveryAddr: String? = null,
    val price: Int = 0,
    val tip: Int = 0,
    val status: String = STATUS_PUBLISHED,
    val deadline: String? = null,
    val schoolId: String,
    val images: String = "[]",
    val genderRestriction: String = GENDER_ANY,
    val autoCancelMinutes: Int = 20,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val TYPE_PICKUP = "pickup"
        const val TYPE_DELIVERY = "delivery"
        const val TYPE_PURCHASE = "purchase"
        const val TYPE_UNIVERSAL = "universal"
        const val STATUS_PUBLISHED = "published"
        const val STATUS_ASSIGNED = "assigned"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"
        const val GENDER_ANY = "any"
        const val GENDER_FEMALE_ONLY = "female_only"
        const val GENDER_MALE_ONLY = "male_only"
    }
}

@Entity(tableName = "runner_orders")
data class RunnerOrderEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val buyerId: String,
    val runnerId: String,
    val status: String = STATUS_ACCEPTED,
    val cancelReason: String? = null,
    val completedAt: String? = null,
    val expectedAt: String? = null,
    val schoolId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_DELIVERING = "delivering"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_AFTER_SALE = "after_sale"
    }
}

@Entity(tableName = "runner_reviews")
data class RunnerReviewEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val reviewerId: String,
    val revieweeId: String,
    val rating: Int,
    val comment: String? = null,
    val schoolId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Entity(tableName = "after_sales")
data class AfterSaleEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val requesterId: String,
    val type: String,
    val reason: String,
    val images: String = "[]",
    val status: String = STATUS_PENDING,
    val resultComment: String? = null,
    val schoolId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val TYPE_REFUND = "refund"
        const val TYPE_RETURN = "return"
        const val TYPE_COMPLAINT = "complaint"
        const val STATUS_PENDING = "pending"
        const val STATUS_PROCESSING = "processing"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_COMPLETED = "completed"
    }
}

@Entity(tableName = "runner_applications")
data class RunnerApplicationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val realName: String,
    val studentId: String,
    val phone: String,
    val reason: String? = null,
    val idCardFront: String? = null,
    val idCardBack: String? = null,
    val status: String = STATUS_PENDING,
    val reviewComment: String? = null,
    val reviewedBy: String? = null,
    val reviewedAt: String? = null,
    val schoolId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
    }
}
