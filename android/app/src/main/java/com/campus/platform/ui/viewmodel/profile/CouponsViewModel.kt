package com.campus.platform.ui.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.local.mapper.CouponDto
import com.campus.platform.data.local.mapper.UserCouponDto
import com.campus.platform.domain.repository.IMiscRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserCouponWithDetails(
    val userCoupon: UserCouponDto,
    val coupon: CouponDto?,
)

@HiltViewModel
class CouponsViewModel @Inject constructor(
    private val miscRepository: IMiscRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CouponsUiState())
    val uiState: StateFlow<CouponsUiState> = _uiState.asStateFlow()

    fun loadCoupons(userId: String, schoolId: String) {
        viewModelScope.launch {
            try {
                combine(
                    miscRepository.getUserCoupons(userId),
                    miscRepository.getActiveCoupons(schoolId),
                ) { userCoupons, activeCoupons ->
                    val couponMap = activeCoupons.associateBy { it.id }

                    val allCoupons = userCoupons.map { uc ->
                        UserCouponWithDetails(
                            userCoupon = uc,
                            coupon = couponMap[uc.couponId],
                        )
                    }

                    _uiState.value = CouponsUiState(
                        unusedCoupons = allCoupons.filter { it.userCoupon.status == "unused" },
                        usedCoupons = allCoupons.filter { it.userCoupon.status == "used" },
                        expiredCoupons = allCoupons.filter { it.userCoupon.status == "expired" },
                        isLoading = false,
                    )
                }.collect { /* state is updated inside combine */ }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun onTabChange(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class CouponsUiState(
    val unusedCoupons: List<UserCouponWithDetails> = emptyList(),
    val usedCoupons: List<UserCouponWithDetails> = emptyList(),
    val expiredCoupons: List<UserCouponWithDetails> = emptyList(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)
