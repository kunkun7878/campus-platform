package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.RunnerDao
import com.campus.platform.data.local.mapper.AfterSaleTimelineDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IAfterSaleTimelineRepository
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
private data class AfterSaleTimelineApiDto(
    val id: String,
    @SerialName("after_sale_id") val afterSaleId: String,
    val event: String,
    val description: String? = null,
    @SerialName("operator_id") val operatorId: String? = null,
    @SerialName("school_id") val schoolId: String,
    @SerialName("created_at") val createdAt: String? = null,
)

private fun AfterSaleTimelineApiDto.toMapperDto() = AfterSaleTimelineDto(
    id, afterSaleId, event, description, operatorId, schoolId, createdAt,
)

@Singleton
class AfterSaleTimelineRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val runnerDao: RunnerDao,
) : IAfterSaleTimelineRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getTimelineByAfterSaleId(afterSaleId: String): Flow<List<AfterSaleTimelineDto>> {
        scope.launch { refreshTimeline(afterSaleId) }
        return runnerDao.getTimelineByAfterSaleId(afterSaleId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun refreshTimeline(afterSaleId: String) {
        try {
            val result = supabase.postgrest
                .from("after_sale_timeline")
                .select { filter { eq("after_sale_id", afterSaleId) } }
                .decodeList<AfterSaleTimelineApiDto>()
            runnerDao.deleteTimelineByAfterSaleId(afterSaleId)
            runnerDao.upsertAllAfterSaleTimelineEvents(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }
}
