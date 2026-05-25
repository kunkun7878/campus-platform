package com.campus.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campus.platform.data.local.entity.ProfileEntity
import com.campus.platform.data.local.entity.WalletEntity
import com.campus.platform.data.local.entity.InviteCodeEntity
import com.campus.platform.data.local.entity.InviteRecordEntity
import com.campus.platform.data.local.entity.WalletTransactionEntity
import com.campus.platform.data.local.entity.UserAddressEntity
import com.campus.platform.data.local.entity.UserFavoriteEntity
import com.campus.platform.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // ── Profile ──────────────────────────────────────────────

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun getProfileById(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileByIdOnce(id: String): ProfileEntity?

    @Query("DELETE FROM profiles")
    suspend fun deleteAllProfiles()

    // ── Wallet ───────────────────────────────────────────────

    @Upsert
    suspend fun upsertWallet(wallet: WalletEntity)

    @Query("SELECT * FROM wallets WHERE userId = :userId")
    fun getWalletByUserId(userId: String): Flow<WalletEntity?>

    @Query("DELETE FROM wallets")
    suspend fun deleteAllWallets()

    // ── WalletTransaction ────────────────────────────────────

    @Upsert
    suspend fun upsertWalletTransaction(tx: WalletTransactionEntity)

    @Upsert
    suspend fun upsertAllWalletTransactions(txs: List<WalletTransactionEntity>)

    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    fun getWalletTransactionsByUserId(userId: String, limit: Int = 50): Flow<List<WalletTransactionEntity>>

    @Query("DELETE FROM wallet_transactions")
    suspend fun deleteAllWalletTransactions()

    // ── UserAddress ──────────────────────────────────────────

    @Upsert
    suspend fun upsertAddress(address: UserAddressEntity)

    @Upsert
    suspend fun upsertAllAddresses(addresses: List<UserAddressEntity>)

    @Query("SELECT * FROM user_addresses WHERE userId = :userId ORDER BY isDefault DESC, createdAt DESC")
    fun getAddressesByUserId(userId: String): Flow<List<UserAddressEntity>>

    @Query("SELECT * FROM user_addresses WHERE id = :id")
    suspend fun getAddressById(id: String): UserAddressEntity?

    @Query("DELETE FROM user_addresses WHERE id = :id")
    suspend fun deleteAddressById(id: String)

    @Query("DELETE FROM user_addresses")
    suspend fun deleteAllAddresses()

    // ── UserFavorite ─────────────────────────────────────────

    @Upsert
    suspend fun upsertFavorite(favorite: UserFavoriteEntity)

    @Query("SELECT * FROM user_favorites WHERE userId = :userId ORDER BY createdAt DESC")
    fun getFavoritesByUserId(userId: String): Flow<List<UserFavoriteEntity>>

    @Query("SELECT * FROM user_favorites WHERE userId = :userId AND targetType = :targetType ORDER BY createdAt DESC")
    fun getFavoritesByUserIdAndType(userId: String, targetType: String): Flow<List<UserFavoriteEntity>>

    @Query("SELECT * FROM user_favorites WHERE userId = :userId AND targetType = :targetType AND targetId = :targetId")
    suspend fun getFavoriteByTarget(userId: String, targetType: String, targetId: String): UserFavoriteEntity?

    @Query("DELETE FROM user_favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)

    @Query("DELETE FROM user_favorites WHERE userId = :userId AND targetType = :targetType AND targetId = :targetId")
    suspend fun deleteFavoriteByTarget(userId: String, targetType: String, targetId: String)

    @Query("DELETE FROM user_favorites")
    suspend fun deleteAllFavorites()

    // ── Notification ─────────────────────────────────────────

    @Upsert
    suspend fun upsertNotification(notification: NotificationEntity)

    @Upsert
    suspend fun upsertAllNotifications(notifications: List<NotificationEntity>)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsByUserId(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotificationsByUserId(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCountByUserId(userId: String): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1, readAt = :readAt WHERE id = :id")
    suspend fun markAsRead(id: String, readAt: String)

    @Query("UPDATE notifications SET isRead = 1, readAt = :readAt WHERE userId = :userId AND isRead = 0")
    suspend fun markAllAsRead(userId: String, readAt: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: String)

    @Query("DELETE FROM notifications WHERE createdAt < :before")
    suspend fun deleteNotificationsOlderThan(before: String)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    // ── InviteCode ───────────────────────────────────────────────

    @Upsert
    suspend fun upsertInviteCode(code: InviteCodeEntity)

    @Upsert
    suspend fun upsertAllInviteCodes(codes: List<InviteCodeEntity>)

    @Query("SELECT * FROM invite_codes WHERE userId = :userId")
    fun getInviteCodeByUserId(userId: String): Flow<InviteCodeEntity?>

    @Query("SELECT * FROM invite_codes WHERE code = :code")
    suspend fun getInviteCodeByCode(code: String): InviteCodeEntity?

    @Query("DELETE FROM invite_codes")
    suspend fun deleteAllInviteCodes()

    // ── InviteRecord ─────────────────────────────────────────────

    @Upsert
    suspend fun upsertInviteRecord(record: InviteRecordEntity)

    @Upsert
    suspend fun upsertAllInviteRecords(records: List<InviteRecordEntity>)

    @Query("SELECT * FROM invite_records WHERE inviterId = :userId ORDER BY createdAt DESC")
    fun getInviteRecordsByInviterId(userId: String): Flow<List<InviteRecordEntity>>

    @Query("SELECT COUNT(*) FROM invite_records WHERE inviterId = :userId")
    fun getInviteCountByInviterId(userId: String): Flow<Int>

    @Query("DELETE FROM invite_records")
    suspend fun deleteAllInviteRecords()
}
