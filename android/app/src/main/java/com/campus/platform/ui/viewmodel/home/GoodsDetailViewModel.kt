package com.campus.platform.ui.viewmodel.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.entity.MarketListingEntity
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.data.local.mapper.UserFavoriteDto
import com.campus.platform.data.model.Profile
import com.campus.platform.domain.repository.IFavoriteRepository
import com.campus.platform.domain.repository.IMarketRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "GoodsDetailVM"

@HiltViewModel
class GoodsDetailViewModel @Inject constructor(
    private val marketRepository: IMarketRepository,
    private val favoriteRepository: IFavoriteRepository,
    private val authRepository: AuthRepository,
    private val supabase: SupabaseClient,
) : ViewModel() {

    // ── 核心状态 ──────────────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState<MarketListingDto>>(UiState.Loading)
    val uiState: StateFlow<UiState<MarketListingDto>> = _uiState.asStateFlow()

    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited.asStateFlow()

    private val _isOwnListing = MutableStateFlow(false)
    val isOwnListing: StateFlow<Boolean> = _isOwnListing.asStateFlow()

    private val _sellerProfile = MutableStateFlow<Profile?>(null)
    val sellerProfile: StateFlow<Profile?> = _sellerProfile.asStateFlow()

    // ── 内部追踪 ──────────────────────────────────────────────

    private var currentListingId: String? = null

    // ── 编辑状态 ──────────────────────────────────────────────

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editTitle = MutableStateFlow("")
    val editTitle: StateFlow<String> = _editTitle.asStateFlow()

    private val _editDescription = MutableStateFlow("")
    val editDescription: StateFlow<String> = _editDescription.asStateFlow()

    private val _editPrice = MutableStateFlow("")
    val editPrice: StateFlow<String> = _editPrice.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    // ── 编辑操作 ──────────────────────────────────────────────

    fun startEditing() {
        val listing = (_uiState.value as? UiState.Success)?.data ?: return
        _editTitle.value = listing.title
        _editDescription.value = listing.description ?: ""
        _editPrice.value = listing.price.toString()
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
    }

    fun onEditTitleChange(value: String) {
        _editTitle.value = value
    }

    fun onEditDescriptionChange(value: String) {
        _editDescription.value = value
    }

    fun onEditPriceChange(value: String) {
        _editPrice.value = value.filter { it.isDigit() }
    }

    fun saveEdits() {
        val listingId = currentListingId ?: return
        val title = _editTitle.value.trim()
        val description = _editDescription.value.trim()
        val priceStr = _editPrice.value

        if (title.isBlank()) {
            _saveMessage.value = "标题不能为空"
            return
        }
        val price = priceStr.toIntOrNull()
        if (price == null || price <= 0) {
            _saveMessage.value = "请输入有效的价格"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val updates = mutableMapOf<String, Any?>(
                    "title" to title,
                    "price" to price,
                )
                if (description.isNotBlank()) {
                    updates["description"] = description
                }
                marketRepository.updateListing(listingId, updates)
                _isEditing.value = false
                _saveMessage.value = "保存成功"
                // Refresh listing
                loadListing(listingId)
            } catch (e: Exception) {
                Log.e(TAG, "保存编辑失败", e)
                _saveMessage.value = "保存失败，请稍后重试"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }

    // ── 数据加载 ──────────────────────────────────────────────

    /**
     * 加载商品详情：先从 Room 缓存读取，再从 Supabase 刷新。
     *
     * 购买按钮可见性由 UI 层根据 [uiState] 中的 status 与 [isOwnListing] 联合判断：
     *   listing.status == "active" && !isOwnListing
     */
    fun loadListing(listingId: String) {
        currentListingId = listingId
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val userId = authRepository.currentUserId()

                // 1. Room 缓存即时展示
                val cached = marketRepository.getListingById(listingId)
                if (cached != null) {
                    _uiState.value = UiState.Success(cached)
                    _isOwnListing.value = cached.sellerId == userId
                    if (userId != null) {
                        _isFavorited.value = favoriteRepository.isFavorited(
                            userId, "market_listing", listingId
                        )
                    }
                }

                // 2. 从 Supabase 同步刷新
                val schoolId = cached?.schoolId ?: authRepository.getProfile()?.schoolId
                if (schoolId != null) {
                    marketRepository.refreshListings(schoolId)
                    val refreshed = marketRepository.getListingById(listingId)
                    if (refreshed != null) {
                        _uiState.value = UiState.Success(refreshed)
                        _isOwnListing.value = refreshed.sellerId == userId
                        if (userId != null) {
                            _isFavorited.value = favoriteRepository.isFavorited(
                                userId, "market_listing", listingId
                            )
                        }
                    }
                }

                // 3. 缓存和远程均无数据
                if (_uiState.value is UiState.Loading) {
                    _uiState.value = UiState.Error("商品不存在或已下架")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载商品详情失败", e)
                _uiState.value = UiState.Error("加载失败，请稍后重试")
            }
        }
    }

    // ── 收藏操作 ──────────────────────────────────────────────

    /** 切换当前商品的收藏状态 */
    fun toggleFavorite() {
        val listingId = currentListingId ?: return
        viewModelScope.launch {
            val userId = authRepository.currentUserId() ?: return@launch
            try {
                if (_isFavorited.value) {
                    favoriteRepository.removeFavoriteByTarget(userId, "market_listing", listingId)
                    _isFavorited.value = false
                } else {
                    val favorite = UserFavoriteDto(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        targetType = "market_listing",
                        targetId = listingId,
                        createdAt = java.time.Instant.now().toString(),
                    )
                    favoriteRepository.addFavorite(favorite)
                    _isFavorited.value = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "切换收藏失败", e)
            }
        }
    }

    // ── 购买操作 ──────────────────────────────────────────────

    /** 调用 Edge Function market-purchase 发起购买请求。异常原样抛出，由 UI 层处理。 */
    suspend fun purchaseListing(listingId: String) {
        val body = buildJsonObject {
            put("listing_id", listingId)
        }
        supabase.functions.invoke("market-purchase", body = body)
    }

    // ── 下架操作 ──────────────────────────────────────────────

    /**
     * 将商品状态从 active 更新为 cancelled（下架）。
     * 使用乐观锁：仅当 listing 状态仍为 active 时才允许下架，
     * 避免覆盖已被购买（reserved）的商品状态。
     *
     * @return true 表示下架成功，false 表示商品状态已变更（可能已被购买）。
     * @throws Exception 其他网络/数据库错误。
     */
    suspend fun delistListing(listingId: String): Boolean {
        return marketRepository.updateListingStatus(
            id = listingId,
            expectedStatus = MarketListingEntity.STATUS_ACTIVE,
            newStatus = MarketListingEntity.STATUS_CANCELLED,
        )
    }

    // ── 卖家信息 ──────────────────────────────────────────────

    /** 加载卖家 Profile 用于展示头像和昵称。失败时保持 null，UI 用 sellerId 兜底。 */
    fun loadSellerProfile(sellerId: String) {
        viewModelScope.launch {
            try {
                val profile = supabase.postgrest
                    .from("profiles")
                    .select { filter { eq("id", sellerId) } }
                    .decodeSingleOrNull<Profile>()
                _sellerProfile.value = profile
            } catch (_: Exception) {
                // 加载失败则用 sellerId 兜底
            }
        }
    }
}
