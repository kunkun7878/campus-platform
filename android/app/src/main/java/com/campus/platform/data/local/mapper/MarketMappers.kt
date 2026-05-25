package com.campus.platform.data.local.mapper

import com.campus.platform.data.local.entity.MarketListingEntity
import com.campus.platform.data.local.entity.MarketOrderEntity

// ── MarketListing DTO (simple, to be expanded in Android-002)

data class MarketListingDto(
    val id: String,
    val sellerId: String,
    val title: String,
    val description: String?,
    val price: Int,
    val originalPrice: Int?,
    val images: String,
    val category: String,
    val condition: String,
    val status: String,
    val schoolId: String,
    val isBargain: Boolean,
    val contact: String,
    val meetupLocation: String?,
    val favoriteCount: Int = 0,
    val createdAt: String?,
    val updatedAt: String?,
)

fun MarketListingEntity.toDto(): MarketListingDto = MarketListingDto(
    id = id,
    sellerId = sellerId,
    title = title,
    description = description,
    price = price,
    originalPrice = originalPrice,
    images = images,
    category = category,
    condition = condition,
    status = status,
    schoolId = schoolId,
    isBargain = isBargain,
    contact = contact,
    meetupLocation = meetupLocation,
    favoriteCount = favoriteCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun MarketListingDto.toEntity(): MarketListingEntity = MarketListingEntity(
    id = id,
    sellerId = sellerId,
    title = title,
    description = description,
    price = price,
    originalPrice = originalPrice,
    images = images,
    category = category,
    condition = condition,
    status = status,
    schoolId = schoolId,
    isBargain = isBargain,
    contact = contact,
    meetupLocation = meetupLocation,
    favoriteCount = favoriteCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── MarketOrder DTO (simple) ────────────────────────────────

data class MarketOrderDto(
    val id: String,
    val listingId: String,
    val buyerId: String,
    val sellerId: String,
    val status: String,
    val meetupLocation: String?,
    val completedAt: String?,
    val schoolId: String,
    val createdAt: String?,
    val updatedAt: String?,
)

fun MarketOrderEntity.toDto(): MarketOrderDto = MarketOrderDto(
    id = id,
    listingId = listingId,
    buyerId = buyerId,
    sellerId = sellerId,
    status = status,
    meetupLocation = meetupLocation,
    completedAt = completedAt,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun MarketOrderDto.toEntity(): MarketOrderEntity = MarketOrderEntity(
    id = id,
    listingId = listingId,
    buyerId = buyerId,
    sellerId = sellerId,
    status = status,
    meetupLocation = meetupLocation,
    completedAt = completedAt,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
