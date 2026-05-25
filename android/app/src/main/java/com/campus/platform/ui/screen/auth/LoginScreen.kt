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
import com.campus.platform.ui.viewmodel.auth.LoginUiState
import com.campus.platform.ui.viewmodel.auth.LoginViewModel

/**
 * 登录页。
 *
 * 支持两种登录模式切换：手机号+SMS OTP / 手机号+密码。
 * SMS 发送前弹出图形验证码（CAPTCHA）。
 *
 * Phase 3：使用 LoginViewModel 管理状态，通过 navController 导航。
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 登录成功导航
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            navController.navigate(CampusRoutes.Splash.route)
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
                TextButton(onClick = { viewModel.onToggleLoginMode(true) }) {
                    Text(
                        text = "验证码登录",
                        fontWeight = if (formState.useOtpLogin) FontWeight.Bold else FontWeight.Normal,
                        color = if (formState.useOtpLogin) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "|",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { viewModel.onToggleLoginMode(false) }) {
                    Text(
                        text = "密码登录",
                        fontWeight = if (!formState.useOtpLogin) FontWeight.Bold else FontWeight.Normal,
                        color = if (!formState.useOtpLogin) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── 手机号输入 ──
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

            // ── OTP 模式 ──
            if (formState.useOtpLogin) {
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
                }

                // 同意协议（OTP 模式）
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
                            text = "《用户协议》",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (formState.otpSent) {
                    Button(
                        onClick = { viewModel.loginWithOtp() },
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
                            Text("登录")
                        }
                    }
                }
            }

            // ── 密码模式 ──
            if (!formState.useOtpLogin) {
                OutlinedTextField(
                    value = formState.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
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
                            text = "《用户协议》",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // 密码错误 >= 5 次提示
                if (formState.passwordErrorCount >= 5) {
                    Text(
                        text = if (formState.passwordErrorCount >= 8) "密码错误次数过多，已锁定60秒"
                        else "密码错误次数过多，建议使用验证码登录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = { viewModel.loginWithPassword() },
                    enabled = formState.phone.length == 11 &&
                        formState.password.isNotBlank() &&
                        !formState.isLoading &&
                        formState.lockoutSeconds == 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (formState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else if (formState.lockoutSeconds > 0) {
                        Text("已锁定 ${formState.lockoutSeconds}s")
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
                TextButton(onClick = {
                    navController.navigate(CampusRoutes.PasswordReset.route)
                }) {
                    Text("忘记密码？", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = {
                    navController.navigate(CampusRoutes.Register.route)
                }) {
                    Text("注册账号", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
