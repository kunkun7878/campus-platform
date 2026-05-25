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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.component.CaptchaDialog
import com.campus.platform.ui.component.PasswordStrengthBar
import com.campus.platform.ui.viewmodel.auth.RegisterUiState
import com.campus.platform.ui.viewmodel.auth.RegisterViewModel

/**
 * 注册页 — 分步表单。
 *
 * Step 1: 输入手机号 → 发送验证码（CAPTCHA）→ 输入验证码
 * Step 2: 设置密码 + 密码确认 + 邮箱选填
 * Step 3: 同意协议 → 完成注册
 *
 * 注册成功后跳转选校。
 *
 * Phase 3：使用 RegisterViewModel 管理状态，通过 navController 导航。
 */
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 注册成功导航
    LaunchedEffect(uiState) {
        if (uiState is RegisterUiState.Success) {
            navController.navigate(CampusRoutes.SchoolSelect.route) {
                popUpTo(CampusRoutes.Login.route) { inclusive = true }
            }
        }
    }

    // Snackbar 错误
    LaunchedEffect(formState.error) {
        formState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // CAPTCHA 弹窗
    if (formState.showCaptcha) {
        CaptchaDialog(
            onDismiss = { viewModel.onCaptchaDismiss() },
            onVerified = { viewModel.onCaptchaVerified() },
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
                text = "Step ${formState.currentStep} / ${formState.totalSteps}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { formState.currentStep.toFloat() / formState.totalSteps },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Step 1: 手机号 + 验证码 ──
            if (formState.currentStep == 1) {
                OutlinedTextField(
                    value = formState.phone,
                    onValueChange = { viewModel.onPhoneChange(it) },
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
                    value = formState.otpCode,
                    onValueChange = { viewModel.onOtpCodeChange(it) },
                    label = { Text("验证码") },
                    placeholder = { Text("请输入 6 位验证码") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (formState.countdown > 0) {
                            Text(
                                text = "${formState.countdown}s",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        } else if (formState.otpSent) {
                            TextButton(onClick = { viewModel.sendOtp() }) {
                                Text("重新获取", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!formState.otpSent) {
                    OutlinedButton(
                        onClick = { viewModel.sendOtp() },
                        enabled = formState.phone.length == 11 && !formState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("获取验证码")
                    }
                } else {
                    Button(
                        onClick = { viewModel.verifyOtpAndNext() },
                        enabled = formState.otpCode.length == 6 && !formState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (formState.isLoading) {
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
            if (formState.currentStep == 2) {
                OutlinedTextField(
                    value = formState.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
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

                PasswordStrengthBar(password = formState.password)

                OutlinedTextField(
                    value = formState.confirmPassword,
                    onValueChange = { viewModel.onConfirmPasswordChange(it) },
                    label = { Text("确认密码") },
                    placeholder = { Text("请再次输入密码") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = formState.confirmPassword.isNotEmpty() &&
                        formState.password != formState.confirmPassword,
                    supportingText = if (formState.confirmPassword.isNotEmpty() &&
                        formState.password != formState.confirmPassword
                    ) {
                        { Text("两次密码不一致") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 邮箱选填
                OutlinedTextField(
                    value = formState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
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
                    onClick = { viewModel.setPasswordAndNext() },
                    enabled = formState.password.isNotEmpty()
                        && formState.confirmPassword.isNotEmpty()
                        && formState.password == formState.confirmPassword
                        && formState.password.length >= 8
                        && !formState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("下一步")
                }
            }

            // ── Step 3: 同意协议 ──
            if (formState.currentStep == 3) {
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
                        checked = formState.agreedToTerms,
                        onCheckedChange = { viewModel.onAgreedToTermsChange(it) },
                    )
                    Text(
                        text = "我已阅读并同意",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { navController.navigate(CampusRoutes.UserAgreement.route) }) {
                        Text(
                            text = "《用户协议》和《隐私政策》",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Button(
                    onClick = { viewModel.finishRegistration() },
                    enabled = formState.agreedToTerms && !formState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (formState.isLoading) {
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
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("立即登录", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
