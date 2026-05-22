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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.viewmodel.auth.AccountDeleteUiState
import com.campus.platform.ui.viewmodel.auth.AccountDeleteViewModel

/**
 * 账号注销页。
 *
 * 提供确认对话框风格的全屏注销流程：
 * - 第一步：确认注销 + 原因选择
 * - 第二步：二次确认输入
 * - 调用 deleteAccount() 软删除并退出登录
 *
 * Phase 3：使用 AccountDeleteViewModel 管理状态，通过 navController 导航。
 */
@Composable
fun AccountDeleteScreen(
    viewModel: AccountDeleteViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 注销成功导航
    LaunchedEffect(uiState) {
        if (uiState is AccountDeleteUiState.Success) {
            navController.navigate(CampusRoutes.Login.route) {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // Snackbar 错误
    LaunchedEffect(formState.error) {
        formState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
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
            // ── 标题 ──
            Text(
                text = "注销账号",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )

            // ── Step 1: 选择原因 ──
            if (formState.currentStep == 1) {
                Text(
                    text = "很抱歉看到您离开。注销后您的账号将被停用，\n您发布的内容将保留但不可见。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "请告诉我们您注销的原因（选填）：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )

                viewModel.reasonOptions.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onSelectReason(index) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (formState.selectedReason == index) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }

                // 选"其他"时的补充输入
                if (formState.selectedReason == viewModel.reasonOptions.size - 1) {
                    OutlinedTextField(
                        value = formState.reasonOther,
                        onValueChange = { viewModel.onReasonOtherChange(it) },
                        label = { Text("请说明具体原因") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Button(
                    onClick = { viewModel.onContinueToConfirm() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("继续注销")
                }
            }

            // ── Step 2: 最终确认 ──
            if (formState.currentStep == 2) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "⚠ 请确认以下信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "• 账户将被永久停用，无法恢复",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "• 您发布的内容将保留但不可见",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "• 同手机号可在 30 天后重新注册",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "• 余额和优惠券将被清空，请提前处理",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("我再想想")
                    }
                    Button(
                        onClick = { viewModel.performDelete() },
                        modifier = Modifier.weight(1f),
                        enabled = !formState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        if (formState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        } else {
                            Text("确认注销")
                        }
                    }
                }
            }

            // ── 取消按钮 ──
            if (formState.currentStep == 1) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("取消")
                }
            }
        }
    }
}
