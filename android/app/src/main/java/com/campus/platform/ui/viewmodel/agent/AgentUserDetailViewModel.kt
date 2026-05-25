package com.campus.platform.ui.viewmodel.agent

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.model.Profile
import com.campus.platform.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AgentUserDetailVM"

data class AgentUserDetailState(
    val user: Profile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showBanDialog: Boolean = false,
    val showUnbanDialog: Boolean = false,
    val actionInProgress: Boolean = false,
    val actionResult: String? = null,
)

@HiltViewModel
class AgentUserDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: IUserRepository,
) : ViewModel() {

    private val userId: String = savedStateHandle.get<String>("userId") ?: ""

    private val _state = MutableStateFlow(AgentUserDetailState())
    val state: StateFlow<AgentUserDetailState> = _state.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val user = userRepository.getUserById(userId)
                if (user != null) {
                    _state.value = _state.value.copy(user = user, isLoading = false)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "用户不存在")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadUser failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    // ── Ban / Unban ─────────────────────────────────────────────

    fun showBanDialog() {
        _state.value = _state.value.copy(showBanDialog = true)
    }

    fun dismissBanDialog() {
        _state.value = _state.value.copy(showBanDialog = false)
    }

    fun confirmBan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true, showBanDialog = false)
            try {
                userRepository.updateUserStatus(userId, 1)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    actionResult = "用户已封禁",
                    user = _state.value.user?.copy(status = 1),
                )
            } catch (e: Exception) {
                Log.e(TAG, "ban failed", e)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    error = "操作失败: ${e.message}",
                )
            }
        }
    }

    fun showUnbanDialog() {
        _state.value = _state.value.copy(showUnbanDialog = true)
    }

    fun dismissUnbanDialog() {
        _state.value = _state.value.copy(showUnbanDialog = false)
    }

    fun confirmUnban() {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true, showUnbanDialog = false)
            try {
                userRepository.updateUserStatus(userId, 0)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    actionResult = "用户已解封",
                    user = _state.value.user?.copy(status = 0),
                )
            } catch (e: Exception) {
                Log.e(TAG, "unban failed", e)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    error = "操作失败: ${e.message}",
                )
            }
        }
    }

    fun consumeActionResult() {
        _state.value = _state.value.copy(actionResult = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
