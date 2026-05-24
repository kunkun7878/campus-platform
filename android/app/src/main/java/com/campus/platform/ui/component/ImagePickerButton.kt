package com.campus.platform.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

// ═══════════════════════════════════════════════════════════
// Single-image picker button (for chat input bars)
// ═══════════════════════════════════════════════════════════

/**
 * 单张图片选择按钮 — 点击打开系统相册，选择后回调 [onImageSelected]。
 * 用于聊天输入栏附件按钮。
 */
@Composable
fun ImagePickerButton(
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onImageSelected(uri)
    }

    IconButton(
        onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.AddAPhoto,
            contentDescription = "选择图片",
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

// ═══════════════════════════════════════════════════════════
// Multi-image picker (for posts / lost-found publish)
// ═══════════════════════════════════════════════════════════

/**
 * 多图选择器区域 — 展示已选图片缩略图 + 添加按钮。
 * 最大选择 [maxCount] 张（默认 9）。
 *
 * @param selectedUris 当前已选图片 URI 列表
 * @param onAddImages 新增图片回调（追加到列表）
 * @param onRemoveImage 移除单张图片回调（按 index）
 * @param maxCount 最大可选张数
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiImagePicker(
    selectedUris: List<Uri>,
    onAddImages: (List<Uri>) -> Unit,
    onRemoveImage: (Int) -> Unit,
    maxCount: Int = 9,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxCount - selectedUris.size),
    ) { uris ->
        if (uris.isNotEmpty()) onAddImages(uris)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Title row
        Text(
            text = "图片（${selectedUris.size}/$maxCount）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Thumbnail grid
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Existing thumbnails
            selectedUris.forEachIndexed { index, uri ->
                Box(
                    modifier = Modifier.size(80.dp),
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "已选图片 ${index + 1}",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    // Delete button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(22.dp)
                            .clip(RoundedCornerShape(bottomStart = 8.dp))
                            .background(MaterialTheme.colorScheme.error)
                            .clickable { onRemoveImage(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "移除",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // "Add" button (shown when there are remaining slots)
            if (selectedUris.size < maxCount) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable {
                            launcher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加图片",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "添加",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Helper text
        if (selectedUris.isEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "支持 jpg / png / webp / gif，单张最大 10MB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
