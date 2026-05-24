package com.campus.platform.ui.viewmodel.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.UserPreferencesDataStore
import com.campus.platform.data.local.mapper.LostFoundItemDto
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.data.local.mapper.RunnerTaskDto
import com.campus.platform.data.local.mapper.UserFavoriteDto
import com.campus.platform.domain.repository.IFavoriteRepository
import com.campus.platform.domain.repository.ILostFoundRepository
import com.campus.platform.domain.repository.IMarketRepository
import com.campus.platform.domain.repository.IRunnerOrderRepository
import com.campus.platform.domain.repository.IRunnerTaskRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HomeVM"

/**
 * 首页 ViewModel。
 *
 * 管理跑腿 tab 的任务列表加载、类型筛选、搜索与下拉刷新，
 * 以及二手市场 tab 的商品列表与收藏状态。
 * lost tab 当前为占位，未接入数据。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val runnerTaskRepository: IRunnerTaskRepository,
    private val runnerOrderRepository: IRunnerOrderRepository,
    private val marketRepo: IMarketRepository,
    private val favoriteRepo: IFavoriteRepository,
    private val lostFoundRepository: ILostFoundRepository,
    private val authRepository: AuthRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    // ── 筛选状态 ────────────────────────────────────────

    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    /** 二手市场搜索关键词，客户端文本筛选 */
    private val _marketSearchQuery = MutableStateFlow("")
    val marketSearchQuery: StateFlow<String> = _marketSearchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** 标记是否已收到首次数据 emission，用于区分"加载中"与"列表为空"。 */
    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    /** 错误消息。非 null 时表示最近一次操作失败（Room 查询或刷新）。 */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 导航事件。非 null 时 HomeScreen 应导航到 OrderDetail 并消费该事件。 */
    private val _navToOrderId = MutableStateFlow<String?>(null)
    val navToOrderId: StateFlow<String?> = _navToOrderId.asStateFlow()

    // ── Runner 任务列表（从 Supabase → Room → 本地筛选） ──

    val tasks: StateFlow<List<RunnerTaskDto>> = prefs.schoolId
        .flatMapLatest { schoolId ->
            if (schoolId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                runnerTaskRepository.getTasksBySchool(schoolId)
            }
        }
        .combine(_selectedType) { tasks, type -> tasks.filterByType(type) }
        .combine(_searchKeyword) { tasks, keyword -> tasks.filterByKeyword(keyword) }
        .catch { e ->
            Log.e(TAG, "加载首页任务失败", e)
            _error.value = "加载失败，请重试"
            emit(emptyList())
        }
        .onEach {
            _isInitialLoading.value = false
            _error.value = null
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // ── Market 商品列表 ─────────────────────────────────

    val marketListings: StateFlow<UiState<List<MarketListingDto>>> = prefs.schoolId
        .flatMapLatest { schoolId ->
            if (schoolId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                marketRepo.getListingsBySchool(schoolId)
                    .map { listings ->
                        UiState.Success(listings) as UiState<List<MarketListingDto>>
                    }
                    .catch { e ->
                        Log.e(TAG, "加载市场商品列表失败", e)
                        emit(UiState.Error("加载失败，请稍后重试"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    // ── Market 收藏 ID 集合 ─────────────────────────────

    val marketFavoriteIds: StateFlow<Set<String>> = authRepository.isLoggedInFlow
        .flatMapLatest { isLoggedIn ->
            if (!isLoggedIn) flowOf(emptySet())
            else {
                val userId = authRepository.currentUserId()
                if (userId == null) flowOf(emptySet())
                else favoriteRepo.getFavorites(userId)
                    .map { favorites ->
                        favorites
                            .filter { it.targetType == FAVORITE_TYPE_MARKET }
                            .map { it.targetId }
                            .toSet()
                    }
            }
        }
        .catch { e ->
            Log.e(TAG, "加载收藏状态失败", e)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )

    // ── Lost 筛选状态 ──────────────────────────────────

    private val _lostType = MutableStateFlow<String?>(null)
    val lostType: StateFlow<String?> = _lostType.asStateFlow()

    private val _lostSearchKeyword = MutableStateFlow("")
    val lostSearchKeyword: StateFlow<String> = _lostSearchKeyword.asStateFlow()

    private val _isLostRefreshing = MutableStateFlow(false)
    val isLostRefreshing: StateFlow<Boolean> = _isLostRefreshing.asStateFlow()

    // ── Lost 失物招领列表 ──────────────────────────────

    val lostItems: StateFlow<UiState<List<LostFoundItemDto>>> = prefs.schoolId
        .flatMapLatest { schoolId ->
            if (schoolId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                lostFoundRepository.getItemsBySchool(schoolId)
                    .map { items -> UiState.Success(items) as UiState<List<LostFoundItemDto>> }
                    .catch { e ->
                        Log.e(TAG, "加载失物列表失败", e)
                        emit(UiState.Error("加载失败，请下拉重试"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    // ── Actions ────────────────────────────────────────

    /** 选择跑腿类型筛选。再次点击同一类型即取消筛选（回到"全部"）。 */
    fun selectType(type: String?) {
        _selectedType.value = if (type == _selectedType.value) null else type
    }

    /** 更新搜索关键词，触发本地筛选。 */
    fun onSearchKeywordChange(keyword: String) {
        _searchKeyword.value = keyword
    }

    /** 更新二手市场搜索关键词，触发客户端文本筛选。 */
    fun onMarketSearchQueryChange(query: String) {
        _marketSearchQuery.value = query
    }

    /** 下拉刷新：强制从 Supabase 拉取最新任务并 upsert Room。 */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            try {
                val schoolId = prefs.schoolId.first() ?: return@launch
                runnerTaskRepository.refreshTasks(schoolId)
                marketRepo.refreshListings(schoolId)
            } catch (e: Exception) {
                Log.e(TAG, "刷新失败", e)
                _error.value = "刷新失败，请重试"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * 点击任务卡片时调用。先解析关联 Order（如果存在）的 id，
     * 再触发导航事件。保证传给 OrderDetail 的始终是 orderId 而不是 taskId。
     */
    fun onTaskClick(taskId: String) {
        viewModelScope.launch {
            val order = runnerOrderRepository.getOrdersByTaskId(taskId).firstOrNull()?.firstOrNull()
            val navigateId = order?.id ?: taskId
            _navToOrderId.value = navigateId
        }
    }

    /** HomeScreen 消费导航事件后调用，重置为 null 避免重复导航。 */
    fun onNavEventConsumed() {
        _navToOrderId.value = null
    }

    /**
     * 切换市场商品的收藏状态。
     *
     * 根据当前是否已收藏，调用 [IFavoriteRepository] 的添加或移除接口。
     */
    fun toggleMarketFavorite(listingId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId() ?: return@launch
                val isFavorited = marketFavoriteIds.value.contains(listingId)
                if (isFavorited) {
                    favoriteRepo.removeFavoriteByTarget(userId, FAVORITE_TYPE_MARKET, listingId)
                } else {
                    favoriteRepo.addFavorite(
                        UserFavoriteDto(
                            id = "",
                            userId = userId,
                            targetType = FAVORITE_TYPE_MARKET,
                            targetId = listingId,
                            createdAt = null,
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "切换收藏失败 listingId=$listingId", e)
            }
        }
    }

    // ── Lost actions ───────────────────────────────────

    /** 选择失物类型筛选。再次点击同一类型即取消筛选。 */
    fun selectLostType(type: String?) {
        _lostType.value = if (type == _lostType.value) null else type
    }

    /** 更新失物搜索关键词。 */
    fun onLostSearchKeywordChange(keyword: String) {
        _lostSearchKeyword.value = keyword
    }

    /** 下拉刷新失物列表。 */
    fun refreshLost() {
        viewModelScope.launch {
            _isLostRefreshing.value = true
            try {
                val schoolId = prefs.schoolId.first() ?: return@launch
                lostFoundRepository.refreshItems(schoolId)
            } catch (e: Exception) {
                Log.e(TAG, "刷新失物列表失败", e)
            } finally {
                _isLostRefreshing.value = false
            }
        }
    }

    // ── Helpers ────────────────────────────────────────

    companion object {
        private const val FAVORITE_TYPE_MARKET = "market_listing"

        /** 将任务状态码映射为中文展示标签。 */
        fun statusLabel(status: String): String = when (status) {
            "published" -> "待接单"
            "assigned" -> "已接单"
            "in_progress" -> "配送中"
            "completed" -> "已完成"
            "cancelled" -> "已取消"
            else -> status
        }

        /** 将 ISO 时间字符串格式化为简短展示文本。 */
        fun formatTime(iso: String?): String {
            if (iso == null) return ""
            return try {
                iso.substring(0, minOf(16, iso.length)).replace("T", " ")
            } catch (_: Exception) {
                iso
            }
        }
    }
}

// ── 本地筛选扩展 ──────────────────────────────────────

private fun List<RunnerTaskDto>.filterByType(type: String?): List<RunnerTaskDto> {
    if (type == null) return this
    return filter { it.type == type }
}

private fun List<RunnerTaskDto>.filterByKeyword(keyword: String): List<RunnerTaskDto> {
    if (keyword.isBlank()) return this
    return filter { task ->
        task.title.contains(keyword, ignoreCase = true) ||
        (task.description?.contains(keyword, ignoreCase = true) ?: false)
    }
}
