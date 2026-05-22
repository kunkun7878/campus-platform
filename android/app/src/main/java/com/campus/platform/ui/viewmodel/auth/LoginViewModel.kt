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

data class LoginFormState(
    val phone: String = "",
    val password: String = "",
    val otpCode: String = "",
    val useOtpLogin: Boolean = true,
    val agreedToTerms: Boolean = false,
    val isLoading: Boolean = false,
    val otpSent: Boolean = false,
    val countdown: Int = 0,
    val showCaptcha: Boolean = false,
    val captchaForPasswordLogin: Boolean = false,
    val error: String? = null,
    val passwordErrorCount: Int = 0,
    val lockoutSeconds: Int = 0,
)

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ── 倒计时 side-effect ──
    private var countdownJob: kotlinx.coroutines.Job? = null

    fun onPhoneChange(value: String) {
        _formState.update { it.copy(phone = value.take(11).filter { c -> c.isDigit() }, error = null) }
    }

    fun onPasswordChange(value: String) {
        _formState.update { it.copy(password = value, error = null) }
    }

    fun onOtpCodeChange(value: String) {
        _formState.update { it.copy(otpCode = value.take(6).filter { c -> c.isDigit() }, error = null) }
    }

    fun onToggleLoginMode(useOtp: Boolean) {
        _formState.update { it.copy(useOtpLogin = useOtp, otpSent = false, error = null) }
    }

    fun onAgreedToTermsChange(checked: Boolean) {
        _formState.update { it.copy(agreedToTerms = checked) }
    }

    fun onCaptchaDismiss() {
        _formState.update { it.copy(showCaptcha = false) }
    }

    fun onErrorDismissed() {
        _formState.update { it.copy(error = null) }
        _uiState.value = LoginUiState.Idle
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
        val state = _formState.value
        _formState.update { it.copy(showCaptcha = false) }

        if (state.captchaForPasswordLogin) {
            _formState.update { it.copy(captchaForPasswordLogin = false) }
            performPasswordLogin()
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            try {
                authRepository.signInWithOtp(state.phone)
                _formState.update { it.copy(otpSent = true, countdown = 60) }
                startCountdown()
            } catch (e: Exception) {
                setError(e.message ?: "发送验证码失败")
            } finally {
                _formState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loginWithOtp() {
        val state = _formState.value
        AuthValidator.validateVerificationCode(state.otpCode)?.let {
            setError(it)
            return
        }
        if (!state.agreedToTerms) {
            setError("请先同意用户协议")
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            try {
                authRepository.verifyOtp(state.phone, state.otpCode)
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                setError(e.message ?: "验证码错误或已过期")
            } finally {
                _formState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loginWithPassword() {
        val state = _formState.value
        AuthValidator.validatePhone(state.phone)?.let {
            setError(it)
            return
        }
        if (state.password.isBlank()) {
            setError("请输入密码")
            return
        }
        if (!state.agreedToTerms) {
            setError("请先同意用户协议")
            return
        }
        _formState.update { it.copy(captchaForPasswordLogin = true, showCaptcha = true) }
    }

    private fun performPasswordLogin() {
        val state = _formState.value
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            try {
                authRepository.signInWithPhoneAndPassword(state.phone, state.password)
                _formState.update { it.copy(passwordErrorCount = 0, lockoutSeconds = 0, error = null) }
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                val newCount = state.passwordErrorCount + 1
                val msg = when {
                    newCount >= 8 -> {
                        _formState.update { it.copy(lockoutSeconds = 60) }
                        "密码错误次数过多，已锁定60秒，请稍后再试"
                    }
                    newCount >= 5 -> {
                        "密码错误次数过多，建议使用验证码登录（${e.message ?: "密码错误"}）"
                    }
                    else -> e.message ?: "登录失败，请检查手机号和密码"
                }
                _formState.update { it.copy(passwordErrorCount = newCount) }
                setError(msg)
            } finally {
                _formState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_formState.value.countdown > 0) {
                kotlinx.coroutines.delay(1000)
                _formState.update { it.copy(countdown = it.countdown - 1) }
            }
        }
    }

    /** lockout 倒计时 */
    fun startLockoutCountdown() {
        viewModelScope.launch {
            while (_formState.value.lockoutSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                _formState.update { it.copy(lockoutSeconds = it.lockoutSeconds - 1) }
            }
        }
    }

    private fun setError(message: String) {
        _formState.update { it.copy(error = message) }
    }
}
