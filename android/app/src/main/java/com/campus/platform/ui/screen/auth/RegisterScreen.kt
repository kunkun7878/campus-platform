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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
 * 注册页 — 分步表单。
 *
 * Step 1: 输入手机号 → 发送验证码（CAPTCHA）→ 输入验证码
 * Step 2: 设置密码 + 密码确认 + 邮箱选填
 * Step 3: 同意协议 → 完成注册
 *
 * 注册成功后回调 onRegisterSuccess，由 NavGraph 引导至选校。
 *
 * @param authRepository Supabase Auth 仓库（Hilt 注入）
 * @param onRegisterSuccess 注册成功回调（跳转选校）
 * @param onNavigateToLogin 返回登录
 */
@Composable
fun RegisterScreen(
    authRepository: AuthRepository,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 步骤
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 3

    // 表单状态
    var phone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var showCaptcha by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // 倒计时
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
    }

    fun showError(msg: String) {
        error = msg
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun sendOtp() {
        AuthValidator.validatePhone(phone)?.let { showError(it); return }
        showCaptcha = true
    }

    fun onCaptchaVerified() {
        showCaptcha = false
        scope.launch {
            isLoading = true
            try {
                authRepository.signInWithOtp(phone)
                otpSent = true
                countdown = 60
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("already registered", ignoreCase = true) ||
                    msg.contains("already exists", ignoreCase = true) ||
                    msg.contains("User already", ignoreCase = true)
                ) {
                    showError("该手机号已注册，请直接登录")
                } else {
                    showError(e.message ?: "发送验证码失败")
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun verifyOtpAndNext() {
        AuthValidator.validateVerificationCode(otpCode)?.let { showError(it); return }
        scope.launch {
            isLoading = true
            try {
                authRepository.verifyOtp(phone, otpCode)
                currentStep = 2
            } catch (e: Exception) {
                showError(e.message ?: "验证码错误或已过期")
            } finally {
                isLoading = false
            }
        }
    }

    fun setPasswordAndNext() {
        AuthValidator.validatePassword(password)?.let { showError(it); return }
        if (password != confirmPassword) { showError("两次密码不一致"); return }
        currentStep = 3
    }

    fun finishRegistration() {
        scope.launch {
            isLoading = true
            try {
                authRepository.signUpWithPhoneAndPassword(phone, password)
                onRegisterSuccess()
            } catch (e: Exception) {
                showError(e.message ?: "注册失败，请稍后重试")
            } finally {
                isLoading = false
            }
        }
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
            // ── 标题 + 进度条 ──
            Text(
                text = "注册账号",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Step $currentStep / $totalSteps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / totalSteps },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Step 1: 手机号 + 验证码 ──
            if (currentStep == 1) {
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
                } else {
                    Button(
                        onClick = { verifyOtpAndNext() },
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
                            Text("下一步")
                        }
                    }
                }
            }

            // ── Step 2: 设置密码 ──
            if (currentStep == 2) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("设置密码") },
                    placeholder = { Text("至少 8 位，包含字母和数字") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                PasswordStrengthBar(password = password)

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认密码") },
                    placeholder = { Text("请再次输入密码") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                    supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                        { Text("两次密码不一致") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 邮箱选填
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱（选填）") },
                    placeholder = { Text("example@campus.edu.cn") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = { setPasswordAndNext() },
                    enabled = password.isNotEmpty()
                        && confirmPassword.isNotEmpty()
                        && password == confirmPassword
                        && password.length >= 8
                        && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("下一步")
                }
            }

            // ── Step 3: 同意协议 ──
            if (currentStep == 3) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "欢迎加入校园聚合平台！",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

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
                    TextButton(onClick = { /* TODO: 用户协议页 */ }) {
                        Text(
                            text = "《用户协议》和《隐私政策》",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Button(
                    onClick = { finishRegistration() },
                    enabled = agreedToTerms && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("完成注册")
                    }
                }
            }

            // ── 底部链接 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "已有账号？",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text("立即登录", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
