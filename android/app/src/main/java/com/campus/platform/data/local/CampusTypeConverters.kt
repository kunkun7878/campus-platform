package com.campus.platform.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters for the campus platform database.
 *
 * Handles conversions between Kotlin types and SQLite column types,
 * primarily List<String> <-> JSON String for jsonb/image columns.
 */
class CampusTypeConverters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromListToString(list: List<String>?): String {
        return if (list != null) json.encodeToString(list) else "[]"
    }

    @TypeConverter
    fun fromStringToList(value: String?): List<String> {
        if (value.isNullOrBlank() || value == "[]") return emptyList()
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
