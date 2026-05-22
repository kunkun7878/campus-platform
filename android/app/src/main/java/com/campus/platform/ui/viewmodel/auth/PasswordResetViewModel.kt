package com.campus.platform.ui.viewmodel.auth

import androidx.lifecycle.ViewModel
import com.campus.platform.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PasswordResetFormState(
    val phone: String = "",
    val error: String? = null,
)

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _formState = MutableStateFlow(PasswordResetFormState())
    val formState: StateFlow<PasswordResetFormState> = _formState.asStateFlow()

    fun onPhoneChange(value: String) {
        _formState.update { it.copy(phone = value.take(11).filter { c -> c.isDigit() }) }
    }

    fun onErrorDismissed() {
        _formState.update { it.copy(error = null) }
    }
}
