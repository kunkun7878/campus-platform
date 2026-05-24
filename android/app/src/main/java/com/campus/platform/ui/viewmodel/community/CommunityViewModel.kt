package com.campus.platform.ui.viewmodel.community

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.UserPreferencesDataStore
import com.campus.platform.data.local.mapper.CommunityPostDto
import com.campus.platform.data.local.mapper.OfficialGroupDto
import com.campus.platform.domain.repository.ICommunityRepository
import com.campus.platform.domain.repository.IGroupRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CommunityVM"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityRepo: ICommunityRepository,
    private val groupRepo: IGroupRepository,
    private val authRepository: AuthRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    // ── 频道选择 ──────────────────────────────────

    private val _selectedChannelIndex = MutableStateFlow(0)
    val selectedChannelIndex: StateFlow<Int> = _selectedChannelIndex.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 导航事件
    private val _navToPostId = MutableStateFlow<String?>(null)
    val navToPostId: StateFlow<String?> = _navToPostId.asStateFlow()

    private val _navToGroupId = MutableStateFlow<String?>(null)
    val navToGroupId: StateFlow<String?> = _navToGroupId.asStateFlow()

    // ── 帖子流（按学校 + 频道） ──

    val posts: StateFlow<UiState<List<CommunityPostDto>>> = combine(
        prefs.schoolId,
        _selectedChannelIndex,
    ) { schoolId, channelIndex ->
        schoolId to channelIndex
    }.flatMapLatest { (schoolId, channelIndex) ->
        if (schoolId.isNullOrBlank()) {
            flowOf(UiState.Success(emptyList()))
        } else {
            val section = COMMUNITY_CHANNEL_SECTIONS.getOrElse(channelIndex) { "campus_wall" }
            communityRepo.getPostsBySection(schoolId, section)
                .map { posts -> UiState.Success(posts) as UiState<List<CommunityPostDto>> }
        }
    }.catch { e ->
        Log.e(TAG, "加载帖子失败", e)
        _errorMessage.value = "加载失败，请下拉重试"
        emit(UiState.Error("加载失败，请下拉重试"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
    )

    // ── 官方群列表 ──

    val officialGroups: StateFlow<UiState<List<OfficialGroupDto>>> = prefs.schoolId
        .flatMapLatest { schoolId ->
            if (schoolId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                groupRepo.getGroupsBySchool(schoolId)
                    .map { groups -> UiState.Success(groups) as UiState<List<OfficialGroupDto>> }
            }
        }.catch { e ->
            Log.e(TAG, "加载群列表失败", e)
            emit(UiState.Success(emptyList()))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    // ── 当前登录用户 ID ──

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        viewModelScope.launch {
            _currentUserId.value = authRepository.currentUserId()
        }
    }

    // ── Actions ─────────────────────────────────────

    fun selectChannel(index: Int) {
        _selectedChannelIndex.value = index
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                val schoolId = prefs.schoolId.first() ?: return@launch
                communityRepo.refreshPosts(schoolId)
                groupRepo.refreshGroups(schoolId)
            } catch (e: Exception) {
                Log.e(TAG, "刷新失败", e)
                _errorMessage.value = "刷新失败，请重试"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onPostClick(postId: String) {
        _navToPostId.value = postId
    }

    fun onGroupClick(groupId: String) {
        _navToGroupId.value = groupId
    }

    fun onNavPostConsumed() {
        _navToPostId.value = null
    }

    fun onNavGroupConsumed() {
        _navToGroupId.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        /** 与 ChannelTabBar.COMMUNITY_CHANNELS 及 DB CHECK 约束保持同步 */
        val COMMUNITY_CHANNEL_SECTIONS = listOf(
            "campus_wall",
            "discussion",
            "lost_found",
            "second_hand",
            "help",
            "announcement",
        )
    }
}
