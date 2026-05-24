package com.campus.platform.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.model.Profile
import com.campus.platform.navigation.AppStartDestination
import com.campus.platform.navigation.determineStartDestination
import com.campus.platform.push.CampusMessagingService
import com.campus.platform.push.FcmTokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Splash/认证守护 ViewModel。
 *
 * 负责 App 启动时的认证状态检查以及 post-auth 路由决策，
 * 将 splash 和 post-auth 的路由逻辑从 CampusNavGraph 提取到此 ViewModel，
 * 使导航图不再直接依赖 AuthRepository。
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val fcmTokenManager: FcmTokenManager,
) : ViewModel() {

    sealed interface SplashUiState {
        /** 正在检查认证状态 */
        data object Loading : SplashUiState

        /** 已确定目标路由 */
        data class Destination(
            val target: AppStartDestination,
            val profile: Profile?,
        ) : SplashUiState

        /** 检查失败 */
        data class Error(val message: String) : SplashUiState
    }

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkAuth()
    }

    /** 初始启动：检查 session + profile，确定首页目标路由 */
    fun checkAuth() {
        viewModelScope.launch {
            _uiState.value = SplashUiState.Loading
            try {
                val session = authRepository.getSession()
                val isAuth = session != null
                var profile: Profile? = null
                if (isAuth) {
                    profile = authRepository.getProfile()
                }
                val destination = determineStartDestination(isAuth, profile)
                // Save pending FCM token now that a user is authenticated
                if (isAuth && profile != null) {
                    savePendingFcmToken(profile.id)
                }
                _uiState.value = SplashUiState.Destination(destination, profile)
            } catch (e: Exception) {
                Log.e("SplashViewModel", "启动失败", e)
                _uiState.value = SplashUiState.Error("启动失败，请检查网络连接")
            }
        }
    }

    /**
     * Post-auth 路由判定：登录/注册成功后，检查 profile 是否已选校，
     * 决定进入首页还是选校页。
     */
    fun determinePostAuthDestination() {
        viewModelScope.launch {
            _uiState.value = SplashUiState.Loading
            try {
                val profile = authRepository.getProfile()
                val target = if (profile != null && profile.schoolId != null && profile.campusId != null) {
                    AppStartDestination.Home
                } else {
                    AppStartDestination.SchoolSelect
                }
                if (profile != null) {
                    savePendingFcmToken(profile.id)
                }
                _uiState.value = SplashUiState.Destination(target, profile)
            } catch (e: Exception) {
                Log.e("SplashViewModel", "加载用户信息失败", e)
                _uiState.value = SplashUiState.Error("加载用户信息失败，请稍后重试")
            }
        }
    }

    /**
     * If the FCM service cached a token before the user logged in,
     * save it now with the real userId.
     */
    private fun savePendingFcmToken(userId: String) {
        val token = CampusMessagingService.pendingToken ?: return
        viewModelScope.launch {
            fcmTokenManager.saveToken(userId, token)
        }
    }
}
