package com.campus.platform.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 表单卡片容器，提供一致的白色底、圆角、浅阴影、内边距样式。
 *
 * 参照原型中 `form-card` 的视觉风格：
 * - Card: shape = 12dp, elevation = 2dp, color = surface（白色）
 * - 内部 Column: contentPadding = 16dp
 *
 * 不负责表单字段渲染 —— 只提供容器样式。表单内容通过 [content] Slot API 传入。
 *
 * @param modifier 可选修饰符，作用于外层 Card。
 * @param content 表单内容 Composable，接收 [ColumnScope] 作为接收者。
 */
@Composable
fun FormCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}
