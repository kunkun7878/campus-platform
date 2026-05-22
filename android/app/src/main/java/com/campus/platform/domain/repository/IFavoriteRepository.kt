package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.UserFavoriteDto
import kotlinx.coroutines.flow.Flow

interface IFavoriteRepository {

    fun getFavorites(userId: String): Flow<List<UserFavoriteDto>>

    suspend fun addFavorite(favorite: UserFavoriteDto)

    suspend fun removeFavorite(id: String)

    suspend fun removeFavoriteByTarget(userId: String, targetType: String, targetId: String)

    suspend fun isFavorited(userId: String, targetType: String, targetId: String): Boolean

    suspend fun refreshFavorites(userId: String)
}
