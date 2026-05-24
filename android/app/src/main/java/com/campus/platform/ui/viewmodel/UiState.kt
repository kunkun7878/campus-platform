package com.campus.platform.ui.viewmodel

/**
 * 通用 UI 状态封装，适用所有页面的 Loading / Success / Error 三态。
 *
 * @param T 成功状态携带的数据类型
 */
sealed class UiState<out T> {
    /** 加载中 */
    data object Loading : UiState<Nothing>()

    /** 加载成功，携带数据 */
    data class Success<T>(val data: T) : UiState<T>()

    /** 加载失败，携带错误信息 */
    data class Error(val message: String) : UiState<Nothing>()
}
