package com.campus.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campus.platform.data.local.entity.MarketListingEntity
import com.campus.platform.data.local.entity.MarketOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketDao {

    // ── MarketListing ────────────────────────────────────────

    @Upsert
    suspend fun upsertListing(listing: MarketListingEntity)

    @Upsert
    suspend fun upsertAllListings(listings: List<MarketListingEntity>)

    @Query("SELECT * FROM market_listings WHERE schoolId = :schoolId ORDER BY createdAt DESC")
    fun getListingsBySchoolId(schoolId: String): Flow<List<MarketListingEntity>>

    @Query("SELECT * FROM market_listings WHERE schoolId = :schoolId AND status = :status ORDER BY createdAt DESC")
    fun getListingsBySchoolAndStatus(schoolId: String, status: String): Flow<List<MarketListingEntity>>

    @Query("SELECT * FROM market_listings WHERE schoolId = :schoolId AND category = :category ORDER BY createdAt DESC")
    fun getListingsBySchoolAndCategory(schoolId: String, category: String): Flow<List<MarketListingEntity>>

    @Query("SELECT * FROM market_listings WHERE id = :id")
    suspend fun getListingById(id: String): MarketListingEntity?

    @Query("SELECT * FROM market_listings WHERE sellerId = :userId ORDER BY createdAt DESC")
    fun getListingsBySeller(userId: String): Flow<List<MarketListingEntity>>

    @Query("DELETE FROM market_listings WHERE createdAt < :before")
    suspend fun deleteListingsOlderThan(before: String)

    @Query("DELETE FROM market_listings")
    suspend fun deleteAllListings()

    // ── MarketOrder ──────────────────────────────────────────

    @Upsert
    suspend fun upsertOrder(order: MarketOrderEntity)

    @Upsert
    suspend fun upsertAllOrders(orders: List<MarketOrderEntity>)

    @Query("SELECT * FROM market_orders WHERE schoolId = :schoolId ORDER BY createdAt DESC")
    fun getOrdersBySchoolId(schoolId: String): Flow<List<MarketOrderEntity>>

    @Query("SELECT * FROM market_orders WHERE id = :id")
    suspend fun getOrderById(id: String): MarketOrderEntity?

    @Query("SELECT * FROM market_orders WHERE buyerId = :userId ORDER BY createdAt DESC")
    fun getOrdersByBuyer(userId: String): Flow<List<MarketOrderEntity>>

    @Query("SELECT * FROM market_orders WHERE sellerId = :userId ORDER BY createdAt DESC")
    fun getOrdersBySeller(userId: String): Flow<List<MarketOrderEntity>>

    @Query("SELECT * FROM market_orders WHERE listingId = :listingId ORDER BY createdAt DESC")
    fun getOrdersByListingId(listingId: String): Flow<List<MarketOrderEntity>>

    @Query("DELETE FROM market_orders WHERE createdAt < :before")
    suspend fun deleteOrdersOlderThan(before: String)

    @Query("DELETE FROM market_orders")
    suspend fun deleteAllOrders()
}
