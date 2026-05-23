package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.MarketDao
import com.campus.platform.data.local.mapper.MarketOrderDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IMarketOrderRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import android.util.Log
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
private data class MarketOrderApiDto(
    val id: String,
    @SerialName("listing_id") val listingId: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("seller_id") val sellerId: String,
    val status: String = "pending",
    @SerialName("meetup_location") val meetupLocation: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("school_id") val schoolId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun MarketOrderApiDto.toMapperDto() = MarketOrderDto(
    id, listingId, buyerId, sellerId, status, meetupLocation,
    completedAt, schoolId, createdAt, updatedAt,
)

private fun MarketOrderDto.toApiDto() = MarketOrderApiDto(
    id, listingId, buyerId, sellerId, status, meetupLocation,
    completedAt, schoolId, createdAt, updatedAt,
)

@Singleton
class MarketOrderRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val marketDao: MarketDao,
) : IMarketOrderRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getOrdersByBuyer(userId: String): Flow<List<MarketOrderDto>> {
        return marketDao.getOrdersByBuyer(userId).map { it.map { e -> e.toDto() } }
    }

    override fun getOrdersBySeller(userId: String): Flow<List<MarketOrderDto>> {
        return marketDao.getOrdersBySeller(userId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun getOrderById(id: String): MarketOrderDto? {
        return marketDao.getOrderById(id)?.toDto()
    }

    override suspend fun createOrder(order: MarketOrderDto) {
        val result = supabase.postgrest
            .from("market_orders")
            .insert(order.toApiDto()) { select() }
            .decodeSingle<MarketOrderApiDto>()
        marketDao.upsertOrder(result.toMapperDto().toEntity())
    }

    override suspend fun updateOrderStatus(id: String, status: String) {
        val result = supabase.postgrest
            .from("market_orders")
            .update(mapOf("status" to status)) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<MarketOrderApiDto>()
        marketDao.upsertOrder(result.toMapperDto().toEntity())
    }

    override suspend fun refreshOrders(schoolId: String) {
        try {
            val result = supabase.postgrest
                .from("market_orders")
                .select { filter { eq("school_id", schoolId) } }
                .decodeList<MarketOrderApiDto>()
            marketDao.upsertAllOrders(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }
}
