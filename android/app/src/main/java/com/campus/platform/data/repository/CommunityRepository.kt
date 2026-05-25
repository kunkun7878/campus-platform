package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.CommunityDao
import com.campus.platform.data.local.entity.PostLikeEntity
import com.campus.platform.data.local.mapper.CommunityCommentDto
import com.campus.platform.data.local.mapper.CommunityPostDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.ICommunityRepository
import com.campus.platform.domain.repository.ModerationResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class CommunityPostApiDto(
    val id: String,
    @SerialName("author_id") val authorId: String,
    val section: String = "campus_wall",
    val title: String,
    val content: String,
    val images: String = "[]",
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    val status: String = "published",
    @SerialName("school_id") val schoolId: String,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class CommunityCommentApiDto(
    val id: String,
    @SerialName("post_id") val postId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("parent_id") val parentId: String? = null,
    val content: String,
    @SerialName("like_count") val likeCount: Int = 0,
    val status: String = "published",
    @SerialName("school_id") val schoolId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun CommunityPostApiDto.toMapperDto() = CommunityPostDto(
    id, authorId, section, title, content, images, likeCount,
    commentCount, isPinned, status, schoolId, viewCount, createdAt, updatedAt,
)

private fun CommunityPostDto.toApiDto() = CommunityPostApiDto(
    id, authorId, section, title, content, images, likeCount,
    commentCount, isPinned, status, schoolId, viewCount, createdAt, updatedAt,
)

private fun CommunityCommentApiDto.toMapperDto() = CommunityCommentDto(
    id, postId, authorId, parentId, content, likeCount, status,
    schoolId, createdAt, updatedAt,
)

private fun CommunityCommentDto.toApiDto() = CommunityCommentApiDto(
    id, postId, authorId, parentId, content, likeCount, status,
    schoolId, createdAt, updatedAt,
)

@Singleton
class CommunityRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val communityDao: CommunityDao,
) : ICommunityRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Posts ──────────────────────────────────────────────────

    override fun getPostsBySchool(schoolId: String): Flow<List<CommunityPostDto>> {
        scope.launch { refreshPosts(schoolId) }
        return communityDao.getPostsBySchoolId(schoolId).map { it.map { e -> e.toDto() } }
    }

    override fun getPostsBySection(schoolId: String, section: String): Flow<List<CommunityPostDto>> {
        scope.launch { refreshPosts(schoolId) }
        return communityDao.getPostsBySchoolAndSection(schoolId, section).map { it.map { e -> e.toDto() } }
    }

    override suspend fun getPostById(id: String): CommunityPostDto? {
        return communityDao.getPostById(id)?.toDto()
    }

    override fun getPostsByAuthor(userId: String): Flow<List<CommunityPostDto>> {
        return communityDao.getPostsByAuthor(userId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun createPost(post: CommunityPostDto) {
        val result = supabase.postgrest
            .from("community_posts")
            .insert(post.toApiDto()) { select() }
            .decodeSingle<CommunityPostApiDto>()
        communityDao.upsertPost(result.toMapperDto().toEntity())
    }

    override suspend fun updatePost(id: String, updates: Map<String, Any?>) {
        val result = supabase.postgrest
            .from("community_posts")
            .update(updates) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<CommunityPostApiDto>()
        communityDao.upsertPost(result.toMapperDto().toEntity())
    }

    // ── Comments ───────────────────────────────────────────────

    override fun getCommentsByPostId(postId: String): Flow<List<CommunityCommentDto>> {
        scope.launch { refreshComments(postId) }
        return communityDao.getCommentsByPostId(postId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun createComment(comment: CommunityCommentDto) {
        val result = supabase.postgrest
            .from("community_comments")
            .insert(comment.toApiDto()) { select() }
            .decodeSingle<CommunityCommentApiDto>()
        communityDao.upsertComment(result.toMapperDto().toEntity())
    }

    // ── Comment mutations ─────────────────────────────────────

    override suspend fun deleteComment(commentId: String) {
        supabase.postgrest
            .from("community_comments")
            .update(mapOf("status" to "deleted")) {
                filter { eq("id", commentId) }
            }
        // 同时 mark 本地
        val local = communityDao.getCommentById(commentId)
        if (local != null) {
            communityDao.upsertComment(local.copy(status = "deleted"))
        }
    }

    // ── Likes ──────────────────────────────────────────────────

    override suspend fun toggleLike(postId: String, userId: String): Boolean {
        val existing = communityDao.getLikeByPostAndUser(postId, userId)
        return if (existing != null) {
            supabase.postgrest
                .from("post_likes")
                .delete { filter { eq("id", existing.id) } }
            communityDao.deleteLike(postId, userId)
            false
        } else {
            val id = UUID.randomUUID().toString()
            supabase.postgrest
                .from("post_likes")
                .insert(mapOf("id" to id, "post_id" to postId, "user_id" to userId))
            communityDao.upsertLike(PostLikeEntity(id = id, postId = postId, userId = userId))
            true
        }
    }

    override suspend fun isLiked(postId: String, userId: String): Boolean {
        return communityDao.getLikeByPostAndUser(postId, userId) != null
    }

    override fun getLikesByPostId(postId: String): Flow<List<String>> {
        return communityDao.getLikesByPostId(postId).map { it.map { like -> like.userId } }
    }

    // ── Moderation (EdgeFn) ────────────────────────────────────

    override suspend fun publishPostViaModeration(post: CommunityPostDto): ModerationResult {
        return try {
            val jsonBody = buildJsonObject {
                put("action", "publish_post")
                put("id", post.id)
                put("author_id", post.authorId)
                put("section", post.section)
                put("title", post.title)
                put("content", post.content)
                put("images", post.images)
                put("school_id", post.schoolId)
            }

            val response = supabase.functions.invoke("community-moderation", body = jsonBody)
            val bodyText = response.bodyAsText()

            val moderationResponse = Json.decodeFromString<ModerationResponse>(bodyText)
            // Use server-assigned post_id when available (EdgeFn auto-generates it).
            val serverPostId = moderationResponse.postId ?: post.id
            when (moderationResponse.action) {
                "block" -> {
                    // 仍写入本地以便作者在"我的帖子"中看到被拒状态
                    val blockedPost = post.copy(id = serverPostId, status = "blocked")
                    communityDao.upsertPost(blockedPost.toEntity())
                    ModerationResult.Blocked(
                        reason = moderationResponse.reason ?: "内容不符合社区规范"
                    )
                }
                "review" -> {
                    // EdgeFn already inserted with status=pending_review; sync local cache only.
                    val reviewingPost = post.copy(id = serverPostId, status = "reviewing")
                    communityDao.upsertPost(reviewingPost.toEntity())
                    ModerationResult.Reviewing
                }
                else -> {
                    // pass: EdgeFn already inserted with status=published; sync local cache only.
                    val passedPost = post.copy(id = serverPostId, status = "published")
                    communityDao.upsertPost(passedPost.toEntity())
                    ModerationResult.Passed(postId = serverPostId)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            // ── 不可重试失败（4xx）：EdgeFn 不存在或请求格式错误，不降级 ──
            Log.e(javaClass.simpleName, "EdgeFn returned ${e.response.status.value}: ${e.message}")
            ModerationResult.Error("请求失败 (${e.response.status.value})")
        } catch (e: ServerResponseException) {
            // ── 可重试失败（5xx）：降级直写 ──
            Log.e(javaClass.simpleName, "EdgeFn returned ${e.response.status.value}, falling back to direct insert")
            fallbackInsertPost(post)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Moderation EdgeFn network error", e)
            // 网络错误（timeout/连接失败）视为可重试，降级直写
            fallbackInsertPost(post)
        }
    }

    private suspend fun fallbackInsertPost(post: CommunityPostDto): ModerationResult {
        return try {
            val result = supabase.postgrest
                .from("community_posts")
                .insert(post.toApiDto()) { select() }
                .decodeSingle<CommunityPostApiDto>()
            communityDao.upsertPost(result.toMapperDto().toEntity())
            ModerationResult.Passed(postId = post.id)
        } catch (inner: Exception) {
            Log.e(javaClass.simpleName, "Fallback insert failed", inner)
            throw inner
        }
    }

    // ── Moderation: edit post via EdgeFn ────────────────────────

    override suspend fun updatePostViaModeration(post: CommunityPostDto): ModerationResult {
        return try {
            val jsonBody = buildJsonObject {
                put("action", "update_post")
                put("id", post.id)
                put("author_id", post.authorId)
                put("section", post.section)
                put("title", post.title)
                put("content", post.content)
                put("images", post.images)
                put("school_id", post.schoolId)
            }

            val response = supabase.functions.invoke("community-moderation", body = jsonBody)
            val bodyText = response.bodyAsText()

            val moderationResponse = Json.decodeFromString<ModerationResponse>(bodyText)
            when (moderationResponse.action) {
                "block" -> {
                    // 更新本地状态为 blocked 以便作者可见
                    communityDao.upsertPost(post.copy(status = "blocked").toEntity())
                    ModerationResult.Blocked(
                        reason = moderationResponse.reason ?: "编辑内容不符合社区规范"
                    )
                }
                "review" -> {
                    // EdgeFn 已更新 DB；同步本地缓存即可（不再重复调用 REST UPDATE）
                    communityDao.upsertPost(post.copy(status = "pending_review").toEntity())
                    ModerationResult.Reviewing
                }
                else -> {
                    // pass: EdgeFn 已更新 DB；同步本地缓存即可
                    communityDao.upsertPost(post.copy(status = "published").toEntity())
                    ModerationResult.Passed(postId = post.id)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            // ── 不可重试失败（4xx）：EdgeFn 不存在或请求格式错误，不降级 ──
            Log.e(javaClass.simpleName, "EdgeFn returned ${e.response.status.value}: ${e.message}")
            ModerationResult.Error("编辑请求失败 (${e.response.status.value})")
        } catch (e: ServerResponseException) {
            // ── 可重试失败（5xx）：降级直写 ──
            Log.e(javaClass.simpleName, "EdgeFn returned ${e.response.status.value}, falling back to direct update")
            fallbackUpdatePost(post)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Moderation EdgeFn network error during update", e)
            // 网络错误（timeout/连接失败）降级直写
            fallbackUpdatePost(post)
        }
    }

    private suspend fun fallbackUpdatePost(post: CommunityPostDto): ModerationResult {
        return try {
            val result = supabase.postgrest
                .from("community_posts")
                .update(
                    mapOf(
                        "title" to post.title,
                        "content" to post.content,
                        "images" to post.images,
                        "status" to "published",
                    )
                ) {
                    filter { eq("id", post.id) }
                    select()
                }
                .decodeSingle<CommunityPostApiDto>()
            communityDao.upsertPost(result.toMapperDto().toEntity())
            ModerationResult.Passed(postId = post.id)
        } catch (inner: Exception) {
            Log.e(javaClass.simpleName, "Fallback update failed", inner)
            throw inner
        }
    }

    // ── Moderation: publish comment via EdgeFn ──────────────────

    override suspend fun publishCommentViaModeration(comment: CommunityCommentDto): ModerationResult {
        return try {
            val jsonBody = buildJsonObject {
                put("action", "publish_comment")
                put("post_id", comment.postId)
                put("content", comment.content)
                put("school_id", comment.schoolId)
                comment.parentId?.let { put("parent_id", it) }
            }

            val response = supabase.functions.invoke("community-moderation", body = jsonBody)
            val bodyText = response.bodyAsText()

            val moderationResponse = Json.decodeFromString<ModerationResponse>(bodyText)
            val serverCommentId = moderationResponse.commentId ?: comment.id
            when (moderationResponse.action) {
                "block" -> {
                    // 本地写入 blocked 状态以便作者可见
                    val blocked = comment.copy(id = serverCommentId, status = "blocked")
                    communityDao.upsertComment(blocked.toEntity())
                    ModerationResult.Blocked(
                        reason = moderationResponse.reason ?: "评论内容不符合社区规范"
                    )
                }
                "review" -> {
                    val reviewing = comment.copy(id = serverCommentId, status = "pending_review")
                    communityDao.upsertComment(reviewing.toEntity())
                    ModerationResult.Reviewing
                }
                else -> {
                    val published = comment.copy(id = serverCommentId, status = "published")
                    communityDao.upsertComment(published.toEntity())
                    ModerationResult.Passed(postId = serverCommentId)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            Log.e(javaClass.simpleName, "EdgeFn returned ${e.response.status.value}: ${e.message}")
            ModerationResult.Error("请求失败 (${e.response.status.value})")
        } catch (e: ServerResponseException) {
            Log.e(javaClass.simpleName, "EdgeFn returned ${e.response.status.value}, falling back to direct comment insert")
            fallbackInsertComment(comment)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Moderation EdgeFn network error for comment", e)
            fallbackInsertComment(comment)
        }
    }

    private suspend fun fallbackInsertComment(comment: CommunityCommentDto): ModerationResult {
        return try {
            val result = supabase.postgrest
                .from("community_comments")
                .insert(comment.toApiDto()) { select() }
                .decodeSingle<CommunityCommentApiDto>()
            communityDao.upsertComment(result.toMapperDto().toEntity())
            ModerationResult.Passed(postId = comment.id)
        } catch (inner: Exception) {
            Log.e(javaClass.simpleName, "Fallback comment insert failed", inner)
            throw inner
        }
    }

    // ── Agent: pending review ──────────────────────────────────

    override suspend fun getPendingReviewPosts(schoolId: String): List<CommunityPostDto> {
        return try {
            val result = supabase.postgrest
                .from("community_posts")
                .select {
                    filter {
                        eq("school_id", schoolId)
                        eq("status", "pending_review")
                    }
                }
                .decodeList<CommunityPostApiDto>()
            result.map { it.toMapperDto() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "getPendingReviewPosts error", e)
            emptyList()
        }
    }

    override suspend fun getPendingReviewComments(schoolId: String): List<CommunityCommentDto> {
        return try {
            val result = supabase.postgrest
                .from("community_comments")
                .select {
                    filter {
                        eq("school_id", schoolId)
                        eq("status", "pending_review")
                    }
                }
                .decodeList<CommunityCommentApiDto>()
            result.map { it.toMapperDto() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "getPendingReviewComments error", e)
            emptyList()
        }
    }

    // ── Agent: moderation actions ──────────────────────────────

    override suspend fun updatePostStatus(postId: String, status: String, reason: String?) {
        try {
            // Use EdgeFn for moderation review to log in moderation_logs
            val jsonBody = buildJsonObject {
                put("action", "review")
                put("target_type", "post")
                put("target_id", postId)
                put("status", status)
                reason?.let { put("reason", it) }
            }
            val response = supabase.functions.invoke("community-moderation", body = jsonBody)
            val bodyText = response.bodyAsText()
            val moderationResponse = Json.decodeFromString<ModerationResponse>(bodyText)
            // Update local cache
            val local = communityDao.getPostById(postId)
            if (local != null) {
                communityDao.upsertPost(
                    local.copy(
                        status = status,
                        reviewReason = reason ?: local.reviewReason,
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            Log.e(javaClass.simpleName, "EdgeFn updatePostStatus returned ${e.response.status.value}")
            // Fallback: direct update
            fallbackUpdatePostStatus(postId, status, reason)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "EdgeFn updatePostStatus error", e)
            fallbackUpdatePostStatus(postId, status, reason)
        }
    }

    private suspend fun fallbackUpdatePostStatus(postId: String, status: String, reason: String?) {
        val updates = mutableMapOf<String, Any?>("status" to status)
        reason?.let { updates["review_reason"] = it }
        supabase.postgrest
            .from("community_posts")
            .update(updates) {
                filter { eq("id", postId) }
            }
        val local = communityDao.getPostById(postId)
        if (local != null) {
            communityDao.upsertPost(local.copy(status = status, reviewReason = reason ?: local.reviewReason))
        }
    }

    override suspend fun updateCommentStatus(commentId: String, status: String, reason: String?) {
        try {
            val jsonBody = buildJsonObject {
                put("action", "review")
                put("target_type", "comment")
                put("target_id", commentId)
                put("status", status)
                reason?.let { put("reason", it) }
            }
            val response = supabase.functions.invoke("community-moderation", body = jsonBody)
            val bodyText = response.bodyAsText()
            val moderationResponse = Json.decodeFromString<ModerationResponse>(bodyText)
            // Update local cache
            val local = communityDao.getCommentById(commentId)
            if (local != null) {
                communityDao.upsertComment(
                    local.copy(
                        status = status,
                        reviewReason = reason ?: local.reviewReason,
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            Log.e(javaClass.simpleName, "EdgeFn updateCommentStatus returned ${e.response.status.value}")
            fallbackUpdateCommentStatus(commentId, status, reason)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "EdgeFn updateCommentStatus error", e)
            fallbackUpdateCommentStatus(commentId, status, reason)
        }
    }

    private suspend fun fallbackUpdateCommentStatus(commentId: String, status: String, reason: String?) {
        val updates = mutableMapOf<String, Any?>("status" to status)
        reason?.let { updates["review_reason"] = it }
        supabase.postgrest
            .from("community_comments")
            .update(updates) {
                filter { eq("id", commentId) }
            }
        val local = communityDao.getCommentById(commentId)
        if (local != null) {
            communityDao.upsertComment(local.copy(status = status, reviewReason = reason ?: local.reviewReason))
        }
    }

    // ── Refresh ────────────────────────────────────────────────

    override suspend fun refreshPosts(schoolId: String) {
        try {
            val result = supabase.postgrest
                .from("community_posts")
                .select { filter { eq("school_id", schoolId) } }
                .decodeList<CommunityPostApiDto>()
            communityDao.upsertAllPosts(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }

    override suspend fun refreshComments(postId: String) {
        try {
            val result = supabase.postgrest
                .from("community_comments")
                .select { filter { eq("post_id", postId) } }
                .decodeList<CommunityCommentApiDto>()
            communityDao.upsertAllComments(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }
}

// ── Moderation EdgeFn DTOs ─────────────────────────────────────

@Serializable
private data class ModerationResponse(
    val action: String,   // "block" | "review" | "pass"
    val reason: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("comment_id") val commentId: String? = null,
    val status: String? = null,
)
