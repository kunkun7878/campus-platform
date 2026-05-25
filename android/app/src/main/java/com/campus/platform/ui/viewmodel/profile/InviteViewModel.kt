package com.campus.platform.ui.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.local.mapper.InviteCodeDto
import com.campus.platform.data.local.mapper.InviteRecordDto
import com.campus.platform.domain.repository.IInviteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InviteViewModel @Inject constructor(
    private val inviteRepository: IInviteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteUiState())
    val uiState: StateFlow<InviteUiState> = _uiState.asStateFlow()

    fun loadInviteData(userId: String) {
        viewModelScope.launch {
            try {
                inviteRepository.getInviteCode(userId).collect { code ->
                    _uiState.value = _uiState.value.copy(
                        inviteCode = code,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }

        viewModelScope.launch {
            try {
                inviteRepository.getInviteRecords(userId).collect { records ->
                    _uiState.value = _uiState.value.copy(inviteRecords = records)
                }
            } catch (e: Exception) {
                // records are supplementary
            }
        }

        viewModelScope.launch {
            try {
                inviteRepository.getInviteCount(userId).collect { count ->
                    _uiState.value = _uiState.value.copy(inviteCount = count)
                }
            } catch (e: Exception) {
                // count is supplementary
            }
        }
    }

    fun generateInviteCode(userId: String) {
        viewModelScope.launch {
            try {
                inviteRepository.generateInviteCode(userId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class InviteUiState(
    val inviteCode: InviteCodeDto? = null,
    val inviteRecords: List<InviteRecordDto> = emptyList(),
    val inviteCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)
