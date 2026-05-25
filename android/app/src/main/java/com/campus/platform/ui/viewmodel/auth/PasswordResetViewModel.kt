package com.campus.platform.ui.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.auth.AuthValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PasswordResetVM"

data class PasswordResetFormState(
    val phone: String = "",
    val otpCode: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val countdown: Int = 0,
    val otpSent: Boolean = false,
    val otpVerified: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _formState = MutableStateFlow(PasswordResetFormState())
    val formState: StateFlow<PasswordResetFormState> = _formState.asStateFlow()

    private var countdownJob: Job? = null

    // ── Step 1: Phone + OTP ─────────────────────────────────────

    fun onPhoneChange(value: String) {
        _formState.update { it.copy(phone = value.take(11).filter { c -> c.isDigit() }) }
    }

    fun onOtpCodeChange(value: String) {
        _formState.update { it.copy(otpCode = value.take(6).filter { c -> c.isDigit() }) }
    }

    fun sendOtp() {
        val phone = _formState.value.phone
        if (phone.length != 11) {
            _formState.update { it.copy(error = "请输入有效的11位手机号") }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.signInWithOtp(phone)
                _formState.update { it.copy(otpSent = true, isLoading = false) }
                startCountdown()
            } catch (e: Exception) {
                Log.e(TAG, "发送验证码失败", e)
                _formState.update { it.copy(isLoading = false, error = "发送验证码失败，请稍后重试") }
            }
        }
    }

    fun verifyOtp() {
        val state = _formState.value
        if (state.otpCode.length != 6) return
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.verifyOtp(state.phone, state.otpCode)
                _formState.update { it.copy(otpVerified = true, isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "验证码验证失败", e)
                _formState.update { it.copy(isLoading = false, error = "验证码错误，请重新输入") }
            }
        }
    }

    // ── Step 2: New password ────────────────────────────────────

    fun onNewPasswordChange(value: String) {
        _formState.update { it.copy(newPassword = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _formState.update { it.copy(confirmPassword = value, error = null) }
    }

    fun submitNewPassword() {
        val state = _formState.value
        // Validate
        AuthValidator.validatePassword(state.newPassword)?.let { errorMsg ->
            _formState.update { it.copy(error = errorMsg) }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _formState.update { it.copy(error = "两次输入的密码不一致") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.updatePassword(state.newPassword)
                _formState.update { it.copy(isSuccess = true, isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "更新密码失败", e)
                _formState.update { it.copy(isLoading = false, error = "密码重置失败，请稍后重试") }
            }
        }
    }

    fun onErrorDismissed() {
        _formState.update { it.copy(error = null) }
    }

    // ── Countdown ───────────────────────────────────────────────

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _formState.update { it.copy(countdown = 60) }
            while (_formState.value.countdown > 0) {
                delay(1000L)
                _formState.update { it.copy(countdown = it.countdown - 1) }
            }
        }
    }
}
