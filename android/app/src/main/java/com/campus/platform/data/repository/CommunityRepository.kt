package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.CommunityDao
import com.campus.platform.data.local.entity.PostLikeEntity
import com.campus.platform.data.local.mapper.CommunityCommentDto
import com.campus.platform.data.local.mapper.CommunityPostDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.ICommunityRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
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
