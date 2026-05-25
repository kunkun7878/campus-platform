package com.campus.platform.data.local.mapper

import com.campus.platform.data.local.entity.AnnouncementEntity
import com.campus.platform.data.local.entity.CouponEntity
import com.campus.platform.data.local.entity.FeedbackEntity
import com.campus.platform.data.local.entity.UserCouponEntity

// ── Announcement DTO (simple, to be expanded in Android-002)

data class AnnouncementDto(
    val id: String,
    val title: String,
    val content: String?,
    val schoolId: String?,
    val publishedBy: String,
    val isPinned: Boolean,
    val priority: String = "normal",
    val status: String = "published",
    val createdAt: String?,
    val updatedAt: String?,
)

fun AnnouncementEntity.toDto(): AnnouncementDto = AnnouncementDto(
    id = id,
    title = title,
    content = content,
    schoolId = schoolId,
    publishedBy = publishedBy,
    isPinned = isPinned,
    priority = priority,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun AnnouncementDto.toEntity(): AnnouncementEntity = AnnouncementEntity(
    id = id,
    title = title,
    content = content,
    schoolId = schoolId,
    publishedBy = publishedBy,
    isPinned = isPinned,
    priority = priority,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── Coupon DTO (simple) ─────────────────────────────────────

data class CouponDto(
    val id: String,
    val title: String,
    val type: String,
    val value: Int,
    val minAmount: Int,
    val totalCount: Int,
    val usedCount: Int,
    val startAt: String?,
    val endAt: String?,
    val schoolId: String?,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)

fun CouponEntity.toDto(): CouponDto = CouponDto(
    id = id,
    title = title,
    type = type,
    value = value,
    minAmount = minAmount,
    totalCount = totalCount,
    usedCount = usedCount,
    startAt = startAt,
    endAt = endAt,
    schoolId = schoolId,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CouponDto.toEntity(): CouponEntity = CouponEntity(
    id = id,
    title = title,
    type = type,
    value = value,
    minAmount = minAmount,
    totalCount = totalCount,
    usedCount = usedCount,
    startAt = startAt,
    endAt = endAt,
    schoolId = schoolId,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── UserCoupon DTO (simple) ─────────────────────────────────

data class UserCouponDto(
    val id: String,
    val userId: String,
    val couponId: String,
    val status: String,
    val usedAt: String?,
    val orderId: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun UserCouponEntity.toDto(): UserCouponDto = UserCouponDto(
    id = id,
    userId = userId,
    couponId = couponId,
    status = status,
    usedAt = usedAt,
    orderId = orderId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun UserCouponDto.toEntity(): UserCouponEntity = UserCouponEntity(
    id = id,
    userId = userId,
    couponId = couponId,
    status = status,
    usedAt = usedAt,
    orderId = orderId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── Feedback DTO (simple) ───────────────────────────────────

data class FeedbackDto(
    val id: String,
    val userId: String,
    val type: String,
    val content: String,
    val contact: String?,
    val images: String,
    val status: String,
    val reply: String?,
    val schoolId: String,
    val createdAt: String?,
    val updatedAt: String?,
)

fun FeedbackEntity.toDto(): FeedbackDto = FeedbackDto(
    id = id,
    userId = userId,
    type = type,
    content = content,
    contact = contact,
    images = images,
    status = status,
    reply = reply,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FeedbackDto.toEntity(): FeedbackEntity = FeedbackEntity(
    id = id,
    userId = userId,
    type = type,
    content = content,
    contact = contact,
    images = images,
    status = status,
    reply = reply,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
