package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.UserDao
import com.campus.platform.data.local.mapper.UserFavoriteDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IFavoriteRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class FavoriteApiDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    @SerialName("created_at") val createdAt: String? = null,
)

private fun FavoriteApiDto.toMapperDto() = UserFavoriteDto(
    id, userId, targetType, targetId, createdAt,
)

private fun UserFavoriteDto.toApiDto() = FavoriteApiDto(
    id, userId, targetType, targetId, createdAt,
)

@Singleton
class FavoriteRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao,
) : IFavoriteRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getFavorites(userId: String): Flow<List<UserFavoriteDto>> {
        scope.launch { refreshFavorites(userId) }
        return userDao.getFavoritesByUserId(userId).map { it.map { e -> e.toDto() } }
    }

    override fun getFavoritesByUserIdAndTypeFlow(userId: String, targetType: String): Flow<List<UserFavoriteDto>> {
        return userDao.getFavoritesByUserIdAndType(userId, targetType).map { it.map { e -> e.toDto() } }
    }

    override suspend fun addFavorite(favorite: UserFavoriteDto) {
        val apiDto = favorite.copy(id = favorite.id.ifBlank { UUID.randomUUID().toString() }).toApiDto()
        supabase.postgrest
            .from("user_favorites")
            .insert(apiDto)
        userDao.upsertFavorite(apiDto.toMapperDto().toEntity())
    }

    override suspend fun removeFavorite(id: String) {
        supabase.postgrest
            .from("user_favorites")
            .delete { filter { eq("id", id) } }
        userDao.deleteFavoriteById(id)
    }

    override suspend fun removeFavoriteByTarget(userId: String, targetType: String, targetId: String) {
        val existing = userDao.getFavoriteByTarget(userId, targetType, targetId)
        if (existing != null) {
            supabase.postgrest
                .from("user_favorites")
                .delete { filter { eq("id", existing.id) } }
            userDao.deleteFavoriteByTarget(userId, targetType, targetId)
        }
    }

    override suspend fun isFavorited(userId: String, targetType: String, targetId: String): Boolean {
        return userDao.getFavoriteByTarget(userId, targetType, targetId) != null
    }

    override suspend fun refreshFavorites(userId: String) {
        try {
            val result = supabase.postgrest
                .from("user_favorites")
                .select { filter { eq("user_id", userId) } }
                .decodeList<FavoriteApiDto>()
            result.forEach { userDao.upsertFavorite(it.toMapperDto().toEntity()) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }
}
