package com.campus.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campus.platform.data.local.entity.CommunityPostEntity
import com.campus.platform.data.local.entity.CommunityCommentEntity
import com.campus.platform.data.local.entity.OfficialGroupEntity
import com.campus.platform.data.local.entity.GroupMemberEntity
import com.campus.platform.data.local.entity.PostLikeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityDao {

    // ── CommunityPost ────────────────────────────────────────

    @Upsert
    suspend fun upsertPost(post: CommunityPostEntity)

    @Upsert
    suspend fun upsertAllPosts(posts: List<CommunityPostEntity>)

    @Query("SELECT * FROM community_posts WHERE schoolId = :schoolId AND status != 'deleted' ORDER BY isPinned DESC, createdAt DESC")
    fun getPostsBySchoolId(schoolId: String): Flow<List<CommunityPostEntity>>

    @Query("SELECT * FROM community_posts WHERE schoolId = :schoolId AND section = :section AND status != 'deleted' ORDER BY isPinned DESC, createdAt DESC")
    fun getPostsBySchoolAndSection(schoolId: String, section: String): Flow<List<CommunityPostEntity>>

    @Query("SELECT * FROM community_posts WHERE id = :id")
    suspend fun getPostById(id: String): CommunityPostEntity?

    @Query("SELECT * FROM community_posts WHERE authorId = :userId ORDER BY createdAt DESC")
    fun getPostsByAuthor(userId: String): Flow<List<CommunityPostEntity>>

    @Query("DELETE FROM community_posts WHERE createdAt < :before")
    suspend fun deletePostsOlderThan(before: String)

    @Query("DELETE FROM community_posts")
    suspend fun deleteAllPosts()

    // ── CommunityComment ─────────────────────────────────────

    @Upsert
    suspend fun upsertComment(comment: CommunityCommentEntity)

    @Upsert
    suspend fun upsertAllComments(comments: List<CommunityCommentEntity>)

    @Query("SELECT * FROM community_comments WHERE postId = :postId AND status != 'deleted' ORDER BY createdAt ASC")
    fun getCommentsByPostId(postId: String): Flow<List<CommunityCommentEntity>>

    @Query("SELECT * FROM community_comments WHERE id = :id")
    suspend fun getCommentById(id: String): CommunityCommentEntity?

    @Query("SELECT * FROM community_comments WHERE authorId = :userId ORDER BY createdAt DESC")
    fun getCommentsByAuthor(userId: String): Flow<List<CommunityCommentEntity>>

    @Query("DELETE FROM community_comments WHERE createdAt < :before")
    suspend fun deleteCommentsOlderThan(before: String)

    @Query("DELETE FROM community_comments")
    suspend fun deleteAllComments()

    // ── OfficialGroup ────────────────────────────────────────

    @Upsert
    suspend fun upsertGroup(group: OfficialGroupEntity)

    @Upsert
    suspend fun upsertAllGroups(groups: List<OfficialGroupEntity>)

    @Query("SELECT * FROM official_groups WHERE schoolId = :schoolId ORDER BY isPinned DESC, createdAt DESC")
    fun getGroupsBySchoolId(schoolId: String): Flow<List<OfficialGroupEntity>>

    @Query("SELECT * FROM official_groups WHERE schoolId = :schoolId AND direction = :direction ORDER BY isPinned DESC, createdAt DESC")
    fun getGroupsBySchoolAndDirection(schoolId: String, direction: String): Flow<List<OfficialGroupEntity>>

    @Query("SELECT * FROM official_groups WHERE id = :id")
    suspend fun getGroupById(id: String): OfficialGroupEntity?

    @Query("DELETE FROM official_groups")
    suspend fun deleteAllGroups()

    // ── GroupMember ──────────────────────────────────────────

    @Upsert
    suspend fun upsertMember(member: GroupMemberEntity)

    @Upsert
    suspend fun upsertAllMembers(members: List<GroupMemberEntity>)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedAt ASC")
    fun getMembersByGroupId(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE userId = :userId")
    fun getGroupsByUserId(userId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun getMemberByGroupAndUser(groupId: String, userId: String): GroupMemberEntity?

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteMember(groupId: String, userId: String)

    @Query("DELETE FROM group_members")
    suspend fun deleteAllMembers()

    // ── PostLike ─────────────────────────────────────────────

    @Upsert
    suspend fun upsertLike(like: PostLikeEntity)

    @Query("SELECT * FROM post_likes WHERE postId = :postId ORDER BY createdAt DESC")
    fun getLikesByPostId(postId: String): Flow<List<PostLikeEntity>>

    @Query("SELECT * FROM post_likes WHERE userId = :userId ORDER BY createdAt DESC")
    fun getLikesByUserId(userId: String): Flow<List<PostLikeEntity>>

    @Query("SELECT * FROM post_likes WHERE postId = :postId AND userId = :userId")
    suspend fun getLikeByPostAndUser(postId: String, userId: String): PostLikeEntity?

    @Query("DELETE FROM post_likes WHERE postId = :postId AND userId = :userId")
    suspend fun deleteLike(postId: String, userId: String)

    @Query("DELETE FROM post_likes")
    suspend fun deleteAllLikes()
}
