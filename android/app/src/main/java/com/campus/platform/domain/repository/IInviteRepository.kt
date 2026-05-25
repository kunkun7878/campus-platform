package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.InviteCodeDto
import com.campus.platform.data.local.mapper.InviteRecordDto
import kotlinx.coroutines.flow.Flow

interface IInviteRepository {

    fun getInviteCode(userId: String): Flow<InviteCodeDto?>

    suspend fun generateInviteCode(userId: String): InviteCodeDto

    fun getInviteRecords(userId: String): Flow<List<InviteRecordDto>>

    fun getInviteCount(userId: String): Flow<Int>
}
