package com.campus.platform.ui.viewmodel.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.FavoriteEntry
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.domain.repository.IFavoriteRepository
import com.campus.platform.domain.repository.ILostFoundRepository
import com.campus.platform.domain.repository.IMarketRepository
import com.campus.platform.domain.repository.IRunnerTaskRepository
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
 * filter index: 0=全部, 1=跑腿, 2=二手, 3=失物, 4=帖子
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyFavoritesViewModel @Inject constructor(
    private val favoriteRepo: IFavoriteRepository,
    private val marketRepo: IMarketRepository,
    private val runnerTaskRepo: IRunnerTaskRepository,
    private val lostFoundRepo: ILostFoundRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    companion object {
        val FILTER_LABELS = listOf("全部", "跑腿", "二手", "失物", "帖子")
    }

    // ── FilterChip ──────────────────────────────────────────────

    private val _activeFilterIndex = MutableStateFlow(0)
    val activeFilterIndex: StateFlow<Int> = _activeFilterIndex.asStateFlow()

    // ── UI 状态 ─────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState<List<FavoriteEntry>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<FavoriteEntry>>> = _uiState.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    // ── Init ────────────────────────────────────────────────────

    init {
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
                    return@collectLatest
                }

                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _uiState.value = UiState.Success(emptyList())
                    return@collectLatest
                }

                _uiState.value = UiState.Loading
                try {
                    when (filterIndex) {
                        0 -> {
                            // All: query market + runner + lost
                            val profile = authRepository.getProfile()
                            val schoolId = profile?.schoolId ?: ""
                            val marketFavs = loadFavoritesByType(userId, "market_listing")
                            val runnerFavs = if (schoolId.isNotEmpty()) loadFavoritesByType(userId, "runner_task") else emptyList()
                            val lostFavs = loadFavoritesByType(userId, "lost_found")
                            _uiState.value = UiState.Success(marketFavs + runnerFavs + lostFavs)
                        }
                        1 -> {
                            // Runner favorites
                            val profile = authRepository.getProfile()
                            val schoolId = profile?.schoolId ?: ""
                            if (schoolId.isNotEmpty()) {
                                _uiState.value = UiState.Success(loadFavoritesByType(userId, "runner_task"))
                            } else {
                                _uiState.value = UiState.Success(emptyList())
                            }
                        }
                        2 -> {
                            _uiState.value = UiState.Success(loadFavoritesByType(userId, "market_listing"))
                        }
                        3 -> {
                            _uiState.value = UiState.Success(loadFavoritesByType(userId, "lost_found"))
                        }
                        4 -> {
                            // Post favorites — no post repository connected yet, return empty for now
                            _uiState.value = UiState.Success(emptyList())
                        }
                        else -> _uiState.value = UiState.Success(emptyList())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "加载我的收藏失败", e)
                    _uiState.value = UiState.Error("加载失败，请稍后重试")
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

    fun removeFavorite(targetId: String) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId() ?: return@launch
            try {
                // Determine targetType from current entries
                val current = _uiState.value
                if (current is UiState.Success) {
                    val entry = current.data.find { it.id == targetId }
                    val targetType = when (entry) {
                        is FavoriteEntry.Market -> "market_listing"
                        is FavoriteEntry.Runner -> "runner_task"
                        is FavoriteEntry.LostFound -> "lost_found"
                        else -> null
                    }
                    if (targetType != null) {
                        favoriteRepo.removeFavoriteByTarget(userId, targetType, targetId)
                        _uiState.value = UiState.Success(current.data.filter { it.id != targetId })
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "取消收藏失败 targetId=$targetId", e)
            }
        }
    }

    // ── Private helpers ─────────────────────────────────────────

    private suspend fun loadFavoritesByType(userId: String, targetType: String): List<FavoriteEntry> {
        val favorites = mutableListOf<FavoriteEntry>()
        val targetIds = mutableListOf<String>()

        favoriteRepo.getFavoritesByUserIdAndTypeFlow(userId, targetType).collect { favs ->
            targetIds.clear()
            targetIds.addAll(favs.map { it.targetId })
        }

        if (targetIds.isEmpty()) return emptyList()

        when (targetType) {
            "market_listing" -> {
                marketRepo.getListingsByIdsFlow(targetIds).collect { listings ->
                    favorites.clear()
                    favorites.addAll(listings.map { FavoriteEntry.Market(it) })
                }
            }
            "runner_task" -> {
                val profile = authRepository.getProfile()
                val schoolId = profile?.schoolId ?: ""
                if (schoolId.isNotEmpty()) {
                    runnerTaskRepo.getTasksBySchool(schoolId).collect { tasks ->
                        favorites.clear()
                        favorites.addAll(
                            tasks.filter { it.id in targetIds }.map { FavoriteEntry.Runner(it) }
                        )
                    }
                }
            }
            "lost_found" -> {
                val profile = authRepository.getProfile()
                val schoolId = profile?.schoolId ?: ""
                if (schoolId.isNotEmpty()) {
                    lostFoundRepo.getItemsBySchool(schoolId).collect { items ->
                        favorites.clear()
                        favorites.addAll(
                            items.filter { it.id in targetIds }.map { FavoriteEntry.LostFound(it) }
                        )
                    }
                }
            }
        }
        return favorites
    }
}
