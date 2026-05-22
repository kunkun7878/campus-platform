package com.campus.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lost_found_items")
data class LostFoundItemEntity(
    @PrimaryKey val id: String,
    val publisherId: String,
    val type: String,
    val title: String,
    val description: String? = null,
    val images: String = "[]",
    val location: String? = null,
    val lostDate: String? = null,
    val category: String = "other",
    val status: String = STATUS_ACTIVE,
    val schoolId: String,
    val reward: Int = 0,
    val contact: String = "站内私信联系",
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val TYPE_LOST = "lost"
        const val TYPE_FOUND = "found"
        const val STATUS_ACTIVE = "active"
        const val STATUS_CLAIMED = "claimed"
        const val STATUS_CLOSED = "closed"
    }
}

@Entity(tableName = "lost_found_claims")
data class LostFoundClaimEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val claimantId: String,
    val proofDescription: String? = null,
    val status: String = STATUS_PENDING,
    val schoolId: String,
    val resolvedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
    }
}
