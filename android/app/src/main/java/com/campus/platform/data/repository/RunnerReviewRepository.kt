package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.RunnerDao
import com.campus.platform.data.local.mapper.RunnerReviewDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IRunnerReviewRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RunnerReviewApiDto(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("reviewer_id") val reviewerId: String,
    @SerialName("reviewee_id") val revieweeId: String,
    val rating: Int,
    val comment: String? = null,
    @SerialName("school_id") val schoolId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun RunnerReviewApiDto.toMapperDto() = RunnerReviewDto(
    id, orderId, reviewerId, revieweeId, rating, comment,
    schoolId, createdAt, updatedAt,
)

private fun RunnerReviewDto.toApiDto() = RunnerReviewApiDto(
    id, orderId, reviewerId, revieweeId, rating, comment,
    schoolId, createdAt, updatedAt,
)

@Singleton
class RunnerReviewRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val runnerDao: RunnerDao,
) : IRunnerReviewRepository {

    override fun getReviewsByOrder(orderId: String): Flow<List<RunnerReviewDto>> {
        return runnerDao.getReviewsByOrderId(orderId).map { it.map { e -> e.toDto() } }
    }

    override fun getReviewsByReviewee(userId: String): Flow<List<RunnerReviewDto>> {
        return runnerDao.getReviewsByReviewee(userId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun createReview(review: RunnerReviewDto) {
        val result = supabase.postgrest
            .from("runner_reviews")
            .insert(review.toApiDto()) { select() }
            .decodeSingle<RunnerReviewApiDto>()
        runnerDao.upsertReview(result.toMapperDto().toEntity())
    }
}
