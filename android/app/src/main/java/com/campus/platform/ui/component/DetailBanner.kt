package com.campus.platform.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

/** 浅紫装饰色，用于渐变收尾 */
private val BannerPurpleEnd = Color(0xFFE8E0FF)

/**
 * 详情页顶部装饰区，浅蓝紫渐变背景 + 装饰圆 + 可选图片与标签。
 *
 * 视觉风格映射自原型 market-visual：
 * - 渐变从 primaryContainer 过渡到浅紫色再到 background
 * - 散布半透明装饰圆，增加层次感
 * - 标签 pill 使用半透明白色背景
 *
 * @param imageUrl        可选的商品/内容图片 URL，通过 Coil 加载。
 * @param tag             可选的标签文本，如 "二手"、"九成新"。为 null 或空白时不显示。
 * @param placeholderColor 无图片时的占位底色，默认 primaryContainer。
 * @param modifier        修饰符。
 */
@Composable
fun DetailBanner(
    imageUrl: String? = null,
    tag: String? = null,
    placeholderColor: Color = MaterialTheme.colorScheme.primaryContainer,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        BannerPurpleEnd,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        // 装饰圆 — 散布不同大小与透明度的圆，增加空间层次感
        // 左上大圆
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = (-20).dp, y = (-30).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
        )
        // 右上小圆
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = 260.dp, y = 10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
        )
        // 右下中圆
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = 200.dp, y = 110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
        )
        // 左下小圆
        Box(
            modifier = Modifier
                .size(60.dp)
                .offset(x = 30.dp, y = 140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.07f)),
        )

        // 可选图片区 — 居中展示，无图片时显示占位色块
        if (!imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 8.dp)
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(imageUrl)
                        .build(),
                    contentDescription = "详情图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        } else {
            // 无图时的占位色块
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 8.dp)
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(placeholderColor.copy(alpha = 0.4f)),
            )
        }

        // 可选标签 pill — 底部居中
        if (!tag.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.72f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
