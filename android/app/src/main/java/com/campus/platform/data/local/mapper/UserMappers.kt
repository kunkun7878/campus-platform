package com.campus.platform.data.local.mapper

import com.campus.platform.data.local.entity.NotificationEntity
import com.campus.platform.data.local.entity.ProfileEntity
import com.campus.platform.data.local.entity.UserAddressEntity
import com.campus.platform.data.local.entity.UserFavoriteEntity
import com.campus.platform.data.local.entity.WalletEntity
import com.campus.platform.data.local.entity.InviteCodeEntity
import com.campus.platform.data.local.entity.InviteRecordEntity
import com.campus.platform.data.local.entity.WalletTransactionEntity
import com.campus.platform.data.model.Profile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Profile <-> ProfileEntity ──────────────────────────────

fun Profile.toEntity(): ProfileEntity = ProfileEntity(
    id = id,
    phone = phone,
    email = email,
    emailVerifiedAt = emailVerifiedAt,
    nickname = nickname,
    avatarUrl = avatarUrl,
    schoolId = schoolId,
    campusId = campusId,
    isAgent = isAgent,
    status = status,
    runnerStatus = runnerStatus,
    inviteCode = inviteCode,
    referrerId = referrerId,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ProfileEntity.toDto(): Profile = Profile(
    id = id,
    phone = phone,
    email = email,
    emailVerifiedAt = emailVerifiedAt,
    nickname = nickname,
    avatarUrl = avatarUrl,
    schoolId = schoolId,
    campusId = campusId,
    isAgent = isAgent,
    status = status,
    runnerStatus = runnerStatus,
    inviteCode = inviteCode,
    referrerId = referrerId,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── Wallet DTO (simple, to be expanded in Android-002) ─────

@Serializable
data class WalletDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val balance: Int,
    @SerialName("frozen_balance") val frozenBalance: Int,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("updated_at") val updatedAt: String?,
)

fun WalletEntity.toDto(): WalletDto = WalletDto(
    id = id,
    userId = userId,
    balance = balance,
    frozenBalance = frozenBalance,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun WalletDto.toEntity(): WalletEntity = WalletEntity(
    id = id,
    userId = userId,
    balance = balance,
    frozenBalance = frozenBalance,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── UserAddress DTO (simple) ────────────────────────────────

data class UserAddressDto(
    val id: String,
    val userId: String,
    val label: String,
    val contactName: String,
    val contactPhone: String,
    val address: String,
    val isDefault: Boolean,
    val schoolId: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun UserAddressEntity.toDto(): UserAddressDto = UserAddressDto(
    id = id,
    userId = userId,
    label = label,
    contactName = contactName,
    contactPhone = contactPhone,
    address = address,
    isDefault = isDefault,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun UserAddressDto.toEntity(): UserAddressEntity = UserAddressEntity(
    id = id,
    userId = userId,
    label = label,
    contactName = contactName,
    contactPhone = contactPhone,
    address = address,
    isDefault = isDefault,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── UserFavorite DTO (simple) ───────────────────────────────

data class UserFavoriteDto(
    val id: String,
    val userId: String,
    val targetType: String,
    val targetId: String,
    val createdAt: String?,
)

fun UserFavoriteEntity.toDto(): UserFavoriteDto = UserFavoriteDto(
    id = id,
    userId = userId,
    targetType = targetType,
    targetId = targetId,
    createdAt = createdAt,
)

fun UserFavoriteDto.toEntity(): UserFavoriteEntity = UserFavoriteEntity(
    id = id,
    userId = userId,
    targetType = targetType,
    targetId = targetId,
    createdAt = createdAt,
)

// ── Notification DTO (simple) ───────────────────────────────

data class NotificationDto(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String?,
    val refType: String?,
    val refId: String?,
    val isRead: Boolean,
    val readAt: String?,
    val createdAt: String?,
    val priority: String = "normal",
    val pushSent: Boolean = false,
    val pushSentAt: String? = null,
)

fun NotificationEntity.toDto(): NotificationDto = NotificationDto(
    id = id,
    userId = userId,
    type = type,
    title = title,
    body = body,
    refType = refType,
    refId = refId,
    isRead = isRead,
    readAt = readAt,
    createdAt = createdAt,
    priority = priority,
    pushSent = pushSent,
    pushSentAt = pushSentAt,
)

fun NotificationDto.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    userId = userId,
    type = type,
    title = title,
    body = body,
    refType = refType,
    refId = refId,
    isRead = isRead,
    readAt = readAt,
    createdAt = createdAt,
    priority = priority,
    pushSent = pushSent,
    pushSentAt = pushSentAt,
)

// ── WalletTransaction DTO ────────────────────────────────────

data class WalletTransactionDto(
    val id: String,
    val userId: String,
    val type: String,
    val amount: Int,
    val balanceAfter: Int,
    val description: String?,
    val refType: String?,
    val refId: String?,
    val createdAt: String?,
    val walletId: String = "",
    val balanceBefore: Int = 0,
)

fun WalletTransactionEntity.toDto(): WalletTransactionDto = WalletTransactionDto(
    id = id,
    userId = userId,
    type = type,
    amount = amount,
    balanceAfter = balanceAfter,
    description = description,
    refType = refType,
    refId = refId,
    createdAt = createdAt,
    walletId = walletId,
    balanceBefore = balanceBefore,
)

fun WalletTransactionDto.toEntity(): WalletTransactionEntity = WalletTransactionEntity(
    id = id,
    userId = userId,
    type = type,
    amount = amount,
    balanceAfter = balanceAfter,
    description = description,
    refType = refType,
    refId = refId,
    createdAt = createdAt,
    walletId = walletId,
    balanceBefore = balanceBefore,
)

// ── InviteCode DTO ───────────────────────────────────────────

data class InviteCodeDto(
    val id: String,
    val userId: String,
    val code: String,
    val isActive: Boolean,
    val usageCount: Int = 0,
    val maxUses: Int = 100,
    val expiresAt: String? = null,
    val createdAt: String?,
    val updatedAt: String?,
)

fun InviteCodeEntity.toDto(): InviteCodeDto = InviteCodeDto(
    id = id,
    userId = userId,
    code = code,
    isActive = isActive,
    usageCount = usageCount,
    maxUses = maxUses,
    expiresAt = expiresAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun InviteCodeDto.toEntity(): InviteCodeEntity = InviteCodeEntity(
    id = id,
    userId = userId,
    code = code,
    isActive = isActive,
    usageCount = usageCount,
    maxUses = maxUses,
    expiresAt = expiresAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── InviteRecord DTO ─────────────────────────────────────────

data class InviteRecordDto(
    val id: String,
    val inviterId: String,
    val inviteeId: String,
    val code: String,
    val registeredAt: String? = null,
    val createdAt: String?,
)

fun InviteRecordEntity.toDto(): InviteRecordDto = InviteRecordDto(
    id = id,
    inviterId = inviterId,
    inviteeId = inviteeId,
    code = code,
    registeredAt = registeredAt,
    createdAt = createdAt,
)

fun InviteRecordDto.toEntity(): InviteRecordEntity = InviteRecordEntity(
    id = id,
    inviterId = inviterId,
    inviteeId = inviteeId,
    code = code,
    registeredAt = registeredAt,
    createdAt = createdAt,
)
