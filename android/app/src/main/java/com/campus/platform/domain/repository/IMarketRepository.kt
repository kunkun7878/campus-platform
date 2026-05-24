package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.MarketListingDto
import kotlinx.coroutines.flow.Flow

interface IMarketRepository {

    fun getListingsBySchool(schoolId: String): Flow<List<MarketListingDto>>

    fun getListingsByCategory(schoolId: String, category: String): Flow<List<MarketListingDto>>

    suspend fun getListingById(id: String): MarketListingDto?

    fun getListingsBySeller(userId: String): Flow<List<MarketListingDto>>

    suspend fun publishListing(listing: MarketListingDto)

    suspend fun updateListing(id: String, updates: Map<String, Any?>)

    /**
     * 乐观锁更新 listing 状态。只有当 listing 当前状态等于 [expectedStatus]
     * 时才将其更新为 [newStatus]。返回 true 表示更新成功，false 表示状态
     * 已被并发操作改变（如购买已将 status 改为 reserved）。
     */
    suspend fun updateListingStatus(id: String, expectedStatus: String, newStatus: String): Boolean

    fun getListingsByIdsFlow(ids: List<String>): Flow<List<MarketListingDto>>

    suspend fun refreshListings(schoolId: String)
}
