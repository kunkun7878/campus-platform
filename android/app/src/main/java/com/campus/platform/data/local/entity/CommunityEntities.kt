package com.campus.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val section: String = SECTION_CAMPUS_WALL,
    val title: String,
    val content: String,
    val images: String = "[]",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isPinned: Boolean = false,
    val status: String = STATUS_PUBLISHED,
    val schoolId: String,
    val viewCount: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val SECTION_CAMPUS_WALL = "campus_wall"
        const val SECTION_DISCUSSION = "discussion"
        const val STATUS_PUBLISHED = "published"
        const val STATUS_HIDDEN = "hidden"
        const val STATUS_DELETED = "deleted"
    }
}

@Entity(tableName = "community_comments")
data class CommunityCommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val parentId: String? = null,
    val content: String,
    val likeCount: Int = 0,
    val status: String = STATUS_PUBLISHED,
    val schoolId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val STATUS_PUBLISHED = "published"
        const val STATUS_HIDDEN = "hidden"
        const val STATUS_DELETED = "deleted"
    }
}

@Entity(tableName = "official_groups")
data class OfficialGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val direction: String,
    val avatarUrl: String? = null,
    val memberCount: Int = 0,
    val isPinned: Boolean = true,
    val schoolId: String,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val DIRECTION_CHAT = "chat"
        const val DIRECTION_DATING = "dating"
        const val DIRECTION_PART_TIME = "part_time"
    }
}

@Entity(tableName = "group_members")
data class GroupMemberEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val userId: String,
    val role: String = ROLE_MEMBER,
    val joinedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        const val ROLE_MEMBER = "member"
        const val ROLE_ADMIN = "admin"
        const val ROLE_OWNER = "owner"
    }
}

@Entity(tableName = "post_likes")
data class PostLikeEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val userId: String,
    val createdAt: String? = null,
)
