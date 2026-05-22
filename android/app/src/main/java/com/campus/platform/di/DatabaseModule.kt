package com.campus.platform.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.campus.platform.data.local.AppDatabase
import com.campus.platform.data.local.dao.CommunityDao
import com.campus.platform.data.local.dao.LostFoundDao
import com.campus.platform.data.local.dao.MarketDao
import com.campus.platform.data.local.dao.MiscDao
import com.campus.platform.data.local.dao.ReferenceDao
import com.campus.platform.data.local.dao.RunnerDao
import com.campus.platform.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import android.util.Base64
import java.security.SecureRandom
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "campus_platform.db"
    private const val PASSPHRASE_LENGTH = 32
    private const val DB_PREFS_NAME = "campus_db_secure_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"

    @Suppress("DEPRECATION")
    private fun getEncryptedPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            DB_PREFS_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    /**
     * Derives or generates a passphrase for SQLCipher, stored in EncryptedSharedPreferences.
     */
    @Provides
    @Singleton
    fun providePassphrase(@ApplicationContext context: Context): ByteArray {
        val prefs = getEncryptedPrefs(context)
        val stored = prefs.getString(KEY_PASSPHRASE, null)

        return if (stored != null) {
            Base64.decode(stored, Base64.NO_WRAP)
        } else {
            val random = SecureRandom()
            val passphrase = ByteArray(PASSPHRASE_LENGTH)
            random.nextBytes(passphrase)
            val encoded = Base64.encodeToString(passphrase, Base64.NO_WRAP)
            prefs.edit().putString(KEY_PASSPHRASE, encoded).apply()
            passphrase
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        passphrase: ByteArray,
    ): AppDatabase {
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideReferenceDao(db: AppDatabase): ReferenceDao = db.referenceDao()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideRunnerDao(db: AppDatabase): RunnerDao = db.runnerDao()

    @Provides
    fun provideMarketDao(db: AppDatabase): MarketDao = db.marketDao()

    @Provides
    fun provideCommunityDao(db: AppDatabase): CommunityDao = db.communityDao()

    @Provides
    fun provideLostFoundDao(db: AppDatabase): LostFoundDao = db.lostFoundDao()

    @Provides
    fun provideMiscDao(db: AppDatabase): MiscDao = db.miscDao()
}
