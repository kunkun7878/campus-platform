package com.campus.platform.ui.component.message

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.campus.platform.ui.component.ImagePickerButton
import com.campus.platform.ui.theme.Primary
import com.campus.platform.ui.theme.SurfaceVariant

/**
 * 聊天输入栏 — 文本输入 + 可选图片选择按钮 + 发送按钮。
 *
 * @param text 当前输入内容
 * @param onTextChange 输入变化回调
 * @param onSend 发送回调
 * @param isSending 是否正在发送（显示 loading 替代发送图标）
 * @param placeholder 输入框中占位文本
 * @param onPickImage 可选 — 点击图片按钮回调，传 null 隐藏图片按钮
 * @param isUploadingImage 可选 — 正在上传图片时禁用图片按钮
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    placeholder: String = "输入消息...",
    modifier: Modifier = Modifier,
    onPickImage: ((Uri) -> Unit)? = null,
    isUploadingImage: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Optional image picker button
        if (onPickImage != null) {
            ImagePickerButton(
                onImageSelected = onPickImage,
                enabled = !isUploadingImage,
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = SurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (text.isNotBlank()) onSend()
                },
            ),
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (isSending || isUploadingImage) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Primary,
                strokeWidth = 2.dp,
            )
        } else {
            IconButton(
                onClick = {
                    if (text.isNotBlank()) onSend()
                },
                enabled = text.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = if (text.isNotBlank()) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
