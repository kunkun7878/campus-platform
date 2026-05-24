package com.campus.platform.ui.component

import com.campus.platform.data.local.entity.MarketListingEntity
import kotlinx.serialization.json.Json

/**
 * 二手市场 UI 共享映射工具。
 *
 * 将 [MarketListingEntity] 中的英文字段码映射为中文展示文本，
 * 同时提供 JSON 图片解析与时间格式化等纯函数映射。
 *
 * 供 HomeScreen、MyFavoritesScreen、MyPublishedScreen 统一引用，
 * 避免各文件重复定义相同的映射逻辑。
 */
object MarketUiMapper {

    private val json = Json { ignoreUnknownKeys = true }

    /** 将英文状态码映射为中文展示文本 */
    fun statusDisplay(status: String): String = when (status) {
        MarketListingEntity.STATUS_ACTIVE -> "在售"
        MarketListingEntity.STATUS_SOLD -> "已售出"
        MarketListingEntity.STATUS_RESERVED -> "已预定"
        MarketListingEntity.STATUS_CANCELLED -> "已下架"
        else -> status
    }

    // ── 成色映射常量 ──────────────────────────────────────────

    private const val ZH_BRAND_NEW = "全新"
    private const val ZH_LIKE_NEW = "九成新"
    private const val ZH_GOOD = "八成新"
    private const val ZH_FAIR = "七成新"
    private const val ZH_POOR = "六成新及以下"

    private const val EN_BRAND_NEW = "brand_new"
    private const val EN_LIKE_NEW = "like_new"
    private const val EN_GOOD = "good"
    private const val EN_FAIR = "fair"
    private const val EN_POOR = "poor"

    /** 将中文成色映射为英文数据库值（提交用） */
    fun toEnglishCondition(zh: String): String = when (zh) {
        ZH_BRAND_NEW -> EN_BRAND_NEW
        ZH_LIKE_NEW -> EN_LIKE_NEW
        ZH_GOOD -> EN_GOOD
        ZH_FAIR -> EN_FAIR
        ZH_POOR -> EN_POOR
        else -> zh // 已经是英文或未知值，透传
    }

    /** 将英文数据库值映射为中文展示文本（显示用） */
    fun conditionDisplay(en: String): String = when (en) {
        EN_BRAND_NEW -> ZH_BRAND_NEW
        EN_LIKE_NEW -> ZH_LIKE_NEW
        EN_GOOD -> ZH_GOOD
        EN_FAIR -> ZH_FAIR
        EN_POOR -> ZH_POOR
        else -> en // 未知值透传
    }

    /** 解析 JSON 字符串数组为 [List<String>]，失败返回空列表 */
    fun parseImages(raw: String): List<String> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 格式化 ISO 时间为简短展示文本 */
    fun formatTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            iso.substring(0, minOf(16, iso.length)).replace("T", " ")
        } catch (_: Exception) {
            iso ?: ""
        }
    }
}
