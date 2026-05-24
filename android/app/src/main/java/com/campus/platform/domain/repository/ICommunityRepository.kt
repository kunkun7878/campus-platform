package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.CommunityCommentDto
import com.campus.platform.data.local.mapper.CommunityPostDto
import kotlinx.coroutines.flow.Flow

interface ICommunityRepository {

    // ── Posts ──────────────────────────────────────────────────

    fun getPostsBySchool(schoolId: String): Flow<List<CommunityPostDto>>

    fun getPostsBySection(schoolId: String, section: String): Flow<List<CommunityPostDto>>

    suspend fun getPostById(id: String): CommunityPostDto?

    fun getPostsByAuthor(userId: String): Flow<List<CommunityPostDto>>

    suspend fun createPost(post: CommunityPostDto)

    suspend fun updatePost(id: String, updates: Map<String, Any?>)

    // ── Comments ───────────────────────────────────────────────

    fun getCommentsByPostId(postId: String): Flow<List<CommunityCommentDto>>

    suspend fun createComment(comment: CommunityCommentDto)

    // ── Comment mutations ─────────────────────────────────────

    suspend fun deleteComment(commentId: String)

    // ── Likes ──────────────────────────────────────────────────

    suspend fun toggleLike(postId: String, userId: String): Boolean

    suspend fun isLiked(postId: String, userId: String): Boolean

    fun getLikesByPostId(postId: String): Flow<List<String>>

    // ── Moderation (EdgeFn) ────────────────────────────────────

    /**
     * 通过 community-moderation EdgeFn 发布帖子。
     * 返回 ModerationResult 描述审核结果（block / review / pass / error）。
     */
    suspend fun publishPostViaModeration(post: CommunityPostDto): ModerationResult

    /**
     * 通过 community-moderation EdgeFn 编辑帖子（重新审核）。
     * 返回 ModerationResult 描述审核结果（block / review / pass / error）。
     */
    suspend fun updatePostViaModeration(post: CommunityPostDto): ModerationResult

    /**
     * 通过 community-moderation EdgeFn 发布评论。
     * 返回 ModerationResult 描述审核结果（block / review / pass / error）。
     */
    suspend fun publishCommentViaModeration(comment: CommunityCommentDto): ModerationResult

    // ── Refresh ────────────────────────────────────────────────

    suspend fun refreshPosts(schoolId: String)

    suspend fun refreshComments(postId: String)
}

/**
 * 审核结果。
 * - Blocked: 被拒绝，携带拒绝原因
 * - Reviewing: 进入人工审核
 * - Passed: 直接发布成功
 */
sealed class ModerationResult {
    data class Blocked(val reason: String) : ModerationResult()
    data object Reviewing : ModerationResult()
    data class Passed(val postId: String) : ModerationResult()
    /** EdgeFn 不可重试失败（404/400），不降级直写。 */
    data class Error(val message: String) : ModerationResult()
}
