package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.RunnerDao
import com.campus.platform.data.local.mapper.RunnerOrderDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IRunnerOrderRepository
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
private data class RunnerOrderApiDto(
    val id: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("runner_id") val runnerId: String,
    val status: String = "accepted",
    @SerialName("cancel_reason") val cancelReason: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("expected_at") val expectedAt: String? = null,
    @SerialName("school_id") val schoolId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun RunnerOrderApiDto.toMapperDto() = RunnerOrderDto(
    id, taskId, buyerId, runnerId, status, cancelReason,
    completedAt, expectedAt, schoolId, createdAt, updatedAt,
)

private fun RunnerOrderDto.toApiDto() = RunnerOrderApiDto(
    id, taskId, buyerId, runnerId, status, cancelReason,
    completedAt, expectedAt, schoolId, createdAt, updatedAt,
)

@Singleton
class RunnerOrderRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val runnerDao: RunnerDao,
) : IRunnerOrderRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getOrdersByBuyer(userId: String): Flow<List<RunnerOrderDto>> {
        return runnerDao.getOrdersByBuyer(userId).map { it.map { e -> e.toDto() } }
    }

    override fun getOrdersByRunner(userId: String): Flow<List<RunnerOrderDto>> {
        return runnerDao.getOrdersByRunner(userId).map { it.map { e -> e.toDto() } }
    }

    override fun getOrdersByTaskId(taskId: String): Flow<List<RunnerOrderDto>> {
        return runnerDao.getOrdersByTaskId(taskId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun getOrderById(id: String): RunnerOrderDto? {
        return runnerDao.getOrderById(id)?.toDto()
    }

    override suspend fun createOrder(order: RunnerOrderDto) {
        val result = supabase.postgrest
            .from("runner_orders")
            .insert(order.toApiDto()) { select() }
            .decodeSingle<RunnerOrderApiDto>()
        runnerDao.upsertOrder(result.toMapperDto().toEntity())
    }

    override suspend fun updateOrderStatus(id: String, status: String) {
        val result = supabase.postgrest
            .from("runner_orders")
            .update(mapOf("status" to status)) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<RunnerOrderApiDto>()
        runnerDao.upsertOrder(result.toMapperDto().toEntity())
    }

    override suspend fun refreshOrders(schoolId: String) {
        try {
            val result = supabase.postgrest
                .from("runner_orders")
                .select { filter { eq("school_id", schoolId) } }
                .decodeList<RunnerOrderApiDto>()
            runnerDao.upsertAllOrders(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }
}
