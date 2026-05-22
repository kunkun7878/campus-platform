package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.GroupMemberDto
import com.campus.platform.data.local.mapper.OfficialGroupDto
import kotlinx.coroutines.flow.Flow

interface IGroupRepository {

    fun getGroupsBySchool(schoolId: String): Flow<List<OfficialGroupDto>>

    fun getGroupsByDirection(schoolId: String, direction: String): Flow<List<OfficialGroupDto>>

    suspend fun getGroupById(id: String): OfficialGroupDto?

    fun getMembersByGroupId(groupId: String): Flow<List<GroupMemberDto>>

    fun getGroupsByUserId(userId: String): Flow<List<GroupMemberDto>>

    suspend fun joinGroup(groupId: String, userId: String)

    suspend fun leaveGroup(groupId: String, userId: String)

    suspend fun isMember(groupId: String, userId: String): Boolean

    suspend fun refreshGroups(schoolId: String)
}
