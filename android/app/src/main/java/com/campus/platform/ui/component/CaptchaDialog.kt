package com.campus.platform.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.random.Random

/**
 * 图形验证码弹窗。
 *
 * Phase 2 使用简单数学题实现，后期可升级为滑块验证。
 * 发送 SMS 前弹出此窗口，用户回答正确后才允许发送。
 *
 * 安全说明：当前 CAPTCHA 为客户端侧验证（防基本脚本），
 * 如需防专业爬虫，Phase 4 前迁移为服务端验证
 * （Supabase Edge Function 生成题目 + 校验答案）。
 *
 * @param onDismiss 关闭弹窗回调
 * @param onVerified 验证通过回调
 */
@Composable
fun CaptchaDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
) {
    data class CaptchaProblem(
        val operandA: Int,
        val operator: String,
        val operandB: Int,
        val expected: Int,
    )

    val problem = remember {
        val a = Random.nextInt(1, 20)
        val b = Random.nextInt(1, 20)
        if (Random.nextBoolean()) {
            CaptchaProblem(a, "+", b, a + b)
        } else {
            val big = maxOf(a, b)
            val small = minOf(a, b)
            CaptchaProblem(big, "-", small, big - small)
        }
    }

    val operandA = problem.operandA
    val operator = problem.operator
    val operandB = problem.operandB
    val expected = problem.expected

    var userAnswer by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var remainingAttempts by remember { mutableIntStateOf(3) }

    fun verify() {
        val answer = userAnswer.toIntOrNull()
        if (answer == null) {
            errorMsg = "请输入数字"
            return
        }
        if (answer == expected) {
            onVerified()
        } else {
            remainingAttempts--
            if (remainingAttempts <= 0) {
                onDismiss()
            } else {
                userAnswer = ""
                errorMsg = "回答错误，还剩 $remainingAttempts 次机会"
            }
        }
    }

    /* 简单的背景干扰条纹，增加 OCR 难度 */
    val captchaBackground = remember {
        val r = Random.nextInt(200, 240)
        val g = Random.nextInt(200, 240)
        val b = Random.nextInt(200, 240)
        Color(r, g, b)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "安全验证",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "请计算以下数学题，以验证您不是机器人",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                // 数学题显示区域（带背景干扰）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(captchaBackground)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$operandA  $operator  $operandB  =  ?",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }

                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { userAnswer = it },
                    label = { Text("请输入答案") },
                    placeholder = { Text("输入数字") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = { verify() },
                        modifier = Modifier.weight(1f),
                        enabled = userAnswer.isNotBlank(),
                    ) {
                        Text("验证")
                    }
                }
            }
        }
    }
}
