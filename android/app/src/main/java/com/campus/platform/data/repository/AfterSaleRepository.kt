package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.RunnerDao
import com.campus.platform.data.local.mapper.AfterSaleDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IAfterSaleRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
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
private data class AfterSaleApiDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("requester_id") val requesterId: String,
    val type: String,
    val reason: String,
    val images: String = "[]",
    val status: String = "pending",
    @SerialName("result_comment") val resultComment: String? = null,
    @SerialName("school_id") val schoolId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun AfterSaleApiDto.toMapperDto() = AfterSaleDto(
    id, orderId, requesterId, type, reason, images, status,
    resultComment, schoolId, createdAt, updatedAt,
)

private fun AfterSaleDto.toApiDto() = AfterSaleApiDto(
    id, orderId, requesterId, type, reason, images, status,
    resultComment, schoolId, createdAt, updatedAt,
)

@Singleton
class AfterSaleRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val runnerDao: RunnerDao,
) : IAfterSaleRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getAfterSalesByRequester(userId: String): Flow<List<AfterSaleDto>> {
        scope.launch { refreshAfterSales(userId) }
        return runnerDao.getAfterSalesByRequester(userId).map { it.map { e -> e.toDto() } }
    }

    override fun getAfterSalesByOrderId(orderId: String): Flow<List<AfterSaleDto>> {
        return runnerDao.getAfterSalesByOrderId(orderId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun getAfterSaleById(id: String): AfterSaleDto? {
        return runnerDao.getAfterSaleById(id)?.toDto()
    }

    override suspend fun createAfterSale(afterSale: AfterSaleDto) {
        val result = supabase.postgrest
            .from("after_sales")
            .insert(afterSale.toApiDto()) { select() }
            .decodeSingle<AfterSaleApiDto>()
        runnerDao.upsertAfterSale(result.toMapperDto().toEntity())
    }

    override suspend fun updateAfterSaleStatus(id: String, status: String, resultComment: String?) {
        val updates = buildMap<String, String?> {
            put("status", status)
            if (resultComment != null) put("result_comment", resultComment)
        }
        val result = supabase.postgrest
            .from("after_sales")
            .update(updates) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<AfterSaleApiDto>()
        runnerDao.upsertAfterSale(result.toMapperDto().toEntity())
    }

    override suspend fun refreshAfterSales(userId: String) {
        try {
            val result = supabase.postgrest
                .from("after_sales")
                .select { filter { eq("requester_id", userId) } }
                .decodeList<AfterSaleApiDto>()
            runnerDao.upsertAllAfterSales(result.map { it.toMapperDto().toEntity() })
        } catch (e: Exception) { if (e is CancellationException) throw e }
    }
}
