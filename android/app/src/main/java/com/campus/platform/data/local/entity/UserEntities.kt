package com.campus.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val phone: String? = null,
    val email: String? = null,
    val emailVerifiedAt: String? = null,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val schoolId: String? = null,
    val campusId: String? = null,
    val isAgent: Boolean = false,
    val status: Int = 0,
    val runnerStatus: String = ProfileEntity.RUNNER_STATUS_NONE,
    val inviteCode: String? = null,
    val referrerId: String? = null,
    val deletedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val RUNNER_STATUS_NONE = "none"
        const val RUNNER_STATUS_PENDING = "pending"
        const val RUNNER_STATUS_APPROVED = "approved"
        const val RUNNER_STATUS_REJECTED = "rejected"
        const val RUNNER_STATUS_SUSPENDED = "suspended"
        const val STATUS_NORMAL = 0
        const val STATUS_DISABLED = 1
        const val STATUS_DELETED = 2
    }
}

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val balance: Int = 0,
    val frozenBalance: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Entity(tableName = "user_addresses")
data class UserAddressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val label: String,
    val contactName: String,
    val contactPhone: String,
    val address: String,
    val isDefault: Boolean = false,
    val schoolId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val amount: Int,
    val balanceAfter: Int,
    val description: String? = null,
    val refType: String? = null,
    val refId: String? = null,
    val createdAt: String? = null,
) {
    companion object {
        const val TYPE_RECHARGE = "recharge"
        const val TYPE_WITHDRAW = "withdraw"
        const val TYPE_PAYMENT = "payment"
        const val TYPE_REFUND = "refund"
        const val TYPE_INCOME = "income"
    }
}

@Entity(tableName = "user_favorites")
data class UserFavoriteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val targetType: String,
    val targetId: String,
    val createdAt: String? = null,
) {
    companion object {
        const val TARGET_RUNNER_TASK = "runner_task"
        const val TARGET_MARKET_LISTING = "market_listing"
        const val TARGET_LOST_FOUND = "lost_found"
        const val TARGET_COMMUNITY_POST = "community_post"
    }
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String? = null,
    val refType: String? = null,
    val refId: String? = null,
    val isRead: Boolean = false,
    val readAt: String? = null,
    val createdAt: String? = null,
    val priority: String = "normal",
    val pushSent: Boolean = false,
    val pushSentAt: String? = null,
) {
    companion object {
        const val TYPE_ORDER_STATUS = "order_status"
        const val TYPE_REVIEW = "review"
        const val TYPE_SYSTEM = "system"
        const val TYPE_CHAT = "chat"
        const val TYPE_AFTER_SALE = "after_sale"
        const val TYPE_LOST_FOUND = "lost_found"
        const val TYPE_COMMUNITY = "community"
        const val TYPE_GROUP_CHAT = "group_chat"
    }
}

@Entity(tableName = "invite_codes")
data class InviteCodeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val code: String,
    val isActive: Boolean = true,
    val totalInvites: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Entity(tableName = "invite_records")
data class InviteRecordEntity(
    @PrimaryKey val id: String,
    val inviterId: String,
    val inviteeId: String,
    val inviteCodeId: String,
    val rewardStatus: String = REWARD_STATUS_PENDING,
    val rewardAmount: Int = 0,
    val createdAt: String? = null,
) {
    companion object {
        const val REWARD_STATUS_PENDING = "pending"
        const val REWARD_STATUS_GRANTED = "granted"
        const val REWARD_STATUS_EXPIRED = "expired"
    }
}
