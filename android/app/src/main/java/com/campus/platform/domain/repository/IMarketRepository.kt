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

    suspend fun refreshListings(schoolId: String)
}
