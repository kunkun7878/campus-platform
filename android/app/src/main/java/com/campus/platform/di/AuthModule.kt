package com.campus.platform.di

import com.campus.platform.BuildConfig
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.school.SchoolRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    @Provides
    @Singleton
    fun provideAuthRepository(supabase: SupabaseClient): AuthRepository {
        return AuthRepository(supabase)
    }

    @Provides
    @Singleton
    fun provideSchoolRepository(supabase: SupabaseClient): SchoolRepository {
        return SchoolRepository(supabase)
    }
}
