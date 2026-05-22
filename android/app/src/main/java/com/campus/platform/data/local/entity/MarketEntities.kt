package com.campus.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "market_listings")
data class MarketListingEntity(
    @PrimaryKey val id: String,
    val sellerId: String,
    val title: String,
    val description: String? = null,
    val price: Int = 0,
    val originalPrice: Int? = null,
    val images: String = "[]",
    val category: String,
    val condition: String,
    val status: String = STATUS_ACTIVE,
    val schoolId: String,
    val isBargain: Boolean = true,
    val contact: String = "站内私信联系",
    val meetupLocation: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val CONDITION_BRAND_NEW = "brand_new"
        const val CONDITION_LIKE_NEW = "like_new"
        const val CONDITION_GOOD = "good"
        const val CONDITION_FAIR = "fair"
        const val CONDITION_POOR = "poor"
        const val STATUS_ACTIVE = "active"
        const val STATUS_RESERVED = "reserved"
        const val STATUS_SOLD = "sold"
        const val STATUS_CANCELLED = "cancelled"
    }
}

@Entity(tableName = "market_orders")
data class MarketOrderEntity(
    @PrimaryKey val id: String,
    val listingId: String,
    val buyerId: String,
    val sellerId: String,
    val status: String = STATUS_PENDING,
    val meetupLocation: String? = null,
    val completedAt: String? = null,
    val schoolId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"
    }
}
