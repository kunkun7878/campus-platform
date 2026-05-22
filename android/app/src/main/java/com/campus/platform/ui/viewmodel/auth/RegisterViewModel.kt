package com.campus.platform.ui.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.auth.AuthValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterFormState(
    val currentStep: Int = 1,
    val totalSteps: Int = 3,
    val phone: String = "",
    val otpCode: String = "",
    val otpSent: Boolean = false,
    val countdown: Int = 0,
    val password: String = "",
    val confirmPassword: String = "",
    val email: String = "",
    val agreedToTerms: Boolean = false,
    val isLoading: Boolean = false,
    val showCaptcha: Boolean = false,
    val error: String? = null,
)

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data object Success : RegisterUiState
    data class Error(val message: String) : RegisterUiState
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _formState = MutableStateFlow(RegisterFormState())
    val formState: StateFlow<RegisterFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onPhoneChange(value: String) {
        _formState.update { it.copy(phone = value.take(11).filter { c -> c.isDigit() }, error = null) }
    }

    fun onOtpCodeChange(value: String) {
        _formState.update { it.copy(otpCode = value.take(6).filter { c -> c.isDigit() }, error = null) }
    }

    fun onPasswordChange(value: String) {
        _formState.update { it.copy(password = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _formState.update { it.copy(confirmPassword = value, error = null) }
    }

    fun onEmailChange(value: String) {
        _formState.update { it.copy(email = value, error = null) }
    }

    fun onAgreedToTermsChange(checked: Boolean) {
        _formState.update { it.copy(agreedToTerms = checked) }
    }

    fun onCaptchaDismiss() {
        _formState.update { it.copy(showCaptcha = false) }
    }

    fun onErrorDismissed() {
        _formState.update { it.copy(error = null) }
        _uiState.value = RegisterUiState.Idle
    }

    fun sendOtp() {
        val state = _formState.value
        AuthValidator.validatePhone(state.phone)?.let {
            setError(it)
            return
        }
        _formState.update { it.copy(showCaptcha = true) }
    }

    fun onCaptchaVerified() {
        _formState.update { it.copy(showCaptcha = false) }
        val state = _formState.value
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            try {
                authRepository.signInWithOtp(state.phone)
                _formState.update { it.copy(otpSent = true, countdown = 60) }
                startCountdown()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val errorMsg = if (msg.contains("already registered", ignoreCase = true) ||
                    msg.contains("already exists", ignoreCase = true) ||
                    msg.contains("User already", ignoreCase = true)
                ) {
                    "该手机号已注册，请直接登录"
                } else {
                    e.message ?: "发送验证码失败"
                }
                setError(errorMsg)
            } finally {
                _formState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun verifyOtpAndNext() {
        val state = _formState.value
        AuthValidator.validateVerificationCode(state.otpCode)?.let {
            setError(it)
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            try {
                authRepository.verifyOtp(state.phone, state.otpCode)
                _formState.update { it.copy(currentStep = 2) }
            } catch (e: Exception) {
                setError(e.message ?: "验证码错误或已过期")
            } finally {
                _formState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setPasswordAndNext() {
        val state = _formState.value
        AuthValidator.validatePassword(state.password)?.let {
            setError(it)
            return
        }
        if (state.password != state.confirmPassword) {
            setError("两次密码不一致")
            return
        }
        _formState.update { it.copy(currentStep = 3) }
    }

    fun finishRegistration() {
        val state = _formState.value
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            try {
                authRepository.signUpWithPhoneAndPassword(state.phone, state.password)
                _uiState.value = RegisterUiState.Success
            } catch (e: Exception) {
                setError(e.message ?: "注册失败，请稍后重试")
            } finally {
                _formState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (_formState.value.countdown > 0) {
                kotlinx.coroutines.delay(1000)
                _formState.update { it.copy(countdown = it.countdown - 1) }
            }
        }
    }

    private fun setError(message: String) {
        _formState.update { it.copy(error = message) }
    }
}
