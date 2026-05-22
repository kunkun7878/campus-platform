package com.campus.platform.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.campus.platform.data.auth.AuthValidator

/**
 * 密码强度指示条。
 *
 * 实时计算密码强度并在下方显示进度条和反馈文字。
 * 强度分为三级：弱（红色）、中（黄色）、强（绿色）。
 *
 * @param password 当前输入的密码文本
 */
@Composable
fun PasswordStrengthBar(
    password: String,
    modifier: Modifier = Modifier,
) {
    if (password.isBlank()) return

    val strength = AuthValidator.evaluatePasswordStrength(password)

    val targetProgress = when (strength) {
        AuthValidator.PasswordStrength.Weak -> 0.33f
        AuthValidator.PasswordStrength.Medium -> 0.66f
        AuthValidator.PasswordStrength.Strong -> 1f
    }

    val targetColor = when (strength) {
        AuthValidator.PasswordStrength.Weak -> Color(0xFFF45F89)
        AuthValidator.PasswordStrength.Medium -> Color(0xFFFFB020)
        AuthValidator.PasswordStrength.Strong -> Color(0xFF12B7AE)
    }

    val animatedColor by animateColorAsState(targetValue = targetColor, label = "strengthColor")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LinearProgressIndicator(
            progress = { targetProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = animatedColor,
            trackColor = animatedColor.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "密码强度：${strength.label}",
                style = MaterialTheme.typography.bodySmall,
                color = animatedColor,
            )
            Text(
                text = strength.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
