package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.LostFoundDao
import com.campus.platform.data.local.mapper.LostFoundClaimDto
import com.campus.platform.data.local.mapper.LostFoundItemDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.ILostFoundRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class LostFoundItemApiDto(
    val id: String,
    @SerialName("publisher_id") val publisherId: String,
    val type: String,
    val title: String,
    val description: String? = null,
    val images: String = "[]",
    val location: String? = null,
    @SerialName("lost_date") val lostDate: String? = null,
    val category: String = "other",
    val status: String = "active",
    @SerialName("school_id") val schoolId: String,
    val reward: Int = 0,
    val contact: String = "站内私信联系",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class LostFoundClaimApiDto(
    val id: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("claimant_id") val claimantId: String,
    @SerialName("proof_description") val proofDescription: String? = null,
    val status: String = "pending",
    @SerialName("school_id") val schoolId: String,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun LostFoundItemApiDto.toMapperDto() = LostFoundItemDto(
    id, publisherId, type, title, description, images, location,
    lostDate, category, status, schoolId, reward, contact, createdAt, updatedAt,
)

private fun LostFoundItemDto.toApiDto() = LostFoundItemApiDto(
    id, publisherId, type, title, description, images, location,
    lostDate, category, status, schoolId, reward, contact, createdAt, updatedAt,
)

private fun LostFoundClaimApiDto.toMapperDto() = LostFoundClaimDto(
    id, itemId, claimantId, proofDescription, status, schoolId,
    resolvedAt, createdAt, updatedAt,
)

private fun LostFoundClaimDto.toApiDto() = LostFoundClaimApiDto(
    id, itemId, claimantId, proofDescription, status, schoolId,
    resolvedAt, createdAt, updatedAt,
)

@Singleton
class LostFoundRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val lostFoundDao: LostFoundDao,
) : ILostFoundRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getItemsBySchool(schoolId: String): Flow<List<LostFoundItemDto>> {
        scope.launch { refreshItems(schoolId) }
        return lostFoundDao.getItemsBySchoolId(schoolId).map { it.map { e -> e.toDto() } }
    }

    override fun getItemsByType(schoolId: String, type: String): Flow<List<LostFoundItemDto>> {
        scope.launch { refreshItems(schoolId) }
        return lostFoundDao.getItemsBySchoolAndType(schoolId, type).map { it.map { e -> e.toDto() } }
    }

    override fun getItemsByCategory(schoolId: String, category: String): Flow<List<LostFoundItemDto>> {
        scope.launch { refreshItems(schoolId) }
        return lostFoundDao.getItemsBySchoolAndCategory(schoolId, category).map { it.map { e -> e.toDto() } }
    }

    override suspend fun getItemById(id: String): LostFoundItemDto? {
        return lostFoundDao.getItemById(id)?.toDto()
    }

    override fun getItemsByPublisher(userId: String): Flow<List<LostFoundItemDto>> {
        return lostFoundDao.getItemsByPublisher(userId).map { it.map { e -> e.toDto() } }
    }

    @Deprecated(
        "Direct PostgREST insert bypasses EdgeFn wallet freeze. " +
        "Use invokeLostItemLifecycle() instead.",
        replaceWith = ReplaceWith(
            "invokeLostItemLifecycle(mapOf(\"action\" to \"publish_item\", ...))"
        )
    )
    override suspend fun publishItem(item: LostFoundItemDto) {
        // Delegate to EdgeFn so wallet freeze is enforced
        val body = buildMap<String, Any?> {
            put("action", "publish_item")
            put("title", item.title)
            put("type", item.type)
            put("description", item.description)
            put("images", item.images)
            put("location", item.location)
            put("lost_date", item.lostDate)
            put("category", item.category)
            put("reward", item.reward)
            put("contact", item.contact)
            put("school_id", item.schoolId)
        }
        val responseText = invokeLostItemLifecycle(body)
        // Parse result to get assigned id, then refresh local cache
        try {
            val json = Json.parseToJsonElement(responseText) as JsonObject
            val itemId = json["item_id"]?.jsonPrimitive?.content
            if (itemId != null) {
                refreshItemById(itemId)
            }
        } catch (_: Exception) {
            // best-effort local refresh; the item exists server-side
        }
    }

    override suspend fun updateItem(id: String, updates: Map<String, Any?>) {
        val result = supabase.postgrest
            .from("lost_found_items")
            .update(updates) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<LostFoundItemApiDto>()
        lostFoundDao.upsertItem(result.toMapperDto().toEntity())
    }

    override suspend fun createClaim(claim: LostFoundClaimDto) {
        val result = supabase.postgrest
            .from("lost_found_claims")
            .insert(claim.toApiDto()) { select() }
            .decodeSingle<LostFoundClaimApiDto>()
        lostFoundDao.upsertClaim(result.toMapperDto().toEntity())
    }

    override fun getClaimsByItemId(itemId: String): Flow<List<LostFoundClaimDto>> {
        return lostFoundDao.getClaimsByItemId(itemId).map { it.map { e -> e.toDto() } }
    }

    override fun getClaimsByClaimant(userId: String): Flow<List<LostFoundClaimDto>> {
        return lostFoundDao.getClaimsByClaimant(userId).map { it.map { e -> e.toDto() } }
    }

    override suspend fun refreshItems(schoolId: String) {
        try {
            val result = supabase.postgrest
                .from("lost_found_items")
                .select { filter { eq("school_id", schoolId) } }
                .decodeList<LostFoundItemApiDto>()
            lostFoundDao.upsertAllItems(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }

    // ── EdgeFn lifecycle ───────────────────────────────────────────

    override suspend fun invokeLostItemLifecycle(body: Map<String, Any?>): String {
        val jsonBody = buildJsonObject {
            body.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Int -> put(key, value)
                    is Long -> put(key, value)
                    is Double -> put(key, value)
                    is Boolean -> put(key, value)
                    null -> {} // skip nulls
                    else -> put(key, value.toString())
                }
            }
        }
        val response = supabase.functions.invoke("lost-item-lifecycle", body = jsonBody)
        return response.bodyAsText()
    }

    // ── Single-item refresh ────────────────────────────────────────

    override suspend fun refreshItemById(id: String) {
        try {
            val result = supabase.postgrest
                .from("lost_found_items")
                .select { filter { eq("id", id) } }
                .decodeSingle<LostFoundItemApiDto>()
            lostFoundDao.upsertItem(result.toMapperDto().toEntity())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "refreshItemById error for $id", e)
        }
    }

    override suspend fun refreshClaimsByItemId(itemId: String) {
        try {
            val result = supabase.postgrest
                .from("lost_found_claims")
                .select { filter { eq("item_id", itemId) } }
                .decodeList<LostFoundClaimApiDto>()
            lostFoundDao.upsertAllClaims(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "refreshClaimsByItemId error for $itemId", e)
        }
    }
}
