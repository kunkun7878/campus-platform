package com.campus.platform.domain.repository

import com.campus.platform.data.model.Campus
import com.campus.platform.data.model.School
import kotlinx.coroutines.flow.Flow

interface IReferenceRepository {

    fun getSchools(): Flow<List<School>>

    fun getCampusesBySchoolId(schoolId: String): Flow<List<Campus>>

    suspend fun refreshSchools()

    suspend fun refreshCampuses(schoolId: String)

    suspend fun getCampusById(id: String): Campus?

    suspend fun getSchoolById(id: String): School?
}
