package com.campus.platform.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.campus.platform.data.local.entity.LostFoundClaimEntity
import com.campus.platform.data.local.entity.LostFoundItemEntity
import com.campus.platform.data.local.mapper.LostFoundClaimDto
import com.campus.platform.navigation.LostClaim
import com.campus.platform.ui.component.StatusBadge
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.home.LostDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostDetailScreen(
    viewModel: LostDetailViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val item by viewModel.item.collectAsStateWithLifecycle()
    val claims by viewModel.claims.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Start observing claims
    LaunchedEffect(Unit) {
        viewModel.startObservingClaims()
    }

    // Handle action errors
    LaunchedEffect(actionError) {
        actionError?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearActionError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (item?.type == "found") "招领详情" else "失物详情"
                    )
                },
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
    ) { paddingValues ->
        when (uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    val errorMsg = (uiState as UiState.Error).message
                    val isItemDeleted = errorMsg.contains("不存在") || errorMsg.contains("已删除")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    ) {
                        Icon(
                            imageVector = if (isItemDeleted) Icons.Default.Info else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isItemDeleted)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isItemDeleted) "启事不存在" else "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isItemDeleted)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isItemDeleted)
                                "该启事可能已被发布者删除或关闭"
                            else
                                "请检查网络后重试",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadItem() }) {
                            Text(if (isItemDeleted) "刷新" else "重试")
                        }
                    }
                }
            }
            is UiState.Success -> {
                val currentItem = item ?: return@Scaffold
                val isPublisher = currentUserId != null && currentUserId == currentItem.publisherId
                val isClosed = currentItem.status == LostFoundItemEntity.STATUS_CLOSED
                val isClaimed = currentItem.status == LostFoundItemEntity.STATUS_CLAIMED

                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // ── Visual banner ──────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(
                                if (currentItem.type == "found")
                                    Color(0xFFE7FBF7)
                                else
                                    Color(0xFFFFF3E6)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (currentItem.type == "found") "招领启事" else "失物启事",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (currentItem.type == "found")
                                    Color(0xFF0A7A5E)
                                else
                                    Color(0xFFB05E00),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusBadge(
                                status = when (currentItem.status) {
                                    LostFoundItemEntity.STATUS_ACTIVE -> "寻找中"
                                    LostFoundItemEntity.STATUS_CLAIMED -> "已认领"
                                    LostFoundItemEntity.STATUS_CLOSED -> "已关闭"
                                    else -> currentItem.status
                                },
                                color = when (currentItem.status) {
                                    LostFoundItemEntity.STATUS_ACTIVE -> Color(0xFF0A7A5E)
                                    LostFoundItemEntity.STATUS_CLAIMED -> Color(0xFF1565C0)
                                    LostFoundItemEntity.STATUS_CLOSED -> Color(0xFF9E9E9E)
                                    else -> Color(0xFF757575)
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Item info card ────────────────────────────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = currentItem.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )

                            if (!currentItem.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentItem.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Metadata rows
                            MetadataRow(
                                icon = Icons.Default.Info,
                                label = "分类",
                                value = currentItem.category,
                            )

                            if (!currentItem.location.isNullOrBlank()) {
                                MetadataRow(
                                    icon = Icons.Default.LocationOn,
                                    label = if (currentItem.type == "found") "捡到地点" else "丢失地点",
                                    value = currentItem.location,
                                )
                            }

                            if (!currentItem.lostDate.isNullOrBlank()) {
                                MetadataRow(
                                    icon = Icons.Default.Info,
                                    label = "日期",
                                    value = currentItem.lostDate,
                                )
                            }

                            if (currentItem.reward > 0) {
                                MetadataRow(
                                    icon = Icons.Default.MonetizationOn,
                                    label = "悬赏",
                                    value = "¥${currentItem.reward}",
                                )
                            }

                            MetadataRow(
                                icon = Icons.Default.Person,
                                label = "联系方式",
                                value = currentItem.contact,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Claims section ────────────────────────────
                    if (claims.isNotEmpty()) {
                        Text(
                            text = "认领申请 (${claims.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        claims.forEach { claim ->
                            ClaimCard(
                                claim = claim,
                                isPublisher = isPublisher,
                                onApprove = { viewModel.approveClaim(claim.id) },
                                onReject = { viewModel.rejectClaim(claim.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Action buttons ────────────────────────────
                    if (!isClosed) {
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isPublisher) {
                            // Publisher actions
                            if (isClaimed) {
                                Button(
                                    onClick = { viewModel.resolveItem() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                ) {
                                    Text("确认已归还/已找到")
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.closeItem() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text("关闭此启事")
                            }
                        } else {
                            // Non-publisher: can claim
                            Button(
                                onClick = {
                                    navController.navigate(LostClaim(lostId = currentItem.id))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            ) {
                                Text("我要认领")
                            }
                        }
                    } else {
                        // Closed — show resolved state
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "该启事已关闭",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ClaimCard(
    claim: LostFoundClaimDto,
    isPublisher: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "认领人",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(
                    status = when (claim.status) {
                        LostFoundClaimEntity.STATUS_PENDING -> "待审核"
                        LostFoundClaimEntity.STATUS_APPROVED -> "已批准"
                        LostFoundClaimEntity.STATUS_REJECTED -> "已拒绝"
                        else -> claim.status
                    },
                    color = when (claim.status) {
                        LostFoundClaimEntity.STATUS_PENDING -> Color(0xFFFF9800)
                        LostFoundClaimEntity.STATUS_APPROVED -> Color(0xFF4CAF50)
                        LostFoundClaimEntity.STATUS_REJECTED -> Color(0xFFF44336)
                        else -> Color(0xFF757575)
                    },
                )
            }

            if (!claim.proofDescription.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = claim.proofDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Publisher can approve/reject pending claims
            if (isPublisher && claim.status == LostFoundClaimEntity.STATUS_PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onApprove) {
                        Text("批准")
                    }
                    TextButton(
                        onClick = onReject,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("拒绝")
                    }
                }
            }
        }
    }
}
