package com.campus.platform.data.local.mapper

import com.campus.platform.data.local.entity.AfterSaleEntity
import com.campus.platform.data.local.entity.AfterSaleTimelineEntity
import com.campus.platform.data.local.entity.OrderTimelineEntity
import com.campus.platform.data.local.entity.RunnerApplicationEntity
import com.campus.platform.data.local.entity.RunnerOrderEntity
import com.campus.platform.data.local.entity.RunnerReviewEntity
import com.campus.platform.data.local.entity.RunnerTaskEntity

// ── RunnerTask DTO (simple, to be expanded in Android-002) ─

data class RunnerTaskDto(
    val id: String,
    val publisherId: String,
    val runnerId: String?,
    val type: String,
    val title: String,
    val description: String?,
    val pickupAddr: String?,
    val deliveryAddr: String?,
    val price: Int,
    val tip: Int,
    val status: String,
    val deadline: String?,
    val schoolId: String,
    val images: String,
    val genderRestriction: String,
    val autoCancelMinutes: Int,
    val createdAt: String?,
    val updatedAt: String?,
)

fun RunnerTaskEntity.toDto(): RunnerTaskDto = RunnerTaskDto(
    id = id,
    publisherId = publisherId,
    runnerId = runnerId,
    type = type,
    title = title,
    description = description,
    pickupAddr = pickupAddr,
    deliveryAddr = deliveryAddr,
    price = price,
    tip = tip,
    status = status,
    deadline = deadline,
    schoolId = schoolId,
    images = images,
    genderRestriction = genderRestriction,
    autoCancelMinutes = autoCancelMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun RunnerTaskDto.toEntity(): RunnerTaskEntity = RunnerTaskEntity(
    id = id,
    publisherId = publisherId,
    runnerId = runnerId,
    type = type,
    title = title,
    description = description,
    pickupAddr = pickupAddr,
    deliveryAddr = deliveryAddr,
    price = price,
    tip = tip,
    status = status,
    deadline = deadline,
    schoolId = schoolId,
    images = images,
    genderRestriction = genderRestriction,
    autoCancelMinutes = autoCancelMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── RunnerOrder DTO (simple) ────────────────────────────────

data class RunnerOrderDto(
    val id: String,
    val taskId: String,
    val buyerId: String,
    val runnerId: String,
    val status: String,
    val cancelReason: String?,
    val completedAt: String?,
    val expectedAt: String?,
    val schoolId: String,
    val createdAt: String?,
    val updatedAt: String?,
)

fun RunnerOrderEntity.toDto(): RunnerOrderDto = RunnerOrderDto(
    id = id,
    taskId = taskId,
    buyerId = buyerId,
    runnerId = runnerId,
    status = status,
    cancelReason = cancelReason,
    completedAt = completedAt,
    expectedAt = expectedAt,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun RunnerOrderDto.toEntity(): RunnerOrderEntity = RunnerOrderEntity(
    id = id,
    taskId = taskId,
    buyerId = buyerId,
    runnerId = runnerId,
    status = status,
    cancelReason = cancelReason,
    completedAt = completedAt,
    expectedAt = expectedAt,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── RunnerReview DTO (simple) ───────────────────────────────

data class RunnerReviewDto(
    val id: String,
    val orderId: String,
    val reviewerId: String,
    val revieweeId: String,
    val rating: Int,
    val comment: String?,
    val schoolId: String,
    val createdAt: String?,
    val updatedAt: String?,
)

fun RunnerReviewEntity.toDto(): RunnerReviewDto = RunnerReviewDto(
    id = id,
    orderId = orderId,
    reviewerId = reviewerId,
    revieweeId = revieweeId,
    rating = rating,
    comment = comment,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun RunnerReviewDto.toEntity(): RunnerReviewEntity = RunnerReviewEntity(
    id = id,
    orderId = orderId,
    reviewerId = reviewerId,
    revieweeId = revieweeId,
    rating = rating,
    comment = comment,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── AfterSale DTO (simple) ──────────────────────────────────

data class AfterSaleDto(
    val id: String,
    val orderId: String,
    val requesterId: String,
    val type: String,
    val reason: String,
    val images: String,
    val status: String,
    val resultComment: String?,
    val schoolId: String,
    val createdAt: String?,
    val updatedAt: String?,
)

fun AfterSaleEntity.toDto(): AfterSaleDto = AfterSaleDto(
    id = id,
    orderId = orderId,
    requesterId = requesterId,
    type = type,
    reason = reason,
    images = images,
    status = status,
    resultComment = resultComment,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun AfterSaleDto.toEntity(): AfterSaleEntity = AfterSaleEntity(
    id = id,
    orderId = orderId,
    requesterId = requesterId,
    type = type,
    reason = reason,
    images = images,
    status = status,
    resultComment = resultComment,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── RunnerApplication DTO (simple) ──────────────────────────

data class RunnerApplicationDto(
    val id: String,
    val userId: String,
    val realName: String,
    val studentId: String,
    val phone: String,
    val reason: String?,
    val idCardFront: String?,
    val idCardBack: String?,
    val status: String,
    val reviewComment: String?,
    val reviewedBy: String?,
    val reviewedAt: String?,
    val schoolId: String,
    val createdAt: String?,
    val updatedAt: String?,
)

fun RunnerApplicationEntity.toDto(): RunnerApplicationDto = RunnerApplicationDto(
    id = id,
    userId = userId,
    realName = realName,
    studentId = studentId,
    phone = phone,
    reason = reason,
    idCardFront = idCardFront,
    idCardBack = idCardBack,
    status = status,
    reviewComment = reviewComment,
    reviewedBy = reviewedBy,
    reviewedAt = reviewedAt,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun RunnerApplicationDto.toEntity(): RunnerApplicationEntity = RunnerApplicationEntity(
    id = id,
    userId = userId,
    realName = realName,
    studentId = studentId,
    phone = phone,
    reason = reason,
    idCardFront = idCardFront,
    idCardBack = idCardBack,
    status = status,
    reviewComment = reviewComment,
    reviewedBy = reviewedBy,
    reviewedAt = reviewedAt,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── OrderTimeline DTO ──────────────────────────────────────

data class OrderTimelineDto(
    val id: String,
    val orderId: String,
    val event: String,
    val description: String?,
    val operatorId: String?,
    val schoolId: String,
    val createdAt: String?,
    val updatedAt: String? = null,
)

fun OrderTimelineEntity.toDto(): OrderTimelineDto = OrderTimelineDto(
    id = id,
    orderId = orderId,
    event = event,
    description = description,
    operatorId = operatorId,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun OrderTimelineDto.toEntity(): OrderTimelineEntity = OrderTimelineEntity(
    id = id,
    orderId = orderId,
    event = event,
    description = description,
    operatorId = operatorId,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── AfterSaleTimeline DTO ──────────────────────────────────

data class AfterSaleTimelineDto(
    val id: String,
    val afterSaleId: String,
    val event: String,
    val description: String?,
    val operatorId: String?,
    val schoolId: String,
    val createdAt: String?,
    val updatedAt: String? = null,
)

fun AfterSaleTimelineEntity.toDto(): AfterSaleTimelineDto = AfterSaleTimelineDto(
    id = id,
    afterSaleId = afterSaleId,
    event = event,
    description = description,
    operatorId = operatorId,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun AfterSaleTimelineDto.toEntity(): AfterSaleTimelineEntity = AfterSaleTimelineEntity(
    id = id,
    afterSaleId = afterSaleId,
    event = event,
    description = description,
    operatorId = operatorId,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
