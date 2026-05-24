package com.campus.platform.ui.viewmodel.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.UserPreferencesDataStore
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.data.local.mapper.MarketOrderDto
import com.campus.platform.domain.repository.IMarketOrderRepository
import com.campus.platform.domain.repository.IMarketRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MySoldVM"

// ── Filter constants ───────────────────────────────────────────

object MySoldFilter {
    const val ALL = "all"
    const val PENDING_MEETUP = "pending_meetup"   // pending + accepted
    const val COMPLETED = "completed"
}

// ── ViewModel ──────────────────────────────────────────────────

@HiltViewModel
class MySoldViewModel @Inject constructor(
    private val orderRepo: IMarketOrderRepository,
    private val listingRepo: IMarketRepository,
    private val authRepository: AuthRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    // ── Order list state ──────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState<List<MarketOrderDto>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<MarketOrderDto>>> = _uiState.asStateFlow()

    // ── Filter state ──────────────────────────────────────────

    private val _activeFilter = MutableStateFlow(MySoldFilter.ALL)
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    // ── Listing map for title lookup ──────────────────────────

    private val _listingMap = MutableStateFlow<Map<String, MarketListingDto>>(emptyMap())
    val listingMap: StateFlow<Map<String, MarketListingDto>> = _listingMap.asStateFlow()

    // ── Internal cache ────────────────────────────────────────

    private val _allOrders = MutableStateFlow<List<MarketOrderDto>>(emptyList())

    // ── Init ──────────────────────────────────────────────────

    init {
        loadSoldOrders()
    }

    // ── Data loading ──────────────────────────────────────────

    /** 加载当前用户卖出的订单: repository.getOrdersBySeller(currentUserId) */
    fun loadSoldOrders() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val currentUserId = authRepository.currentUserId()
            if (currentUserId == null) {
                _uiState.value = UiState.Error("请先登录")
                return@launch
            }

            try {
                // 先从 Supabase 拉取最新订单（参照 HomeViewModel.refresh 模式）
                val schoolId = prefs.schoolId.first()
                if (schoolId != null) {
                    orderRepo.refreshOrders(schoolId)
                }
                orderRepo.getOrdersBySeller(currentUserId).collectLatest { orders ->
                    _allOrders.value = orders

                    // Load listings for title lookup
                    val listingIds = orders.map { it.listingId }.toSet()
                    if (listingIds.isNotEmpty()) {
                        val map = mutableMapOf<String, MarketListingDto>()
                        for (id in listingIds) {
                            listingRepo.getListingById(id)?.let { map[id] = it }
                        }
                        _listingMap.value = map
                    }

                    // Apply filter
                    applyFilter(orders, _activeFilter.value)
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载已售订单失败", e)
                _uiState.value = UiState.Error("加载失败，请稍后重试")
            }
        }
    }

    // ── Filter ────────────────────────────────────────────────

    fun setFilter(filter: String) {
        _activeFilter.value = filter
        applyFilter(_allOrders.value, filter)
    }

    /** 客户端筛选，参照 OrderListViewModel 的 applyFilter 模式 */
    private fun applyFilter(orders: List<MarketOrderDto>, filter: String) {
        val filtered = when (filter) {
            MySoldFilter.PENDING_MEETUP -> orders.filter {
                it.status == "pending" || it.status == "accepted"
            }
            MySoldFilter.COMPLETED -> orders.filter {
                it.status == "completed"
            }
            else -> orders  // ALL: no filter
        }

        _uiState.value = UiState.Success(filtered)
    }
}
