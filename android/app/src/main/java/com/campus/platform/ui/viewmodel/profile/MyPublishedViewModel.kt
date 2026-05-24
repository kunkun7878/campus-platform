package com.campus.platform.ui.viewmodel.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.domain.repository.IMarketOrderRepository
import com.campus.platform.domain.repository.IMarketRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MyPublishedVM"

/**
 * 「我的发布」跨类型列表 ViewModel。
 *
 * Phase 5 简化策略：先实现二手 tab，跑腿/失物 tab 显示"开发中"占位。
 * 跑腿/失物数据源在 Phase 6 补充。
 */
@HiltViewModel
class MyPublishedViewModel @Inject constructor(
    private val marketRepo: IMarketRepository,
    private val orderRepo: IMarketOrderRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    companion object {
        /** FilterChip 中文标签，按索引对应 */
        val FILTER_LABELS = listOf("全部", "跑腿", "二手", "失物")

        /** 开发中 tab 索引：跑腿(1)、失物(3) */
        private val DEVELOPING_INDICES = setOf(1, 3)
    }

    // ── FilterChip ──────────────────────────────────────────────

    private val _activeFilterIndex = MutableStateFlow(0)
    val activeFilterIndex: StateFlow<Int> = _activeFilterIndex.asStateFlow()

    private val _isUnderDevelopment = MutableStateFlow(false)
    val isUnderDevelopment: StateFlow<Boolean> = _isUnderDevelopment.asStateFlow()

    // ── UI 状态 ─────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState<List<MarketListingDto>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<MarketListingDto>>> = _uiState.asStateFlow()

    // ── listingId → orderId 映射（已售出商品关联订单） ────────────

    private val _listingOrderMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val listingOrderMap: StateFlow<Map<String, String>> = _listingOrderMap.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    // ── Init ────────────────────────────────────────────────────

    init {
        // ── 主数据加载 ──
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
                    _listingOrderMap.value = emptyMap()
                    _isUnderDevelopment.value = false
                    return@collectLatest
                }

                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _uiState.value = UiState.Success(emptyList())
                    _listingOrderMap.value = emptyMap()
                    _isUnderDevelopment.value = false
                    return@collectLatest
                }

                _isUnderDevelopment.value = filterIndex in DEVELOPING_INDICES

                if (filterIndex in DEVELOPING_INDICES) {
                    _uiState.value = UiState.Success(emptyList())
                    _listingOrderMap.value = emptyMap()
                    return@collectLatest
                }

                _uiState.value = UiState.Loading
                try {
                    marketRepo.getListingsBySeller(userId).collect { listings ->
                        _uiState.value = UiState.Success(listings)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "加载我的发布失败", e)
                    _uiState.value = UiState.Error("加载失败，请稍后重试")
                    _listingOrderMap.value = emptyMap()
                }
            }
        }

        // ── 订单映射（已售出商品 → 订单 ID） ──
        viewModelScope.launch {
            authRepository.isLoggedInFlow.collectLatest { isLoggedIn ->
                if (!isLoggedIn) {
                    _listingOrderMap.value = emptyMap()
                    return@collectLatest
                }

                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _listingOrderMap.value = emptyMap()
                    return@collectLatest
                }

                try {
                    orderRepo.getOrdersBySeller(userId).collect { orders ->
                        _listingOrderMap.value = orders.associate { it.listingId to it.id }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "加载订单映射失败", e)
                    _listingOrderMap.value = emptyMap()
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

    /** 刷新当前列表 */
    fun refresh() {
        _refreshTrigger.value += 1
    }
}
