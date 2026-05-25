package com.campus.platform.data.local.mapper

import com.campus.platform.data.local.entity.LostFoundClaimEntity
import com.campus.platform.data.local.entity.LostFoundItemEntity

// ── LostFoundItem DTO (simple, to be expanded in Android-002)

data class LostFoundItemDto(
    val id: String,
    val publisherId: String,
    val type: String,
    val title: String,
    val description: String?,
    val images: String,
    val location: String?,
    val lostDate: String?,
    val category: String,
    val status: String,
    val schoolId: String,
    val reward: Int,
    val contact: String,
    val createdAt: String?,
    val updatedAt: String?,
    val returnedAt: String? = null,
)

fun LostFoundItemEntity.toDto(): LostFoundItemDto = LostFoundItemDto(
    id = id,
    publisherId = publisherId,
    type = type,
    title = title,
    description = description,
    images = images,
    location = location,
    lostDate = lostDate,
    category = category,
    status = status,
    schoolId = schoolId,
    reward = reward,
    contact = contact,
    createdAt = createdAt,
    updatedAt = updatedAt,
    returnedAt = returnedAt,
)

fun LostFoundItemDto.toEntity(): LostFoundItemEntity = LostFoundItemEntity(
    id = id,
    publisherId = publisherId,
    type = type,
    title = title,
    description = description,
    images = images,
    location = location,
    lostDate = lostDate,
    category = category,
    status = status,
    schoolId = schoolId,
    reward = reward,
    contact = contact,
    createdAt = createdAt,
    updatedAt = updatedAt,
    returnedAt = returnedAt,
)

// ── LostFoundClaim DTO (simple) ─────────────────────────────

data class LostFoundClaimDto(
    val id: String,
    val itemId: String,
    val claimantId: String,
    val proofDescription: String?,
    val status: String,
    val schoolId: String,
    val resolvedAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun LostFoundClaimEntity.toDto(): LostFoundClaimDto = LostFoundClaimDto(
    id = id,
    itemId = itemId,
    claimantId = claimantId,
    proofDescription = proofDescription,
    status = status,
    schoolId = schoolId,
    resolvedAt = resolvedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun LostFoundClaimDto.toEntity(): LostFoundClaimEntity = LostFoundClaimEntity(
    id = id,
    itemId = itemId,
    claimantId = claimantId,
    proofDescription = proofDescription,
    status = status,
    schoolId = schoolId,
    resolvedAt = resolvedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
