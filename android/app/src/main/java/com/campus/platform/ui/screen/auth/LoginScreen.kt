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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.auth.AuthValidator
import com.campus.platform.ui.component.CaptchaDialog
import kotlinx.coroutines.launch

/**
 * 登录页。
 *
 * 支持两种登录模式切换：手机号+SMS OTP / 手机号+密码。
 * SMS 发送前弹出图形验证码（CAPTCHA）。
 *
 * @param authRepository Supabase Auth 仓库（Hilt 注入）
 * @param onLoginSuccess 登录成功回调（由 NavGraph 导航处理）
 * @param onNavigateToRegister 跳转注册
 * @param onNavigateToPasswordReset 跳转忘记密码
 */
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToPasswordReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 表单状态
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var useOtpLogin by remember { mutableStateOf(true) }  // 默认 OTP 模式
    var agreedToTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var otpSent by remember { mutableStateOf(false) }

    // 倒计时
    var countdown by remember { mutableIntStateOf(0) }
    var showCaptcha by remember { mutableStateOf(false) }
    var captchaForPasswordLogin by remember { mutableStateOf(false) }

    // 倒计时 LaunchedEffect
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
    }

    // 错误显示
    var error by remember { mutableStateOf<String?>(null) }

    // ── 本地密码错误计数 + 锁定倒计时（客户端反馈，不与 GOTRUE 服务端锁竞态） ──
    var passwordErrorCount by remember { mutableIntStateOf(0) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            lockoutSeconds--
        }
    }

    fun showError(msg: String) {
        error = msg
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun sendOtp() {
        AuthValidator.validatePhone(phone)?.let { showError(it); return }
        showCaptcha = true
    }

    fun performPasswordLogin() {
        scope.launch {
            isLoading = true
            try {
                authRepository.signInWithPhoneAndPassword(phone, password)
                passwordErrorCount = 0
                lockoutSeconds = 0
                error = null
                onLoginSuccess()
            } catch (e: Exception) {
                passwordErrorCount++
                val msg = when {
                    passwordErrorCount >= 8 -> {
                        lockoutSeconds = 60
                        "密码错误次数过多，已锁定60秒，请稍后再试"
                    }
                    passwordErrorCount >= 5 -> {
                        "密码错误次数过多，建议使用验证码登录（${e.message ?: "密码错误"}）"
                    }
                    else -> e.message ?: "登录失败，请检查手机号和密码"
                }
                showError(msg)
            } finally {
                isLoading = false
            }
        }
    }

    fun onCaptchaVerified() {
        showCaptcha = false
        if (captchaForPasswordLogin) {
            captchaForPasswordLogin = false
            performPasswordLogin()
            return
        }
        scope.launch {
            isLoading = true
            try {
                authRepository.signInWithOtp(phone)
                otpSent = true
                countdown = 60
            } catch (e: Exception) {
                showError(e.message ?: "发送验证码失败")
            } finally {
                isLoading = false
            }
        }
    }

    fun loginWithOtp() {
        AuthValidator.validateVerificationCode(otpCode)?.let { showError(it); return }
        scope.launch {
            isLoading = true
            try {
                authRepository.verifyOtp(phone, otpCode)
                onLoginSuccess()
            } catch (e: Exception) {
                showError(e.message ?: "验证码错误或已过期")
            } finally {
                isLoading = false
            }
        }
    }

    fun loginWithPassword() {
        AuthValidator.validatePhone(phone)?.let { showError(it); return }
        if (password.isBlank()) { showError("请输入密码"); return }
        if (!agreedToTerms) { showError("请先同意用户协议"); return }
        captchaForPasswordLogin = true
        showCaptcha = true
    }

    if (showCaptcha) {
        CaptchaDialog(
            onDismiss = { showCaptcha = false },
            onVerified = { onCaptchaVerified() },
        )
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
            // ── 品牌标题 ──
            Text(
                text = "校园聚合平台",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "连接校园生活的每一面",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── 登录模式切换 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { useOtpLogin = true; otpSent = false }) {
                    Text(
                        text = "验证码登录",
                        fontWeight = if (useOtpLogin) FontWeight.Bold else FontWeight.Normal,
                        color = if (useOtpLogin) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "|",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { useOtpLogin = false }) {
                    Text(
                        text = "密码登录",
                        fontWeight = if (!useOtpLogin) FontWeight.Bold else FontWeight.Normal,
                        color = if (!useOtpLogin) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── 手机号输入 ──
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.take(11).filter { c -> c.isDigit() } },
                label = { Text("手机号") },
                placeholder = { Text("请输入 11 位手机号") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── OTP 模式 ──
            if (useOtpLogin) {
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it.take(6).filter { c -> c.isDigit() } },
                    label = { Text("验证码") },
                    placeholder = { Text("请输入 6 位验证码") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (countdown > 0) {
                            Text(
                                text = "${countdown}s",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        } else if (otpSent) {
                            TextButton(onClick = { sendOtp() }) {
                                Text("重新获取", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!otpSent) {
                    OutlinedButton(
                        onClick = { sendOtp() },
                        enabled = phone.length == 11 && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("获取验证码")
                    }
                }

                // 同意协议（OTP 模式）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                    )
                    Text(
                        text = "我已阅读并同意",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { /* TODO: 打开协议页面 */ }) {
                        Text(
                            text = "《用户协议》",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (otpSent) {
                    Button(
                        onClick = {
                            if (!agreedToTerms) { showError("请先同意用户协议"); return@Button }
                            loginWithOtp()
                        },
                        enabled = otpCode.length == 6 && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("登录")
                        }
                    }
                }
            }

            // ── 密码模式 ──
            if (!useOtpLogin) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    placeholder = { Text("请输入密码") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                // 同意协议
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                    )
                    Text(
                        text = "我已阅读并同意",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { /* TODO: 打开协议页面 */ }) {
                        Text(
                            text = "《用户协议》",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // 密码错误 >= 5 次提示
                if (passwordErrorCount >= 5) {
                    Text(
                        text = if (passwordErrorCount >= 8) "密码错误次数过多，已锁定60秒"
                        else "密码错误次数过多，建议使用验证码登录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = { loginWithPassword() },
                    enabled = phone.length == 11 && password.isNotBlank() && !isLoading && lockoutSeconds == 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else if (lockoutSeconds > 0) {
                        Text("已锁定 ${lockoutSeconds}s")
                    } else {
                        Text("登录")
                    }
                }
            }

            // ── 辅助操作 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onNavigateToPasswordReset) {
                    Text("忘记密码？", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onNavigateToRegister) {
                    Text("注册账号", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
