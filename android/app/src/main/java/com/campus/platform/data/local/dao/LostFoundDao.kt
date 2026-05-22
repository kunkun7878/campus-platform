package com.campus.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campus.platform.data.local.entity.LostFoundItemEntity
import com.campus.platform.data.local.entity.LostFoundClaimEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LostFoundDao {

    // ── LostFoundItem ────────────────────────────────────────

    @Upsert
    suspend fun upsertItem(item: LostFoundItemEntity)

    @Upsert
    suspend fun upsertAllItems(items: List<LostFoundItemEntity>)

    @Query("SELECT * FROM lost_found_items WHERE schoolId = :schoolId ORDER BY createdAt DESC")
    fun getItemsBySchoolId(schoolId: String): Flow<List<LostFoundItemEntity>>

    @Query("SELECT * FROM lost_found_items WHERE schoolId = :schoolId AND type = :type ORDER BY createdAt DESC")
    fun getItemsBySchoolAndType(schoolId: String, type: String): Flow<List<LostFoundItemEntity>>

    @Query("SELECT * FROM lost_found_items WHERE schoolId = :schoolId AND category = :category ORDER BY createdAt DESC")
    fun getItemsBySchoolAndCategory(schoolId: String, category: String): Flow<List<LostFoundItemEntity>>

    @Query("SELECT * FROM lost_found_items WHERE id = :id")
    suspend fun getItemById(id: String): LostFoundItemEntity?

    @Query("SELECT * FROM lost_found_items WHERE publisherId = :userId ORDER BY createdAt DESC")
    fun getItemsByPublisher(userId: String): Flow<List<LostFoundItemEntity>>

    @Query("DELETE FROM lost_found_items WHERE createdAt < :before")
    suspend fun deleteItemsOlderThan(before: String)

    @Query("DELETE FROM lost_found_items")
    suspend fun deleteAllItems()

    // ── LostFoundClaim ───────────────────────────────────────

    @Upsert
    suspend fun upsertClaim(claim: LostFoundClaimEntity)

    @Upsert
    suspend fun upsertAllClaims(claims: List<LostFoundClaimEntity>)

    @Query("SELECT * FROM lost_found_claims WHERE itemId = :itemId ORDER BY createdAt DESC")
    fun getClaimsByItemId(itemId: String): Flow<List<LostFoundClaimEntity>>

    @Query("SELECT * FROM lost_found_claims WHERE claimantId = :userId ORDER BY createdAt DESC")
    fun getClaimsByClaimant(userId: String): Flow<List<LostFoundClaimEntity>>

    @Query("SELECT * FROM lost_found_claims WHERE id = :id")
    suspend fun getClaimById(id: String): LostFoundClaimEntity?

    @Query("DELETE FROM lost_found_claims WHERE createdAt < :before")
    suspend fun deleteClaimsOlderThan(before: String)

    @Query("DELETE FROM lost_found_claims")
    suspend fun deleteAllClaims()
}
