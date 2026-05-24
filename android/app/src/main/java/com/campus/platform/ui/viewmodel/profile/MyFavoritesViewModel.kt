package com.campus.platform.ui.viewmodel.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.domain.repository.IFavoriteRepository
import com.campus.platform.domain.repository.IMarketRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MyFavoritesVM"

/**
 * 「我的收藏」跨类型收藏列表 ViewModel。
 *
 * Phase 5 简化策略：只实现二手 tab（targetType="market_listing"）。
 * 跑腿/失物/帖子 tab 显示"开发中"占位，数据源在后续 Phase 补充。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyFavoritesViewModel @Inject constructor(
    private val favoriteRepo: IFavoriteRepository,
    private val marketRepo: IMarketRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    companion object {
        /** FilterChip 中文标签，按索引对应 */
        val FILTER_LABELS = listOf("全部", "跑腿", "二手", "失物", "帖子")

        /** 开发中 tab 索引：跑腿(1)、失物(3)、帖子(4) */
        private val DEVELOPING_INDICES = setOf(1, 3, 4)

        /** Phase 5 仅实现的收藏类型 */
        private const val TARGET_TYPE = "market_listing"
    }

    // ── FilterChip ──────────────────────────────────────────────

    private val _activeFilterIndex = MutableStateFlow(0)
    val activeFilterIndex: StateFlow<Int> = _activeFilterIndex.asStateFlow()

    private val _isUnderDevelopment = MutableStateFlow(false)
    val isUnderDevelopment: StateFlow<Boolean> = _isUnderDevelopment.asStateFlow()

    // ── UI 状态 ─────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState<List<MarketListingDto>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<MarketListingDto>>> = _uiState.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    // ── Init ────────────────────────────────────────────────────

    init {
        // 首次加载时从 Supabase 同步收藏数据（Room 缓存可能为空）
        viewModelScope.launch {
            authRepository.isLoggedInFlow.collectLatest { isLoggedIn ->
                if (isLoggedIn) {
                    val userId = authRepository.currentUserId()
                    if (userId != null) {
                        try {
                            favoriteRepo.refreshFavorites(userId)
                        } catch (e: Exception) {
                            Log.e(TAG, "初始同步收藏失败", e)
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                authRepository.isLoggedInFlow,
                _activeFilterIndex,
                _refreshTrigger,
            ) { isLoggedIn, filterIndex, _ ->
                Pair(isLoggedIn, filterIndex)
            }.collectLatest { (isLoggedIn, filterIndex) ->
                if (!isLoggedIn) {
                    _uiState.value = UiState.Success(emptyList())
                    _isUnderDevelopment.value = false
                    return@collectLatest
                }

                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _uiState.value = UiState.Success(emptyList())
                    _isUnderDevelopment.value = false
                    return@collectLatest
                }

                _isUnderDevelopment.value = filterIndex in DEVELOPING_INDICES

                if (filterIndex in DEVELOPING_INDICES) {
                    _uiState.value = UiState.Success(emptyList())
                    return@collectLatest
                }

                _uiState.value = UiState.Loading
                try {
                    // 1. 获取当前用户指定类型的收藏 ID 列表
                    // 2. 批量查询对应的商品详情
                    // 3. flatMapLatest 保证收藏列表变化时自动重新查询商品
                    favoriteRepo
                        .getFavoritesByUserIdAndTypeFlow(userId, TARGET_TYPE)
                        .flatMapLatest { favorites ->
                            val listingIds = favorites.map { it.targetId }
                            if (listingIds.isEmpty()) {
                                flowOf(emptyList<MarketListingDto>())
                            } else {
                                marketRepo.getListingsByIdsFlow(listingIds)
                            }
                        }
                        .collect { listings ->
                            _uiState.value = UiState.Success(listings)
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "加载我的收藏失败", e)
                    _uiState.value = UiState.Error("加载失败，请稍后重试")
                }
            }
        }
    }

    // ── Actions ─────────────────────────────────────────────────

    /** 切换 FilterChip */
    fun selectFilter(index: Int) {
        if (index in FILTER_LABELS.indices) {
            _activeFilterIndex.value = index
        }
    }

    /** 下拉刷新：先从 Supabase 同步最新收藏数据，再触发本地 Flow 重新查询 */
    fun refresh() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId()
            if (userId != null) {
                try {
                    favoriteRepo.refreshFavorites(userId)
                } catch (e: Exception) {
                    Log.e(TAG, "刷新收藏失败", e)
                }
            }
        }
        _refreshTrigger.value += 1
    }

    /**
     * 取消收藏指定商品。
     *
     * 先调用网络层删除，确认后乐观更新本地 UI 状态。
     * DAO 层的 Flow 也会因数据库变更自动重新触发查询，
     * 双重保障保证 UI 及时更新。
     */
    fun removeFavorite(targetId: String) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId() ?: return@launch
            try {
                favoriteRepo.removeFavoriteByTarget(userId, TARGET_TYPE, targetId)
                // 乐观更新：立即从当前列表中移除该项
                val current = _uiState.value
                if (current is UiState.Success) {
                    _uiState.value = UiState.Success(current.data.filter { it.id != targetId })
                }
            } catch (e: Exception) {
                Log.e(TAG, "取消收藏失败 targetId=$targetId", e)
                // DAO Flow 会保持旧状态，无需显式回滚
            }
        }
    }
}
