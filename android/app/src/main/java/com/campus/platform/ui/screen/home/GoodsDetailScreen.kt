package com.campus.platform.ui.screen.home

import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import com.campus.platform.data.local.entity.MarketListingEntity
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.data.model.Profile
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.navigation.GoodsDetail
import com.campus.platform.ui.component.DetailBanner
import com.campus.platform.ui.component.MarketUiMapper
import com.campus.platform.ui.component.StatusBadge
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.home.GoodsDetailViewModel
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.launch

private const val TAG = "GoodsDetailScreen"

// ── Color helpers ──────────────────────────────────────────────

private val SoldOutColor = Color(0xFF7C89A6)
private val BargainTagColor = Color(0xFFFF9A62)

// ── Screen ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodsDetailScreen(
    viewModel: GoodsDetailViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorited by viewModel.isFavorited.collectAsStateWithLifecycle()
    val isOwnListing by viewModel.isOwnListing.collectAsStateWithLifecycle()
    val sellerProfile by viewModel.sellerProfile.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    val editTitle by viewModel.editTitle.collectAsStateWithLifecycle()
    val editDescription by viewModel.editDescription.collectAsStateWithLifecycle()
    val editPrice by viewModel.editPrice.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveMessage by viewModel.saveMessage.collectAsStateWithLifecycle()

    val goodsId = navController.currentBackStackEntry?.toRoute<GoodsDetail>()?.goodsId ?: ""

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var purchaseLoading by remember { mutableStateOf(false) }
    var deliverLoading by remember { mutableStateOf(false) }

    // ── 加载数据 ───────────────────────────────────────────────
    LaunchedEffect(goodsId) {
        if (goodsId.isNotEmpty()) {
            viewModel.loadListing(goodsId)
        }
    }

    // 当 listing 加载完成后加载卖家 Profile
    val listingSellerId = (uiState as? UiState.Success)?.data?.sellerId
    LaunchedEffect(listingSellerId) {
        if (listingSellerId != null) {
            viewModel.loadSellerProfile(listingSellerId)
        }
    }

    // Save message Snackbar
    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("商品详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(CampusRoutes.Message.route)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "消息",
                        )
                    }
                },
            )
        },
        bottomBar = {
            // 仅在 Success 状态渲染底部栏
            val listing = (uiState as? UiState.Success)?.data
            if (listing != null) {
                val isSold = listing.status == "sold"
                val isCancelled = listing.status == MarketListingEntity.STATUS_CANCELLED
                val isReserved = listing.status == "reserved"
                GoodsDetailBottomBar(
                    listing = listing,
                    isOwnListing = isOwnListing,
                    isFavorited = isFavorited,
                    isSoldOrDelisted = isSold || isCancelled || isReserved,
                    purchaseLoading = purchaseLoading,
                    deliverLoading = deliverLoading,
                    onFavoriteClick = { viewModel.toggleFavorite() },
                    onContactClick = {
                        navController.navigate(
                            CampusRoutes.ChatDetail.createRoute(listing.sellerId)
                        )
                    },
                    onPurchaseClick = {
                        scope.launch {
                            purchaseLoading = true
                            try {
                                viewModel.purchaseListing(listing.id)
                                snackbarHostState.showSnackbar("购买请求已提交，请等待卖家确认")
                            } catch (e: Exception) {
                                Log.e(TAG, "购买失败", e)
                                val statusCode = when (e) {
                                    is ClientRequestException -> e.response.status.value
                                    is ServerResponseException -> e.response.status.value
                                    else -> null
                                }
                                when (statusCode) {
                                    403 -> snackbarHostState.showSnackbar("不能购买自己的商品")
                                    422 -> {
                                        snackbarHostState.showSnackbar("商品不可购买或已被预订")
                                        viewModel.loadListing(goodsId)
                                    }
                                    else -> snackbarHostState.showSnackbar("购买失败，请稍后重试")
                                }
                            } finally {
                                purchaseLoading = false
                            }
                        }
                    },
                    onEditClick = {
                        viewModel.startEditing()
                    },
                    onDelistClick = {
                        scope.launch {
                            deliverLoading = true
                            try {
                                val success = viewModel.delistListing(listing.id)
                                viewModel.loadListing(goodsId)
                                snackbarHostState.showSnackbar(
                                    if (success) "商品已下架"
                                    else "商品状态已变更，无法下架"
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "下架失败", e)
                                snackbarHostState.showSnackbar("下架失败，请稍后重试")
                            } finally {
                                deliverLoading = false
                            }
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadListing(goodsId) }) {
                            Text("重试")
                        }
                    }
                }

                is UiState.Success -> {
                    GoodsDetailContent(
                        listing = state.data,
                        isOwnListing = isOwnListing,
                        sellerProfile = sellerProfile,
                    )
                }
            }
        }
    }

    // ── Edit Bottom Sheet ──────────────────────────────────────
    if (isEditing) {
        EditListingSheet(
            title = editTitle,
            description = editDescription,
            price = editPrice,
            isSaving = isSaving,
            onTitleChange = { viewModel.onEditTitleChange(it) },
            onDescriptionChange = { viewModel.onEditDescriptionChange(it) },
            onPriceChange = { viewModel.onEditPriceChange(it) },
            onSave = { viewModel.saveEdits() },
            onCancel = { viewModel.cancelEditing() },
        )
    }
}

// ── Edit Sheet ─────────────────────────────────────────────────

