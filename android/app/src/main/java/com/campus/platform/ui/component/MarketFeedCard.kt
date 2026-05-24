package com.campus.platform.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.campus.platform.ui.component.runner.RunnerPriceTag

// ---- 枚举 ----

/** 二手商品卡片展示场景 */
enum class MarketCardVariant {
    /** 首页信息流 */
    HOME,
    /** 我发布的 */
    MY_PUBLISHED,
    /** 我的收藏 */
    MY_FAVORITES,
}

// ---- 数据模型 ----

/**
 * 二手商品卡片数据，通过 data class 聚合而非散列参数。
 *
 * @property id             商品唯一标识。
 * @property images         图片 URL 列表，取 [0] 作为卡片封面。
 * @property title          商品标题。
 * @property price          价格字符串，已含 ¥ 前缀，如 "¥15"，可直接传入 [RunnerPriceTag]。
 * @property condition      成色文本，如 "全新"、"九成新"。
 * @property category       分类文本，如 "数码"、"书籍"。
 * @property time           发布时间文本，如 "2小时前"。
 * @property status         商品状态文本（仅 MY_PUBLISHED 场景使用），如 "在售"、"已售出"、"已下架"。
 * @property favoriteCount  收藏数。
 * @property isFavorite     当前用户是否已收藏。
 */
data class MarketFeedItem(
    val id: String,
    val images: List<String>,
    val title: String,
    val price: String,
    val condition: String,
    val category: String,
    val time: String,
    val status: String,
    val favoriteCount: Int,
    val isFavorite: Boolean,
)

// ---- 颜色常量 ----

/** 图片加载失败 / 无图时的 fallback 底色 */
private val FallbackImageBg = Color(0xFFEEF1FF)

/** 在售状态色 */
private val StatusOnSaleColor = Color(0xFF12B7AE)

/** 已售出状态色 */
private val StatusSoldColor = Color(0xFF7C89A6)

/** 已下架状态色 */
private val StatusDelistedColor = Color(0xFFF45F89)

/** 成色标签色 */
private val ConditionTagColor = Color(0xFF2D6BFF)

/** 分类标签色 */
private val CategoryTagColor = Color(0xFFFF9A62)

// ----

/**
 * 根据商品状态文本映射对应的展示颜色。
 */
private fun statusColor(status: String): Color = when (status) {
    "在售" -> StatusOnSaleColor
    "已售出" -> StatusSoldColor
    "已下架" -> StatusDelistedColor
    else -> StatusSoldColor
}

/**
 * 二手商品卡片，通过 [variant] 枚举适配首页 / 我发布的 / 我的收藏三种场景。
 *
 * 白色卡片、16dp 圆角、浅阴影。左侧为封面图（Coil 加载 + 色块 fallback），
 * 右侧为标题 / 价格 / 成色与分类标签 / 底部信息行。
 *
 * @param item               商品数据，通过 [MarketFeedItem] 聚合传入。
 * @param variant            展示场景枚举，控制底部区域与额外按钮。
 * @param onClick            整卡点击回调。
 * @param onFavoriteClick    收藏/取消收藏点击回调（HOME 场景的心形图标）。
 * @param onEditClick        编辑入口回调（MY_PUBLISHED 场景的编辑按钮）。
 * @param onUnfavoriteClick  取消收藏回调（MY_FAVORITES 场景的取消收藏按钮）。
 * @param modifier           修饰符。
 */
@Composable
fun MarketFeedCard(
    item: MarketFeedItem,
    variant: MarketCardVariant,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onUnfavoriteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box {
            // 主体行：左侧封面图 + 右侧信息列
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // 封面图区
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FallbackImageBg),
                    contentAlignment = Alignment.Center,
                ) {
                    val coverUrl = item.images.firstOrNull()
                    if (coverUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(
                                androidx.compose.ui.platform.LocalContext.current,
                            )
                                .data(coverUrl)
                                .build(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    // 无图时保留 FallbackImageBg 色块即可，无需额外占位文字
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧信息列
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    // 标题 — 单行省略
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 价格 — 复用 RunnerPriceTag 的 ¥ 格式化
                    RunnerPriceTag(amount = item.price, tip = null)

                    Spacer(modifier = Modifier.height(6.dp))

                    // 成色 + 分类标签行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TagPill(text = item.condition, color = ConditionTagColor)
                        TagPill(text = item.category, color = CategoryTagColor)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 底部行 — 按 variant 切换
                    when (variant) {
                        MarketCardVariant.HOME -> BottomHomeRow(
                            favoriteCount = item.favoriteCount,
                            isFavorite = item.isFavorite,
                            time = item.time,
                            onFavoriteClick = onFavoriteClick,
                        )
                        MarketCardVariant.MY_PUBLISHED -> BottomPublishedRow(
                            status = item.status,
                            statusColor = statusColor(item.status),
                            onEditClick = onEditClick,
                        )
                        // MY_FAVORITES 底部无额外信息，仅展示时间
                        MarketCardVariant.MY_FAVORITES -> {
                            Text(
                                text = item.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // MY_FAVORITES 专属：右上角"取消收藏"按钮
            if (variant == MarketCardVariant.MY_FAVORITES && onUnfavoriteClick != null) {
                TextButton(
                    onClick = onUnfavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp),
                ) {
                    Text(
                        text = "取消收藏",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

// ---- 内部标签 pill ----

/**
 * 卡片内联小标签，半透明底色 + 实色文字。
 */
@Composable
private fun TagPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

// ---- HOME 底部行 ----

/**
 * HOME 变体的底部行：心形收藏图标 + 收藏数 + 发布时间。
 */
@Composable
private fun BottomHomeRow(
    favoriteCount: Int,
    isFavorite: Boolean,
    time: String,
    onFavoriteClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧：❤ 收藏数
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onFavoriteClick?.invoke() },
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "已收藏" else "收藏",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${favoriteCount}收藏",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 右侧：发布时间
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- MY_PUBLISHED 底部行 ----

/**
 * MY_PUBLISHED 变体的底部行：状态标签 + 编辑入口。
 */
@Composable
private fun BottomPublishedRow(
    status: String,
    statusColor: Color,
    onEditClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 状态标签
        StatusBadge(status = status, color = statusColor)

        // 编辑入口
        if (onEditClick != null) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
