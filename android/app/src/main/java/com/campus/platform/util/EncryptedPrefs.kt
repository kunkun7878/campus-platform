package com.campus.platform.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * EncryptedSharedPreferences wrapper for secure local storage.
 * Uses AndroidX Security Crypto with AES256-GCM encryption.
 *
 * Note: Auth token storage is managed by the Supabase SDK internally.
 * DataStore no longer stores authToken — do not add token storage back here.
 */
object EncryptedPrefs {

    private const val PREFS_NAME = "campus_secure_prefs"

    private fun getPrefs(context: Context): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
