package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.LostFoundClaimDto
import com.campus.platform.data.local.mapper.LostFoundItemDto
import kotlinx.coroutines.flow.Flow

interface ILostFoundRepository {

    fun getItemsBySchool(schoolId: String): Flow<List<LostFoundItemDto>>

    fun getItemsByType(schoolId: String, type: String): Flow<List<LostFoundItemDto>>

    fun getItemsByCategory(schoolId: String, category: String): Flow<List<LostFoundItemDto>>

    suspend fun getItemById(id: String): LostFoundItemDto?

    fun getItemsByPublisher(userId: String): Flow<List<LostFoundItemDto>>

    suspend fun publishItem(item: LostFoundItemDto)

    suspend fun updateItem(id: String, updates: Map<String, Any?>)

    suspend fun createClaim(claim: LostFoundClaimDto)

    fun getClaimsByItemId(itemId: String): Flow<List<LostFoundClaimDto>>

    fun getClaimsByClaimant(userId: String): Flow<List<LostFoundClaimDto>>

    suspend fun refreshItems(schoolId: String)
}
