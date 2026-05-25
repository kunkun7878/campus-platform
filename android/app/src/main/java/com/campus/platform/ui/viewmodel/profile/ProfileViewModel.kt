package com.campus.platform.ui.viewmodel.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.UserPreferencesDataStore
import com.campus.platform.data.model.Profile
import com.campus.platform.data.school.SchoolRepository
import com.campus.platform.domain.repository.ICommunityRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ProfileVM"

/**
 * 个人中心 ViewModel。
 *
 * 管理当前用户 Profile 加载、Agent 状态、待审核计数，
 * 以及退出登录等操作。
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val communityRepo: ICommunityRepository,
    private val prefs: UserPreferencesDataStore,
    private val schoolRepository: SchoolRepository,
) : ViewModel() {

    // ── Profile 状态 ────────────────────────────────────────

    private val _profileState = MutableStateFlow<UiState<Profile>>(UiState.Loading)
    val profileState: StateFlow<UiState<Profile>> = _profileState.asStateFlow()

    // ── isAgent（从 profile 派生） ───────────────────────────

    val isAgent: StateFlow<Boolean> = _profileState
        .map { state -> (state is UiState.Success) && state.data.isAgent }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ── 学校名称 ─────────────────────────────────────────────

    private val _schoolName = MutableStateFlow<String?>(null)
    val schoolName: StateFlow<String?> = _schoolName.asStateFlow()

    // ── Agent 待审核数 ──────────────────────────────────────

    private val _pendingReviewCount = MutableStateFlow(0)
    val pendingReviewCount: StateFlow<Int> = _pendingReviewCount.asStateFlow()

    // ── 错误消息 ─────────────────────────────────────────────

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── 退出登录导航事件 ────────────────────────────────────

    private val _navOnSignOut = MutableStateFlow(false)
    val navOnSignOut: StateFlow<Boolean> = _navOnSignOut.asStateFlow()

    init {
        loadProfile()
    }

    // ── 加载用户信息 ─────────────────────────────────────────

    private fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            try {
                val profile = authRepository.getProfile()
                if (profile != null) {
                    _profileState.value = UiState.Success(profile)
                    val schoolId = profile.schoolId ?: ""
                    if (schoolId.isNotBlank()) {
                        loadSchoolName(schoolId)
                    }
                    if (profile.isAgent && schoolId.isNotBlank()) {
                        loadPendingReviewCount(schoolId)
                    }
                } else {
                    _profileState.value = UiState.Error("无法加载用户信息，请重新登录")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载用户信息失败", e)
                _profileState.value = UiState.Error("加载失败：${e.message}")
            }
        }
    }

    private fun loadSchoolName(schoolId: String) {
        viewModelScope.launch {
            try {
                val schools = schoolRepository.getSchools()
                _schoolName.value = schools.find { it.id == schoolId }?.name
            } catch (e: Exception) {
                Log.e(TAG, "加载学校名称失败", e)
            }
        }
    }

    private fun loadPendingReviewCount(schoolId: String) {
        viewModelScope.launch {
            try {
                communityRepo.getPostsBySchool(schoolId).collect { posts ->
                    _pendingReviewCount.value = posts.count { it.status == "pending_review" }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载待审核数失败", e)
            }
        }
    }

    // ── 操作 ─────────────────────────────────────────────────

    /** 退出登录，清除 session 并触发导航到 Login */
    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                prefs.clearAll()
                _navOnSignOut.value = true
            } catch (e: Exception) {
                Log.e(TAG, "退出登录失败", e)
                _error.value = "退出失败：${e.message}"
            }
        }
    }

    /** 消费退出登录导航事件 */
    fun onSignOutConsumed() {
        _navOnSignOut.value = false
    }

    /** 清除错误状态 */
    fun clearError() {
        _error.value = null
    }
}
