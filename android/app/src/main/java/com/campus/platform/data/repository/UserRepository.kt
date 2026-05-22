package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.UserDao
import com.campus.platform.data.local.mapper.WalletDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.data.model.Profile
import com.campus.platform.domain.repository.IUserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao,
) : IUserRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Profile ────────────────────────────────────────────────

    override fun getProfile(userId: String): Flow<Profile?> {
        scope.launch { refreshProfile(userId) }
        return userDao.getProfileById(userId).map { it?.toDto() }
    }

    override suspend fun refreshProfile(userId: String) {
        try {
            val profile = supabase.postgrest
                .from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<Profile>()
            if (profile != null) {
                userDao.upsertProfile(profile.toEntity())
            }
        } catch (e: Exception) { if (e is CancellationException) throw e }
    }

    override suspend fun updateProfile(
        userId: String,
        nickname: String?,
        avatarUrl: String?,
    ) {
        val updates = buildMap<String, String?> {
            nickname?.let { put("nickname", it) }
            avatarUrl?.let { put("avatar_url", it) }
        }
        supabase.postgrest
            .from("profiles")
            .update(updates) { filter { eq("id", userId) } }
        // Refresh local cache after successful update
        refreshProfile(userId)
    }

    // ── Wallet ─────────────────────────────────────────────────

    override fun getWallet(userId: String): Flow<WalletDto?> {
        scope.launch { refreshWallet(userId) }
        return userDao.getWalletByUserId(userId).map { it?.toDto() }
    }

    override suspend fun refreshWallet(userId: String) {
        try {
            val wallet = supabase.postgrest
                .from("wallets")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<WalletDto>()
            if (wallet != null) {
                userDao.upsertWallet(wallet.toEntity())
            }
        } catch (e: Exception) { if (e is CancellationException) throw e }
    }
}
