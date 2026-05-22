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

    // ── Likes ──────────────────────────────────────────────────

    suspend fun toggleLike(postId: String, userId: String): Boolean

    suspend fun isLiked(postId: String, userId: String): Boolean

    fun getLikesByPostId(postId: String): Flow<List<String>>

    // ── Refresh ────────────────────────────────────────────────

    suspend fun refreshPosts(schoolId: String)

    suspend fun refreshComments(postId: String)
}
