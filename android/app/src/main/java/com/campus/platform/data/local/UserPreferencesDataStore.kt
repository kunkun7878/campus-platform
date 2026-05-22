package com.campus.platform.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object Keys {
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_SCHOOL_ID = stringPreferencesKey("school_id")
        val KEY_CAMPUS_ID = stringPreferencesKey("campus_id")
        val KEY_IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    }

    // ── User ID ──────────────────────────────────────────────

    val userId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    suspend fun setUserId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[KEY_USER_ID] = id
            else prefs.remove(KEY_USER_ID)
        }
    }

    // ── School ID ────────────────────────────────────────────

    val schoolId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCHOOL_ID]
    }

    suspend fun setSchoolId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[KEY_SCHOOL_ID] = id
            else prefs.remove(KEY_SCHOOL_ID)
        }
    }

    // ── Campus ID ────────────────────────────────────────────

    val campusId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CAMPUS_ID]
    }

    suspend fun setCampusId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[KEY_CAMPUS_ID] = id
            else prefs.remove(KEY_CAMPUS_ID)
        }
    }

    // ── First Launch ─────────────────────────────────────────

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_FIRST_LAUNCH] = false
        }
    }

    // ── Clear All ────────────────────────────────────────────

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
