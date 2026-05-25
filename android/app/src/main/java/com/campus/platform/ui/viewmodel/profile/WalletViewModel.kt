package com.campus.platform.ui.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.WalletDto
import com.campus.platform.data.local.mapper.WalletTransactionDto
import com.campus.platform.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    fun loadWallet() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId() ?: return@launch
            try {
                userRepository.getWallet(userId).collect { wallet ->
                    _uiState.value = _uiState.value.copy(
                        wallet = wallet,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false,
                )
            }
        }

        viewModelScope.launch {
            val userId = authRepository.currentUserId() ?: return@launch
            try {
                userRepository.getWalletTransactions(userId).collect { transactions ->
                    _uiState.value = _uiState.value.copy(transactions = transactions)
                }
            } catch (e: Exception) {
                // transactions are optional, don't block the wallet display
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class WalletUiState(
    val wallet: WalletDto? = null,
    val transactions: List<WalletTransactionDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
