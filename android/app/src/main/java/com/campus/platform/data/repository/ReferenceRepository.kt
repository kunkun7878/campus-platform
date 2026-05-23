package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.ReferenceDao
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.data.model.Campus
import com.campus.platform.data.model.School
import com.campus.platform.domain.repository.IReferenceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val referenceDao: ReferenceDao,
) : IReferenceRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getSchools(): Flow<List<School>> {
        scope.launch { refreshSchools() }
        return referenceDao.getAllSchools().map { entities ->
            entities.map { it.toDto() }
        }
    }

    override fun getCampusesBySchoolId(schoolId: String): Flow<List<Campus>> {
        scope.launch { refreshCampuses(schoolId) }
        return referenceDao.getCampusesBySchoolId(schoolId).map { entities ->
            entities.map { it.toDto() }
        }
    }

    override suspend fun refreshSchools() {
        try {
            val schools: List<School> = supabase.postgrest
                .from("schools")
                .select()
                .decodeList<School>()
            referenceDao.upsertAllSchools(schools.map { it.toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }

    override suspend fun refreshCampuses(schoolId: String) {
        try {
            val campuses: List<Campus> = supabase.postgrest
                .from("campuses")
                .select { filter { eq("school_id", schoolId) } }
                .decodeList<Campus>()
            referenceDao.upsertAllCampuses(campuses.map { it.toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }

    override suspend fun getCampusById(id: String): Campus? {
        return referenceDao.getCampusById(id)?.toDto()
    }

    override suspend fun getSchoolById(id: String): School? {
        return referenceDao.getSchoolById(id)?.toDto()
    }
}
