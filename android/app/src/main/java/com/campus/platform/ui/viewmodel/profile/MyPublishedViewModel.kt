package com.campus.platform.ui.viewmodel.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.data.local.mapper.PublishedEntry
import com.campus.platform.domain.repository.ILostFoundRepository
import com.campus.platform.domain.repository.IMarketOrderRepository
import com.campus.platform.domain.repository.IMarketRepository
import com.campus.platform.domain.repository.IRunnerTaskRepository
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
 * filter index: 0=全部, 1=跑腿, 2=二手, 3=失物
 * 使用 [PublishedEntry] sealed interface 统一三类型展示。
 */
@HiltViewModel
class MyPublishedViewModel @Inject constructor(
    private val marketRepo: IMarketRepository,
    private val runnerTaskRepo: IRunnerTaskRepository,
    private val lostFoundRepo: ILostFoundRepository,
    private val orderRepo: IMarketOrderRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    companion object {
        val FILTER_LABELS = listOf("全部", "跑腿", "二手", "失物")
    }

    // ── FilterChip ──────────────────────────────────────────────

    private val _activeFilterIndex = MutableStateFlow(0)
    val activeFilterIndex: StateFlow<Int> = _activeFilterIndex.asStateFlow()

    // ── UI 状态 ─────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState<List<PublishedEntry>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<PublishedEntry>>> = _uiState.asStateFlow()

    // ── listingId → orderId 映射（已售出商品关联订单） ────────────

    private val _listingOrderMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val listingOrderMap: StateFlow<Map<String, String>> = _listingOrderMap.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    // ── Init ────────────────────────────────────────────────────

    init {
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
                    return@collectLatest
                }

                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _uiState.value = UiState.Success(emptyList())
                    _listingOrderMap.value = emptyMap()
                    return@collectLatest
                }

                _uiState.value = UiState.Loading
                try {
                    val entries = when (filterIndex) {
                        0 -> {
                            // All types
                            val profile = authRepository.getProfile()
                            val schoolId = profile?.schoolId ?: ""
                            val market = queryMarket(userId)
                            val runner = if (schoolId.isNotEmpty()) queryRunner(userId, schoolId) else emptyList()
                            val lost = queryLost(userId)
                            market + runner + lost
                        }
                        1 -> {
                            // Runner
                            val profile = authRepository.getProfile()
                            val schoolId = profile?.schoolId ?: ""
                            if (schoolId.isNotEmpty()) queryRunner(userId, schoolId) else emptyList()
                        }
                        2 -> queryMarket(userId)
                        3 -> queryLost(userId)
                        else -> emptyList()
                    }
                    _uiState.value = UiState.Success(entries)
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

    fun selectFilter(index: Int) {
        if (index in FILTER_LABELS.indices) {
            _activeFilterIndex.value = index
        }
    }

    fun refresh() {
        _refreshTrigger.value += 1
    }

    // ── Private data fetch ──────────────────────────────────────

    private suspend fun queryMarket(userId: String): List<PublishedEntry> {
        val results = mutableListOf<PublishedEntry>()
        marketRepo.getListingsBySeller(userId).collect { listings ->
            results.clear()
            results.addAll(listings.map { PublishedEntry.Market(it) })
        }
        return results
    }

    private suspend fun queryRunner(userId: String, schoolId: String): List<PublishedEntry> {
        val results = mutableListOf<PublishedEntry>()
        runnerTaskRepo.getTasksByPublisher(userId, schoolId).collect { tasks ->
            results.clear()
            results.addAll(tasks.map { PublishedEntry.Runner(it) })
        }
        return results
    }

    private suspend fun queryLost(userId: String): List<PublishedEntry> {
        val results = mutableListOf<PublishedEntry>()
        lostFoundRepo.getItemsByPublisher(userId).collect { items ->
            results.clear()
            results.addAll(items.map { PublishedEntry.LostFound(it) })
        }
        return results
    }
}
