package com.campus.platform.ui.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountDeleteFormState(
    val currentStep: Int = 1,
    val isLoading: Boolean = false,
    val selectedReason: Int = -1,
    val reasonOther: String = "",
    val error: String? = null,
) {
    companion object {
        val reasonOptions = listOf(
            "不再需要使用",
            "已有其他学校账号",
            "隐私安全考虑",
            "功能不满足需求",
            "其他原因",
        )
    }
}

sealed interface AccountDeleteUiState {
    data object Idle : AccountDeleteUiState
    data object Success : AccountDeleteUiState
    data class Error(val message: String) : AccountDeleteUiState
}

@HiltViewModel
class AccountDeleteViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _formState = MutableStateFlow(AccountDeleteFormState())
    val formState: StateFlow<AccountDeleteFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AccountDeleteUiState>(AccountDeleteUiState.Idle)
    val uiState: StateFlow<AccountDeleteUiState> = _uiState.asStateFlow()

    val reasonOptions = AccountDeleteFormState.reasonOptions

    fun onSelectReason(index: Int) {
        _formState.update { it.copy(selectedReason = index) }
    }

    fun onReasonOtherChange(value: String) {
        _formState.update { it.copy(reasonOther = value) }
    }

    fun onContinueToConfirm() {
        _formState.update { it.copy(currentStep = 2) }
    }

    fun onErrorDismissed() {
        _formState.update { it.copy(error = null) }
        _uiState.value = AccountDeleteUiState.Idle
    }

    fun performDelete() {
        val state = _formState.value
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            try {
                val finalReason = if (state.selectedReason == reasonOptions.size - 1) state.reasonOther
                else reasonOptions.getOrNull(state.selectedReason)
                authRepository.deleteAccount(finalReason)
                _uiState.value = AccountDeleteUiState.Success
            } catch (e: Exception) {
                Log.e("AccountDeleteVM", "注销失败", e)
                _formState.update { it.copy(error = "注销失败，请稍后重试") }
            } finally {
                _formState.update { it.copy(isLoading = false) }
            }
        }
    }
}
