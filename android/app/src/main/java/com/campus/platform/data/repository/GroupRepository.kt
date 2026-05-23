package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.CommunityDao
import com.campus.platform.data.local.mapper.GroupMemberDto
import com.campus.platform.data.local.mapper.OfficialGroupDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IGroupRepository
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
private data class OfficialGroupApiDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val direction: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("is_pinned") val isPinned: Boolean = true,
    @SerialName("school_id") val schoolId: String,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class GroupMemberApiDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    val role: String = "member",
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun OfficialGroupApiDto.toMapperDto() = OfficialGroupDto(
    id, name, description, direction, avatarUrl, memberCount,
    isPinned, schoolId, createdBy, createdAt, updatedAt,
)

private fun OfficialGroupDto.toApiDto() = OfficialGroupApiDto(
    id, name, description, direction, avatarUrl, memberCount,
    isPinned, schoolId, createdBy, createdAt, updatedAt,
)

private fun GroupMemberApiDto.toMapperDto() = GroupMemberDto(
    id, groupId, userId, role, joinedAt, createdAt, updatedAt,
)

private fun GroupMemberDto.toApiDto() = GroupMemberApiDto(
    id, groupId, userId, role, joinedAt, createdAt, updatedAt,
)

@Singleton
class GroupRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val communityDao: CommunityDao,
) : IGroupRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getGroupsBySchool(schoolId: String): Flow<List<OfficialGroupDto>> {
        scope.launch { refreshGroups(schoolId) }
        return communityDao.getGroupsBySchoolId(schoolId).map { it.map { e -> e.toDto() } }
    }

    override fun getGroupsByDirection(schoolId: String, direction: String): Flow<List<OfficialGroupDto>> {
        return communityDao.getGroupsBySchoolAndDirection(schoolId, direction).map { it.map { e -> e.toDto() } }
    }

    override suspend fun getGroupById(id: String): OfficialGroupDto? {
        return communityDao.getGroupById(id)?.toDto()
    }

    override fun getMembersByGroupId(groupId: String): Flow<List<GroupMemberDto>> {
        return communityDao.getMembersByGroupId(groupId).map { it.map { e -> e.toDto() } }
    }

    override fun getGroupsByUserId(userId: String): Flow<List<GroupMemberDto>> {
        return communityDao.getGroupsByUserId(userId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun joinGroup(groupId: String, userId: String) {
        val id = UUID.randomUUID().toString()
        val member = GroupMemberApiDto(id = id, groupId = groupId, userId = userId)
        supabase.postgrest
            .from("group_members")
            .insert(member)
        communityDao.upsertMember(member.toMapperDto().toEntity())
    }

    override suspend fun leaveGroup(groupId: String, userId: String) {
        val member = communityDao.getMemberByGroupAndUser(groupId, userId)
        if (member != null) {
            supabase.postgrest
                .from("group_members")
                .delete { filter { eq("id", member.id) } }
            communityDao.deleteMember(groupId, userId)
        }
    }

    override suspend fun isMember(groupId: String, userId: String): Boolean {
        return communityDao.getMemberByGroupAndUser(groupId, userId) != null
    }

    override suspend fun refreshGroups(schoolId: String) {
        try {
            val result = supabase.postgrest
                .from("official_groups")
                .select { filter { eq("school_id", schoolId) } }
                .decodeList<OfficialGroupApiDto>()
            communityDao.upsertAllGroups(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }
}
