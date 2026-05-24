package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.RunnerDao
import com.campus.platform.data.local.mapper.OrderTimelineDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IOrderTimelineRepository
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
private data class OrderTimelineApiDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    val event: String,
    val description: String? = null,
    @SerialName("operator_id") val operatorId: String? = null,
    @SerialName("school_id") val schoolId: String,
    @SerialName("created_at") val createdAt: String? = null,
)

private fun OrderTimelineApiDto.toMapperDto() = OrderTimelineDto(
    id, orderId, event, description, operatorId, schoolId, createdAt,
)

@Singleton
class OrderTimelineRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val runnerDao: RunnerDao,
) : IOrderTimelineRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getTimelineByOrderId(orderId: String): Flow<List<OrderTimelineDto>> {
        scope.launch { refreshTimeline(orderId) }
        return runnerDao.getTimelineByOrderId(orderId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun refreshTimeline(orderId: String) {
        try {
            val result = supabase.postgrest
                .from("order_timeline")
                .select { filter { eq("order_id", orderId) } }
                .decodeList<OrderTimelineApiDto>()
            runnerDao.deleteTimelineByOrderId(orderId)
            runnerDao.upsertAllTimelineEvents(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }
}
