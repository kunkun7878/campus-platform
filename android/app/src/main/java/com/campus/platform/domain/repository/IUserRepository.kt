package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.WalletDto
import com.campus.platform.data.local.mapper.WalletTransactionDto
import com.campus.platform.data.model.Profile
import kotlinx.coroutines.flow.Flow

interface IUserRepository {

    // ── Profile ────────────────────────────────────────────────

    fun getProfile(userId: String): Flow<Profile?>

    suspend fun refreshProfile(userId: String)

    suspend fun updateProfile(
        userId: String,
        nickname: String? = null,
        avatarUrl: String? = null,
    )

    // ── Wallet ─────────────────────────────────────────────────

    fun getWallet(userId: String): Flow<WalletDto?>

    suspend fun refreshWallet(userId: String)

    fun getWalletTransactions(userId: String, limit: Int = 50): Flow<List<WalletTransactionDto>>

    // ── Admin / Agent ─────────────────────────────────────────

    /** 获取同校用户列表（代课/代理使用） */
    suspend fun getUsersBySchool(schoolId: String): List<Profile>

    /** 获取单个用户详情（代课/代理使用） */
    suspend fun getUserById(userId: String): Profile?

    /** 封禁/解封用户（status: 0=正常, 1=封禁） */
    suspend fun updateUserStatus(userId: String, status: Int)
}
