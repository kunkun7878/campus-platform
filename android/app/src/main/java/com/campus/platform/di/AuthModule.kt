package com.campus.platform.di

import com.campus.platform.BuildConfig
import com.campus.platform.data.auth.AuthRepository
import android.util.Log
import com.campus.platform.data.school.SchoolRepository
import com.campus.platform.push.FcmTokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return try {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            ) {
                install(Auth)
                install(Postgrest)
                install(Realtime)
                install(Storage)
                install(Functions)
            }
        } catch (e: Exception) {
            Log.e("AuthModule", "SupabaseClient init failed", e)
            throw e
        }
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        supabase: SupabaseClient,
        fcmTokenManager: FcmTokenManager,
    ): AuthRepository {
        return AuthRepository(supabase, fcmTokenManager)
    }

    @Provides
    @Singleton
    fun provideSchoolRepository(supabase: SupabaseClient): SchoolRepository {
        return SchoolRepository(supabase)
    }

    @Provides
    @Singleton
    fun provideFcmTokenManager(supabase: SupabaseClient): FcmTokenManager {
        return FcmTokenManager(supabase)
    }
}
