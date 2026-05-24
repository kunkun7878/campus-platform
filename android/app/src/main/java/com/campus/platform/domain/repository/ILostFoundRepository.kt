package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.LostFoundClaimDto
import com.campus.platform.data.local.mapper.LostFoundItemDto
import kotlinx.coroutines.flow.Flow

interface ILostFoundRepository {

    fun getItemsBySchool(schoolId: String): Flow<List<LostFoundItemDto>>

    fun getItemsByType(schoolId: String, type: String): Flow<List<LostFoundItemDto>>

    fun getItemsByCategory(schoolId: String, category: String): Flow<List<LostFoundItemDto>>

    suspend fun getItemById(id: String): LostFoundItemDto?

    fun getItemsByPublisher(userId: String): Flow<List<LostFoundItemDto>>

    @Deprecated(
        "publishItem() bypasses the EdgeFn wallet freeze logic. " +
        "Use invokeLostItemLifecycle(mapOf(\"action\" to \"publish_item\", ...)) instead.",
        replaceWith = ReplaceWith(
            "invokeLostItemLifecycle(mapOf(\"action\" to \"publish_item\", ...))",
            "com.campus.platform.domain.repository.ILostFoundRepository"
        )
    )
    suspend fun publishItem(item: LostFoundItemDto)

    suspend fun updateItem(id: String, updates: Map<String, Any?>)

    suspend fun createClaim(claim: LostFoundClaimDto)

    fun getClaimsByItemId(itemId: String): Flow<List<LostFoundClaimDto>>

    fun getClaimsByClaimant(userId: String): Flow<List<LostFoundClaimDto>>

    suspend fun refreshItems(schoolId: String)

    // ── EdgeFn lifecycle ──────────────────────────────────────────

    /**
     * 通过 EdgeFn "lost-item-lifecycle" 执行生命周期操作。
     * @param body 请求体，必须包含 "action" 字段（publish_item / approve_claim / reject_claim / resolve_item / close_item）。
     * @return 响应的 JSON 字符串，由调用方自行解析。
     */
    suspend fun invokeLostItemLifecycle(body: Map<String, Any?>): String

    // ── Single-item refresh ───────────────────────────────────────

    /** 从 Supabase 刷新单条 item 并 upsert 到本地 Room */
    suspend fun refreshItemById(id: String)

    /** 从 Supabase 刷新某 item 下所有 claims 并 upsert 到本地 Room */
    suspend fun refreshClaimsByItemId(itemId: String)
}
