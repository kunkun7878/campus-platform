package com.campus.platform.ui.screen.market

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.campus.platform.navigation.GoodsDetail
import com.campus.platform.ui.component.MarketOrderStatusColorAccepted
import com.campus.platform.ui.component.MarketOrderStatusColorCompleted
import com.campus.platform.ui.component.MarketUiMapper
import com.campus.platform.ui.component.marketOrderStatusColor
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.market.MarketOrderActions
import com.campus.platform.ui.viewmodel.market.MarketOrderDetailViewModel
import com.campus.platform.ui.viewmodel.market.OrderDetailData

private fun statusLabel(status: String): String = when (status) {
    "pending" -> "等待卖家确认"
    "accepted" -> "待面交"
    "completed" -> "交易完成"
    "cancelled" -> "已取消"
    else -> status
}

private fun statusIcon(status: String): ImageVector = when (status) {
    "pending" -> Icons.Filled.HourglassEmpty
    "accepted" -> Icons.Filled.Handshake
    "completed" -> Icons.Filled.CheckCircle
    "cancelled" -> Icons.Filled.Cancel
    else -> Icons.Filled.HourglassEmpty
}

private fun formatPrice(price: Int): String = "¥$price"

private fun formatTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        timestamp.replace("T", " ").take(19)
    } catch (_: Exception) {
        timestamp.take(19)
    }
}

private fun shortId(id: String): String {
    return if (id.length > 8) id.take(8) + "..." else id
}

// ── Screen ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketOrderDetailScreen(
    viewModel: MarketOrderDetailViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions by viewModel.actions.collectAsStateWithLifecycle()
    val actionInProgress by viewModel.actionInProgress.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    val orderId = navController.currentBackStackEntry
        ?.arguments
        ?.getString("orderId")
        .orEmpty()

    // Load on entry
    LaunchedEffect(orderId) {
        if (orderId.isNotEmpty()) {
            viewModel.loadOrderDetail(orderId)
        }
    }

    // Cancel dialog state
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelLabel by remember { mutableStateOf("取消订单") }
    // Track which action triggered the cancel (for the label)
    var isRejectAction by remember { mutableStateOf(false) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionError) {
        actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    val hasActions = with(actions) { canAccept || canCancel || canConfirmComplete }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("订单详情") },
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
        bottomBar = {
            if (hasActions && uiState is UiState.Success && !actionInProgress) {
                val data = (uiState as UiState.Success).data
                MarketOrderBottomBar(
                    orderStatus = data.order.status,
                    isBuyer = data.isBuyer,
                    isSeller = data.isSeller,
                    onAccept = { viewModel.acceptOrder() },
                    onConfirmComplete = { viewModel.confirmComplete() },
                    onCancelClick = { label, reject ->
                        cancelLabel = label
                        isRejectAction = reject
                        showCancelDialog = true
                    },
                    onContactSeller = {
                        // Placeholder: messaging feature not yet implemented
                    },
                )
            }
        },
    ) { paddingValues ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadOrderDetail(orderId) }) {
                            Text("重试")
                        }
                    }
                }
            }

            is UiState.Success -> {
                val data = state.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ── Status banner ──
                    StatusBanner(status = data.order.status)

                    // ── Product info card ──
                    ProductInfoCard(
                        listing = data.listing,
                        onClick = {
                            navController.navigate(GoodsDetail(goodsId = data.listing.id))
                        },
                    )

                    // ── Both parties card ──
                    PartiesCard(
                        sellerId = data.order.sellerId,
                        buyerId = data.order.buyerId,
                    )

                    // ── Time info ──
                    TimeInfoCard(
                        createdAt = data.order.createdAt,
                        completedAt = data.order.completedAt,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Cancel dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = {
                showCancelDialog = false
                isRejectAction = false
            },
            title = { Text(cancelLabel) },
            text = {
                Text(
                    text = if (isRejectAction)
                        "确认拒绝该订单？拒绝后商品将恢复上架。"
                    else
                        "确认取消该订单？取消后商品将恢复上架。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelOrder(null)
                        showCancelDialog = false
                        isRejectAction = false
                    },
                    enabled = !actionInProgress,
                ) {
                    Text(
                        text = "确认",
                        color = if (actionInProgress)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    isRejectAction = false
                }) {
                    Text("返回")
                }
            },
        )
    }
}

// ── Status Banner ─────────────────────────────────────────────

@Composable
private fun StatusBanner(
    status: String,
    modifier: Modifier = Modifier,
) {
    val color = marketOrderStatusColor(status)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.10f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = statusIcon(status),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusLabel(status),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

// ── Product Info Card ─────────────────────────────────────────

@Composable
private fun ProductInfoCard(
    listing: com.campus.platform.data.local.mapper.MarketListingDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "商品信息",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Title + price
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatPrice(listing.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Condition badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = MarketUiMapper.conditionDisplay(listing.condition),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Meetup location
            if (!listing.meetupLocation.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "面交地点：${listing.meetupLocation}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Parties Card ──────────────────────────────────────────────

@Composable
private fun PartiesCard(
    sellerId: String,
    buyerId: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "双方信息",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Seller
            PartyRow(label = "卖家", userId = sellerId)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Buyer
            PartyRow(label = "买家", userId = buyerId)
        }
    }
}

@Composable
private fun PartyRow(
    label: String,
    userId: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar placeholder
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "用户 ${shortId(userId)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── Time Info Card ────────────────────────────────────────────

@Composable
private fun TimeInfoCard(
    createdAt: String?,
    completedAt: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "时间信息",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "下单时间：${formatTime(createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!completedAt.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MarketOrderStatusColorCompleted,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "完成时间：${formatTime(completedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Bottom Bar ────────────────────────────────────────────────

@Composable
private fun MarketOrderBottomBar(
    orderStatus: String,
    isBuyer: Boolean,
    isSeller: Boolean,
    onAccept: () -> Unit,
    onConfirmComplete: () -> Unit,
    onCancelClick: (label: String, isReject: Boolean) -> Unit,
    onContactSeller: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                // ── pending + seller: "确认接单" + "拒绝" ──
                isSeller && orderStatus == "pending" -> {
                    OutlinedButton(
                        onClick = { onCancelClick("拒绝订单", true) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("拒绝", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MarketOrderStatusColorAccepted,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("确认接单")
                    }
                }

                // ── pending + buyer: "取消订单" ──
                isBuyer && orderStatus == "pending" -> {
                    OutlinedButton(
                        onClick = { onCancelClick("取消订单", false) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("取消订单", color = MaterialTheme.colorScheme.error)
                    }
                }

                // ── accepted + seller: "确认完成" + "取消订单" ──
                isSeller && orderStatus == "accepted" -> {
                    OutlinedButton(
                        onClick = { onCancelClick("取消订单", false) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("取消订单", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onConfirmComplete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MarketOrderStatusColorCompleted,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("确认完成")
                    }
                }

                // ── accepted + buyer: "确认完成" + "联系卖家" ──
                isBuyer && orderStatus == "accepted" -> {
                    OutlinedButton(
                        onClick = onContactSeller,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("联系卖家")
                    }
                    Button(
                        onClick = onConfirmComplete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MarketOrderStatusColorCompleted,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("确认完成")
                    }
                }
            }
        }
    }
}
