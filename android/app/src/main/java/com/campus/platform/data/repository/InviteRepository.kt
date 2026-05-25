package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.UserDao
import com.campus.platform.data.local.mapper.InviteCodeDto
import com.campus.platform.data.local.mapper.InviteRecordDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IInviteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InviteRepository @Inject constructor(
    private val userDao: UserDao,
) : IInviteRepository {

    override fun getInviteCode(userId: String): Flow<InviteCodeDto?> {
        return userDao.getInviteCodeByUserId(userId).map { it?.toDto() }
    }

    override suspend fun generateInviteCode(userId: String): InviteCodeDto {
        val existing = userDao.getInviteCodeByCode("") // trigger flow
        val code = InviteCodeDto(
            id = UUID.randomUUID().toString(),
            userId = userId,
            code = generateRandomCode(),
            isActive = true,
            usageCount = 0,
            createdAt = null,
            updatedAt = null,
        )
        userDao.upsertInviteCode(code.toEntity())
        return code
    }

    override fun getInviteRecords(userId: String): Flow<List<InviteRecordDto>> {
        return userDao.getInviteRecordsByInviterId(userId).map { entities ->
            entities.map { it.toDto() }
        }
    }

    override fun getInviteCount(userId: String): Flow<Int> {
        return userDao.getInviteCountByInviterId(userId)
    }

    private fun generateRandomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
}