@Composable
private fun GoodsDetailContent(
    listing: MarketListingDto,
    isOwnListing: Boolean,
    sellerProfile: Profile?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // 1. DetailBanner — 图片 + 成色标签
        val firstImage = listing.images
            .trim()
            .takeIf { it.isNotEmpty() && it != "[]" }
            ?.let { raw ->
                // images 字段为逗号分隔的 URL 列表
                raw.trim('[', ']').split(",").firstOrNull()?.trim()?.trim('"')
            }
        DetailBanner(
            imageUrl = firstImage,
            tag = MarketUiMapper.conditionDisplay(listing.condition),
            modifier = Modifier.fillMaxWidth(),
        )

        // 2. 信息区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 标题
            Text(
                text = listing.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // 描述（如有）
            if (!listing.description.isNullOrBlank()) {
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 价格行：当前价 + 原价（划线）+ 议价标签
            PriceRow(
                price = listing.price,
                originalPrice = listing.originalPrice,
                isBargain = listing.isBargain,
            )

            // 商品元信息：成色 StatusBadge + 分类标签
            MetaInfoRow(
                condition = MarketUiMapper.conditionDisplay(listing.condition),
                category = listing.category,
            )

            // 卖家信息行
            SellerInfoRow(
                sellerId = listing.sellerId,
                sellerProfile = sellerProfile,
            )

            // 面交地点
            if (!listing.meetupLocation.isNullOrBlank()) {
                MeetupLocationRow(location = listing.meetupLocation)
            }

            // 发布时间
            if (!listing.createdAt.isNullOrBlank()) {
                PublishTimeRow(createdAt = listing.createdAt)
            }

            // 底部留白，防贴底
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Price Row ──────────────────────────────────────────────────

@Composable
private fun PriceRow(
    price: Int,
    originalPrice: Int?,
    isBargain: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 当前价格
        Text(
            text = "¥$price",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        // 原价（划线）
        if (originalPrice != null && originalPrice > price) {
            Text(
                text = "¥$originalPrice",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textDecoration = TextDecoration.LineThrough,
            )
        }

        // 议价标签
        if (isBargain) {
            StatusBadge(
                status = "可议价",
                color = BargainTagColor,
            )
        }
    }
}

// ── Meta Info Row ──────────────────────────────────────────────

@Composable
private fun MetaInfoRow(
    condition: String,
    category: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusBadge(
            status = condition,
            color = MaterialTheme.colorScheme.primary,
        )

        StatusBadge(
            status = category,
            color = BargainTagColor,
        )
    }
}

// ── Seller Info Row ────────────────────────────────────────────

@Composable
private fun SellerInfoRow(
    sellerId: String,
    sellerProfile: Profile?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 小头像 32dp
        val avatarUrl = sellerProfile?.avatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "卖家头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (sellerProfile?.nickname ?: sellerId).firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = sellerProfile?.nickname ?: sellerId.take(8) + "...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Meetup Location Row ────────────────────────────────────────

@Composable
private fun MeetupLocationRow(
    location: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = location,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Publish Time Row ───────────────────────────────────────────

@Composable
private fun PublishTimeRow(
    createdAt: String,
    modifier: Modifier = Modifier,
) {
    val formattedTime = remember(createdAt) {
        try {
            createdAt.replace("T", " ").take(19)
        } catch (_: Exception) {
            createdAt.take(19)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.AccessTime,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "发布时间：$formattedTime",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

// ── Bottom Bar ─────────────────────────────────────────────────

@Composable
private fun GoodsDetailBottomBar(
    listing: MarketListingDto,
    isOwnListing: Boolean,
    isFavorited: Boolean,
    isSoldOrDelisted: Boolean,
    purchaseLoading: Boolean,
    deliverLoading: Boolean,
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onEditClick: () -> Unit,
    onDelistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSoldOrDelisted) {
        // 已售出/已下架：灰色提示文本
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when (listing.status) {
                    "sold" -> "该商品已售出"
                    "reserved" -> "该商品已被预订"
                    else -> "该商品已下架"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SoldOutColor,
                fontWeight = FontWeight.Medium,
            )
        }
    } else if (isOwnListing) {
        // 自己的商品：编辑 + 下架
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("编辑")
            }

            Button(
                onClick = onDelistClick,
                modifier = Modifier.weight(1f),
                enabled = !deliverLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                if (deliverLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.RemoveCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("下架")
            }
        }
    } else {
        // 他人的商品：收藏 | 联系卖家 | 立即购买
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 收藏
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorited) "取消收藏" else "收藏",
                    tint = if (isFavorited) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            // 联系卖家
            OutlinedButton(
                onClick = onContactClick,
                modifier = Modifier.weight(1f),
            ) {
                Text("联系卖家")
            }

            // 立即购买
            Button(
                onClick = onPurchaseClick,
                modifier = Modifier.weight(1f),
                enabled = !purchaseLoading,
            ) {
                if (purchaseLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (purchaseLoading) "处理中..." else "立即购买")
            }
        }
    }
}

// ── Edit Bottom Sheet ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditListingSheet(
    title: String,
    description: String,
    price: String,
    isSaving: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onCancel,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "编辑商品",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("描述") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = price,
                onValueChange = onPriceChange,
                label = { Text("价格（元）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("取消")
                }
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (isSaving) "保存中…" else "保存")
                }
            }
        }
    }
}
