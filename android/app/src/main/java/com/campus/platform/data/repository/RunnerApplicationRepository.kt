package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.RunnerDao
import com.campus.platform.data.local.mapper.RunnerApplicationDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IRunnerApplicationRepository
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RunnerApplicationApiDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("real_name") val realName: String,
    @SerialName("student_id") val studentId: String,
    val phone: String,
    val reason: String? = null,
    @SerialName("id_card_front") val idCardFront: String? = null,
    @SerialName("id_card_back") val idCardBack: String? = null,
    val status: String = "pending",
    @SerialName("review_comment") val reviewComment: String? = null,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("school_id") val schoolId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun RunnerApplicationApiDto.toMapperDto() = RunnerApplicationDto(
    id, userId, realName, studentId, phone, reason,
    idCardFront, idCardBack, status, reviewComment,
    reviewedBy, reviewedAt, schoolId, createdAt, updatedAt,
)

private fun RunnerApplicationDto.toApiDto() = RunnerApplicationApiDto(
    id, userId, realName, studentId, phone, reason,
    idCardFront, idCardBack, status, reviewComment,
    reviewedBy, reviewedAt, schoolId, createdAt, updatedAt,
)

@Singleton
class RunnerApplicationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val runnerDao: RunnerDao,
) : IRunnerApplicationRepository {

    override suspend fun getMyApplication(userId: String, schoolId: String): RunnerApplicationDto? {
        return runnerDao.getMyApplication(userId, schoolId)?.toDto()
    }

    override suspend fun submitApplication(application: RunnerApplicationDto) {
        val result = supabase.postgrest
            .from("runner_applications")
            .insert(application.toApiDto()) { select() }
            .decodeSingle<RunnerApplicationApiDto>()
        runnerDao.upsertApplication(result.toMapperDto().toEntity())
    }

    override suspend fun getApplicationStatus(userId: String, schoolId: String): String? {
        return runnerDao.getMyApplication(userId, schoolId)?.status
    }

    // ── Agent: review ───────────────────────────────────────────

    override suspend fun getPendingApplications(schoolId: String): List<RunnerApplicationDto> {
        return try {
            val result = supabase.postgrest
                .from("runner_applications")
                .select {
                    filter {
                        eq("school_id", schoolId)
                        eq("status", "pending")
                    }
                }
                .decodeList<RunnerApplicationApiDto>()
            result.map { it.toMapperDto() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "getPendingApplications error", e)
            emptyList()
        }
    }

    override suspend fun approveApplication(applicationId: String, reviewedBy: String, comment: String?) {
        val now = java.time.Instant.now().toString()
        val updates = mutableMapOf<String, Any?>(
            "status" to "approved",
            "reviewed_by" to reviewedBy,
            "reviewed_at" to now,
        )
        comment?.let { updates["review_comment"] = it }
        val result = supabase.postgrest
            .from("runner_applications")
            .update(updates) {
                filter { eq("id", applicationId) }
                select()
            }
            .decodeSingle<RunnerApplicationApiDto>()
        runnerDao.upsertApplication(result.toMapperDto().toEntity())
    }

    override suspend fun rejectApplication(applicationId: String, reviewedBy: String, reason: String) {
        val now = java.time.Instant.now().toString()
        val updates = mapOf<String, Any?>(
            "status" to "rejected",
            "reviewed_by" to reviewedBy,
            "reviewed_at" to now,
            "review_comment" to reason,
        )
        val result = supabase.postgrest
            .from("runner_applications")
            .update(updates) {
                filter { eq("id", applicationId) }
                select()
            }
            .decodeSingle<RunnerApplicationApiDto>()
        runnerDao.upsertApplication(result.toMapperDto().toEntity())
    }
}
