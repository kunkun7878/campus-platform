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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.PasswordStrengthBar
import com.campus.platform.ui.viewmodel.auth.PasswordResetViewModel

/**
 * 忘记密码页 — 分步重置流程。
 *
 * Step 1: 输入手机号 → 发送验证码（SMS OTP）→ 输入验证码 → 验证
 * Step 2 (OTP 验证成功后): 设置新密码 + 确认密码 → 提交 → Supabase Auth updateUser
 *
 * 成功后提示并返回登录页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordResetScreen(
    viewModel: PasswordResetViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Success → pop back to login
    LaunchedEffect(formState.isSuccess) {
        if (formState.isSuccess) {
            snackbarHostState.showSnackbar("密码重置成功，请使用新密码登录")
            navController.popBackStack()
        }
    }

    LaunchedEffect(formState.error) {
        formState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorDismissed()
        }
    }

    val currentStep = if (!formState.otpVerified) 1 else 2

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("忘记密码") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Progress indicator
            Text(
                text = "Step $currentStep / 2",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / 2f },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Step 1: 手机号 + 验证码 ────────────────────
            if (!formState.otpVerified) {
                OutlinedTextField(
                    value = formState.phone,
                    onValueChange = { viewModel.onPhoneChange(it) },
                    label = { Text("手机号") },
                    placeholder = { Text("请输入注册时的手机号") },
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
                        onClick = { viewModel.verifyOtp() },
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
                            Text("验证")
                        }
                    }
                }
            }

            // ── Step 2: 设置新密码 ─────────────────────────
            if (formState.otpVerified && !formState.isSuccess) {
                Text(
                    text = "设置新密码",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                OutlinedTextField(
                    value = formState.newPassword,
                    onValueChange = { viewModel.onNewPasswordChange(it) },
                    label = { Text("新密码") },
                    placeholder = { Text("至少 8 位，包含字母和数字") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                PasswordStrengthBar(password = formState.newPassword)

                OutlinedTextField(
                    value = formState.confirmPassword,
                    onValueChange = { viewModel.onConfirmPasswordChange(it) },
                    label = { Text("确认新密码") },
                    placeholder = { Text("请再次输入新密码") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = formState.confirmPassword.isNotEmpty() &&
                        formState.newPassword != formState.confirmPassword,
                    supportingText = if (formState.confirmPassword.isNotEmpty() &&
                        formState.newPassword != formState.confirmPassword
                    ) {
                        { Text("两次密码不一致") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = { viewModel.submitNewPassword() },
                    enabled = formState.newPassword.length >= 8 &&
                        formState.confirmPassword.isNotEmpty() &&
                        formState.newPassword == formState.confirmPassword &&
                        !formState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (formState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("重置密码")
                    }
                }
            }

            // ── 成功提示 ──────────────────────────────────
            if (formState.isSuccess) {
                Text(
                    text = "密码重置成功！",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "正在返回登录页…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }

            // ── 底部链接 ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("返回登录", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
