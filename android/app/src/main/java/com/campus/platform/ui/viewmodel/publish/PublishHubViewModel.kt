package com.campus.platform.ui.viewmodel.publish

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 发布中心 ViewModel。
 *
 * 当前职责：提供入口导航所需的基础数据，确保 publish-hub 路由下的导航逻辑正确。
 * 后续扩展时在此添加发布入口的统计/权限/草稿等逻辑。
 */
@HiltViewModel
class PublishHubViewModel @Inject constructor() : ViewModel()
