package com.campus.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey val id: String,
    val name: String,
    val abbreviation: String? = null,
    val city: String? = null,
    val province: String? = null,
    val createdAt: String? = null,
)

@Entity(tableName = "campuses")
data class CampusEntity(
    @PrimaryKey val id: String,
    val schoolId: String,
    val name: String,
    val address: String? = null,
    val createdAt: String? = null,
)
