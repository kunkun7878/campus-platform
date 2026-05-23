package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.RunnerDao
import com.campus.platform.data.local.mapper.RunnerTaskDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IRunnerTaskRepository
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RunnerTaskApiDto(
    val id: String,
    @SerialName("publisher_id") val publisherId: String,
    @SerialName("runner_id") val runnerId: String? = null,
    val type: String,
    val title: String,
    val description: String? = null,
    @SerialName("pickup_addr") val pickupAddr: String? = null,
    @SerialName("delivery_addr") val deliveryAddr: String? = null,
    val price: Int = 0,
    val tip: Int = 0,
    val status: String = "published",
    val deadline: String? = null,
    @SerialName("school_id") val schoolId: String,
    val images: String = "[]",
    @SerialName("gender_restriction") val genderRestriction: String = "any",
    @SerialName("auto_cancel_minutes") val autoCancelMinutes: Int = 20,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun RunnerTaskApiDto.toMapperDto() = RunnerTaskDto(
    id, publisherId, runnerId, type, title, description,
    pickupAddr, deliveryAddr, price, tip, status, deadline,
    schoolId, images, genderRestriction, autoCancelMinutes, createdAt, updatedAt,
)

private fun RunnerTaskDto.toApiDto() = RunnerTaskApiDto(
    id, publisherId, runnerId, type, title, description,
    pickupAddr, deliveryAddr, price, tip, status, deadline,
    schoolId, images, genderRestriction, autoCancelMinutes, createdAt, updatedAt,
)

@Singleton
class RunnerTaskRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val runnerDao: RunnerDao,
) : IRunnerTaskRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getTasksBySchool(schoolId: String): Flow<List<RunnerTaskDto>> {
        scope.launch { refreshTasks(schoolId) }
        return runnerDao.getTasksBySchoolId(schoolId).map { it.map { e -> e.toDto() } }
    }

    override fun getTasksByStatus(schoolId: String, status: String): Flow<List<RunnerTaskDto>> {
        scope.launch { refreshTasks(schoolId) }
        return runnerDao.getTasksBySchoolAndStatus(schoolId, status).map { it.map { e -> e.toDto() } }
    }

    override suspend fun getTaskById(id: String): RunnerTaskDto? {
        return runnerDao.getTaskById(id)?.toDto()
    }

    override fun getTasksByPublisher(userId: String): Flow<List<RunnerTaskDto>> {
        return runnerDao.getTasksByPublisher(userId).map { it.map { e -> e.toDto() } }
    }

    override fun getTasksByRunner(userId: String): Flow<List<RunnerTaskDto>> {
        return runnerDao.getTasksByRunner(userId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun publishTask(task: RunnerTaskDto) {
        val result = supabase.postgrest
            .from("runner_tasks")
            .insert(task.toApiDto()) { select() }
            .decodeSingle<RunnerTaskApiDto>()
        runnerDao.upsertTask(result.toMapperDto().toEntity())
    }

    override suspend fun updateTask(id: String, updates: Map<String, Any?>) {
        val result = supabase.postgrest
            .from("runner_tasks")
            .update(updates) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<RunnerTaskApiDto>()
        runnerDao.upsertTask(result.toMapperDto().toEntity())
    }

    override suspend fun refreshTasks(schoolId: String) {
        try {
            val result = supabase.postgrest
                .from("runner_tasks")
                .select { filter { eq("school_id", schoolId) } }
                .decodeList<RunnerTaskApiDto>()
            runnerDao.upsertAllTasks(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }
}
