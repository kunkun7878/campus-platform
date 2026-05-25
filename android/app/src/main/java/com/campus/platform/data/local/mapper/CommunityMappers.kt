package com.campus.platform.data.local.mapper

import com.campus.platform.data.local.entity.CommunityCommentEntity
import com.campus.platform.data.local.entity.CommunityPostEntity
import com.campus.platform.data.local.entity.GroupMemberEntity
import com.campus.platform.data.local.entity.OfficialGroupEntity
import com.campus.platform.data.local.entity.PostLikeEntity

// ── CommunityPost DTO (simple, to be expanded in Android-002)

data class CommunityPostDto(
    val id: String,
    val authorId: String,
    val section: String,
    val title: String,
    val content: String,
    val images: String,
    val likeCount: Int,
    val commentCount: Int,
    val isPinned: Boolean,
    val status: String,
    val schoolId: String,
    val viewCount: Int,
    val createdAt: String?,
    val updatedAt: String?,
    val reviewReason: String? = null,
)

fun CommunityPostEntity.toDto(): CommunityPostDto = CommunityPostDto(
    id = id,
    authorId = authorId,
    section = section,
    title = title,
    content = content,
    images = images,
    likeCount = likeCount,
    commentCount = commentCount,
    isPinned = isPinned,
    status = status,
    schoolId = schoolId,
    viewCount = viewCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reviewReason = reviewReason,
)

fun CommunityPostDto.toEntity(): CommunityPostEntity = CommunityPostEntity(
    id = id,
    authorId = authorId,
    section = section,
    title = title,
    content = content,
    images = images,
    likeCount = likeCount,
    commentCount = commentCount,
    isPinned = isPinned,
    status = status,
    schoolId = schoolId,
    viewCount = viewCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reviewReason = reviewReason,
)

// ── CommunityComment DTO (simple) ───────────────────────────

data class CommunityCommentDto(
    val id: String,
    val postId: String,
    val authorId: String,
    val parentId: String?,
    val content: String,
    val likeCount: Int,
    val status: String,
    val schoolId: String,
    val createdAt: String?,
    val updatedAt: String?,
    val reviewReason: String? = null,
)

fun CommunityCommentEntity.toDto(): CommunityCommentDto = CommunityCommentDto(
    id = id,
    postId = postId,
    authorId = authorId,
    parentId = parentId,
    content = content,
    likeCount = likeCount,
    status = status,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reviewReason = reviewReason,
)

fun CommunityCommentDto.toEntity(): CommunityCommentEntity = CommunityCommentEntity(
    id = id,
    postId = postId,
    authorId = authorId,
    parentId = parentId,
    content = content,
    likeCount = likeCount,
    status = status,
    schoolId = schoolId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reviewReason = reviewReason,
)

// ── OfficialGroup DTO (simple) ──────────────────────────────

data class OfficialGroupDto(
    val id: String,
    val name: String,
    val description: String?,
    val direction: String,
    val avatarUrl: String?,
    val memberCount: Int,
    val isPinned: Boolean,
    val schoolId: String,
    val createdBy: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun OfficialGroupEntity.toDto(): OfficialGroupDto = OfficialGroupDto(
    id = id,
    name = name,
    description = description,
    direction = direction,
    avatarUrl = avatarUrl,
    memberCount = memberCount,
    isPinned = isPinned,
    schoolId = schoolId,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun OfficialGroupDto.toEntity(): OfficialGroupEntity = OfficialGroupEntity(
    id = id,
    name = name,
    description = description,
    direction = direction,
    avatarUrl = avatarUrl,
    memberCount = memberCount,
    isPinned = isPinned,
    schoolId = schoolId,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── GroupMember DTO (simple) ────────────────────────────────

data class GroupMemberDto(
    val id: String,
    val groupId: String,
    val userId: String,
    val role: String,
    val joinedAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun GroupMemberEntity.toDto(): GroupMemberDto = GroupMemberDto(
    id = id,
    groupId = groupId,
    userId = userId,
    role = role,
    joinedAt = joinedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun GroupMemberDto.toEntity(): GroupMemberEntity = GroupMemberEntity(
    id = id,
    groupId = groupId,
    userId = userId,
    role = role,
    joinedAt = joinedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── PostLike DTO (simple) ───────────────────────────────────

data class PostLikeDto(
    val id: String,
    val postId: String,
    val userId: String,
    val createdAt: String?,
)

fun PostLikeEntity.toDto(): PostLikeDto = PostLikeDto(
    id = id,
    postId = postId,
    userId = userId,
    createdAt = createdAt,
)

fun PostLikeDto.toEntity(): PostLikeEntity = PostLikeEntity(
    id = id,
    postId = postId,
    userId = userId,
    createdAt = createdAt,
)
