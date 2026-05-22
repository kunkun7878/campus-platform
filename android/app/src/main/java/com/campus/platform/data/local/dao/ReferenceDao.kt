package com.campus.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campus.platform.data.local.entity.SchoolEntity
import com.campus.platform.data.local.entity.CampusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceDao {

    @Upsert
    suspend fun upsertAllSchools(schools: List<SchoolEntity>)

    @Query("SELECT * FROM schools ORDER BY createdAt DESC")
    fun getAllSchools(): Flow<List<SchoolEntity>>

    @Query("SELECT * FROM schools WHERE id = :id")
    suspend fun getSchoolById(id: String): SchoolEntity?

    @Query("DELETE FROM schools")
    suspend fun deleteAllSchools()

    @Upsert
    suspend fun upsertAllCampuses(campuses: List<CampusEntity>)

    @Query("SELECT * FROM campuses WHERE schoolId = :schoolId ORDER BY name ASC")
    fun getCampusesBySchoolId(schoolId: String): Flow<List<CampusEntity>>

    @Query("SELECT * FROM campuses WHERE id = :id")
    suspend fun getCampusById(id: String): CampusEntity?

    @Query("DELETE FROM campuses")
    suspend fun deleteAllCampuses()
}
