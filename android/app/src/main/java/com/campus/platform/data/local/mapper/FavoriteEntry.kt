package com.campus.platform.data.local.mapper

/**
 * 统一收藏条目 — 用于「我的收藏」跨类型列表展示。
 */
sealed interface FavoriteEntry {
    val id: String
    val title: String
    val createdAt: String?
    val entryType: String // "market", "runner", "lost", "post"
    val images: String

    data class Market(val listing: MarketListingDto) : FavoriteEntry {
        override val id get() = listing.id
        override val title get() = listing.title
        override val createdAt get() = listing.createdAt
        override val entryType get() = "market"
        override val images get() = listing.images
    }

    data class Runner(val task: RunnerTaskDto) : FavoriteEntry {
        override val id get() = task.id
        override val title get() = task.title
        override val createdAt get() = task.createdAt
        override val entryType get() = "runner"
        override val images get() = task.images
    }

    data class LostFound(val item: LostFoundItemDto) : FavoriteEntry {
        override val id get() = item.id
        override val title get() = item.title
        override val createdAt get() = item.createdAt
        override val entryType get() = "lost"
        override val images get() = item.images
    }
}
