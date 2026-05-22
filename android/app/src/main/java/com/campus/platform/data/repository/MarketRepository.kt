package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.MarketDao
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IMarketRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class MarketListingApiDto(
    val id: String,
    @SerialName("seller_id") val sellerId: String,
    val title: String,
    val description: String? = null,
    val price: Int = 0,
    @SerialName("original_price") val originalPrice: Int? = null,
    val images: String = "[]",
    val category: String,
    val condition: String,
    val status: String = "active",
    @SerialName("school_id") val schoolId: String,
    @SerialName("is_bargain") val isBargain: Boolean = true,
    val contact: String = "站内私信联系",
    @SerialName("meetup_location") val meetupLocation: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun MarketListingApiDto.toMapperDto() = MarketListingDto(
    id, sellerId, title, description, price, originalPrice, images,
    category, condition, status, schoolId, isBargain, contact,
    meetupLocation, createdAt, updatedAt,
)

private fun MarketListingDto.toApiDto() = MarketListingApiDto(
    id, sellerId, title, description, price, originalPrice, images,
    category, condition, status, schoolId, isBargain, contact,
    meetupLocation, createdAt, updatedAt,
)

@Singleton
class MarketRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val marketDao: MarketDao,
) : IMarketRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getListingsBySchool(schoolId: String): Flow<List<MarketListingDto>> {
        scope.launch { refreshListings(schoolId) }
        return marketDao.getListingsBySchoolId(schoolId).map { it.map { e -> e.toDto() } }
    }

    override fun getListingsByCategory(schoolId: String, category: String): Flow<List<MarketListingDto>> {
        scope.launch { refreshListings(schoolId) }
        return marketDao.getListingsBySchoolAndCategory(schoolId, category).map { it.map { e -> e.toDto() } }
    }

    override suspend fun getListingById(id: String): MarketListingDto? {
        return marketDao.getListingById(id)?.toDto()
    }

    override fun getListingsBySeller(userId: String): Flow<List<MarketListingDto>> {
        return marketDao.getListingsBySeller(userId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun publishListing(listing: MarketListingDto) {
        val result = supabase.postgrest
            .from("market_listings")
            .insert(listing.toApiDto()) { select() }
            .decodeSingle<MarketListingApiDto>()
        marketDao.upsertListing(result.toMapperDto().toEntity())
    }

    override suspend fun updateListing(id: String, updates: Map<String, Any?>) {
        val result = supabase.postgrest
            .from("market_listings")
            .update(updates) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<MarketListingApiDto>()
        marketDao.upsertListing(result.toMapperDto().toEntity())
    }

    override suspend fun refreshListings(schoolId: String) {
        try {
            val result = supabase.postgrest
                .from("market_listings")
                .select { filter { eq("school_id", schoolId) } }
                .decodeList<MarketListingApiDto>()
            marketDao.upsertAllListings(result.map { it.toMapperDto().toEntity() })
        } catch (e: Exception) { if (e is CancellationException) throw e }
    }
}
