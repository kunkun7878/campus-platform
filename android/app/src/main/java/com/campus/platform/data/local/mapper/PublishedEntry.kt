package com.campus.platform.data.local.mapper

/**
 * 统一发布条目 — 用于「我的发布」跨类型列表展示。
 * 每个子类型可转换为 [com.campus.platform.ui.component.MarketFeedItem] 用于统一渲染。
 */
sealed interface PublishedEntry {
    val id: String
    val title: String
    val status: String
    val createdAt: String?
    val entryType: String // "market", "runner", "lost"
    val images: String

    data class Market(val listing: MarketListingDto) : PublishedEntry {
        override val id get() = listing.id
        override val title get() = listing.title
        override val status get() = listing.status
        override val createdAt get() = listing.createdAt
        override val entryType get() = "market"
        override val images get() = listing.images
    }

    data class Runner(val task: RunnerTaskDto) : PublishedEntry {
        override val id get() = task.id
        override val title get() = task.title
        override val status get() = task.status
        override val createdAt get() = task.createdAt
        override val entryType get() = "runner"
        override val images get() = task.images
    }

    data class LostFound(val item: LostFoundItemDto) : PublishedEntry {
        override val id get() = item.id
        override val title get() = item.title
        override val status get() = item.status
        override val createdAt get() = item.createdAt
        override val entryType get() = "lost"
        override val images get() = item.images
    }
}
