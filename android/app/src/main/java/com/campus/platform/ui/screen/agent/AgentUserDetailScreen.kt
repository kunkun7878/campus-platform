package com.campus.platform.ui.screen.agent

import android.widget.Toast
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.campus.platform.ui.viewmodel.agent.AgentUserDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentUserDetailScreen(
    viewModel: AgentUserDetailViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.actionResult) {
        state.actionResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeActionResult()
        }
    }

    // Ban dialog
    if (state.showBanDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBanDialog() },
            title = { Text("封禁用户") },
            text = { Text("确定要封禁该用户吗？封禁后该用户将无法使用平台功能。") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmBan() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("确认封禁") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissBanDialog() }) { Text("取消") }
            },
        )
    }

    // Unban dialog
    if (state.showUnbanDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnbanDialog() },
            title = { Text("解封用户") },
            text = { Text("确定要解封该用户吗？解封后将恢复正常使用。") },
            confirmButton = {
                Button(onClick = { viewModel.confirmUnban() }) { Text("确认解封") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissUnbanDialog() }) { Text("取消") }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("用户详情") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                state.user != null -> {
                    val user = state.user!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        // User info card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                // Avatar placeholder
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = user.nickname ?: "未设置昵称",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val (statusText, statusColor) = if (user.status == 0) {
                                    "正常" to MaterialTheme.colorScheme.primary
                                } else {
                                    "已封禁" to MaterialTheme.colorScheme.error
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (user.status == 0) Icons.Filled.CheckCircle else Icons.Filled.Block,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = statusColor,
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = statusColor,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                DetailRow("用户 ID", user.id)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                DetailRow("手机号", user.phone ?: "未绑定")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                DetailRow("学校 ID", user.schoolId ?: "未绑定")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                DetailRow("Agent", if (user.isAgent) "是" else "否")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                DetailRow("跑腿员状态", user.runnerStatus)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action buttons
                        if (user.status == 0) {
                            Button(
                                onClick = { viewModel.showBanDialog() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                enabled = !state.actionInProgress,
                            ) {
                                Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("封禁用户")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.showUnbanDialog() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                enabled = !state.actionInProgress,
                            ) {
                                Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("解封用户")
                            }
                        }
                    }
                }
            }

            if (state.actionInProgress) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
