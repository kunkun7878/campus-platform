package com.campus.platform.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.auth.AuthValidator
import com.campus.platform.ui.component.CaptchaDialog
import com.campus.platform.ui.component.PasswordStrengthBar
import kotlinx.coroutines.launch

/**
 * 忘记密码页。
 *
 * 当前流程：由于 Supabase Kotlin SDK OTP verify 尚未可用，
 * 暂不提供在线重置密码流程。改为引导用户通过验证码登录后在设置中修改密码。
 *
 * 引导文字：输入手机号确认 → 提示使用验证码登录 → 在设置中修改密码。
 *
 * 注意：Phase 3 SDK 更新后可启用完整 OTP 重置流程（Step 1 验证码 → Step 2 设新密码）。
 *
 * @param authRepository Supabase Auth 仓库
 * @param onNavigateToLogin 返回登录
 */
@Composable
fun PasswordResetScreen(
    authRepository: AuthRepository,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun showError(msg: String) {
        error = msg
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "忘记密码",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 引导说明 ──
            Text(
                text = "暂不支持在线重置密码",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "请使用验证码登录后，在「设置 → 账号安全 → 修改密码」中设置新密码。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 手机号输入（仅用于确认，不实际发送） ──
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.take(11).filter { c -> c.isDigit() } },
                label = { Text("手机号（确认用）") },
                placeholder = { Text("请输入注册时的手机号") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 操作按钮 ──
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("返回登录，使用验证码登录")
            }

            // ── 底部提示 ──
            Text(
                text = "登录后在「设置 → 账号安全」中即可修改密码",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            // ── 返回登录文字链接 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onNavigateToLogin) {
                    Text("← 返回登录")
                }
            }
        }
    }
}
