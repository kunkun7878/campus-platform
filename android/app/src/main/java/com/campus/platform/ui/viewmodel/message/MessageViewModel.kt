package com.campus.platform.ui.viewmodel.message

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.ConversationDto
import com.campus.platform.data.local.mapper.NotificationDto
import com.campus.platform.data.model.Profile
import com.campus.platform.domain.repository.IMessageRepository
import com.campus.platform.domain.repository.INotificationRepository
import com.campus.platform.domain.repository.IUserRepository
import com.campus.platform.push.NotificationDeepLinkHandler
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MessageVM"

/** 会话列表单项（含对方用户信息+未读数） */
data class ConversationItem(
    val conversation: ConversationDto,
    val otherUser: Profile?,
    val unreadCount: Int,
)

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepo: IMessageRepository,
    private val notificationRepo: INotificationRepository,
    private val userRepo: IUserRepository,
    private val authRepository: AuthRepository,
    private val deepLinkHandler: NotificationDeepLinkHandler,
) : ViewModel() {

    // ── Tab ──────────────────────────────────────────────────

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // ── 私信列表 ────────────────────────────────────────────

    private val _conversationsState = MutableStateFlow<UiState<List<ConversationItem>>>(UiState.Loading)
    val conversationsState: StateFlow<UiState<List<ConversationItem>>> = _conversationsState.asStateFlow()

    // ── 通知列表 ────────────────────────────────────────────

    private val _notificationsState = MutableStateFlow<UiState<List<NotificationDto>>>(UiState.Loading)
    val notificationsState: StateFlow<UiState<List<NotificationDto>>> = _notificationsState.asStateFlow()

    // ── 通知未读数 ──────────────────────────────────────────

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    // ── 通知点击导航 ──────────────────────────────────────────

    private val _navOnNotification = MutableStateFlow<String?>(null)
    val navOnNotification: StateFlow<String?> = _navOnNotification.asStateFlow()

    // ── 刷新状态 ────────────────────────────────────────────

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ── 当前用户 ────────────────────────────────────────────

    private var _currentUserId: String? = null
    private val unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    init {
        viewModelScope.launch {
            _currentUserId = authRepository.currentUserId()
            loadConversations()
            loadNotifications()
        }
    }

    // ── Tab 切换 ────────────────────────────────────────────

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    // ── 加载私信列表 ────────────────────────────────────────

    private fun loadConversations() {
        val uid = _currentUserId ?: run {
            _conversationsState.value = UiState.Success(emptyList())
            return
        }

        viewModelScope.launch {
            messageRepo.getConversations()
                .catch { e ->
                    Log.e(TAG, "加载私信列表失败", e)
                    _conversationsState.value = UiState.Error("加载失败，请下拉重试")
                    emit(emptyList())
                }
                .collectLatest { conversations ->
                    val items = buildConversationItems(conversations, uid)
                    _conversationsState.value = UiState.Success(items)
                }
        }

        // 后台计算未读数，更新到状态中
        viewModelScope.launch {
            messageRepo.getConversations()
                .catch { emit(emptyList()) }
                .collectLatest { conversations ->
                    val newCounts = mutableMapOf<String, Int>()
                    for (conv in conversations) {
                        try {
                            newCounts[conv.id] = countUnread(conv.id, uid)
                        } catch (_: Exception) {
                            newCounts[conv.id] = 0
                        }
                    }
                    unreadCounts.value = newCounts
                    // 刷新已缓存的列表
                    val current = _conversationsState.value
                    if (current is UiState.Success) {
                        val refreshed = buildConversationItems(conversations, uid, newCounts)
                        _conversationsState.value = UiState.Success(refreshed)
                    }
                }
        }
    }

    private suspend fun buildConversationItems(
        conversations: List<ConversationDto>,
        uid: String,
        counts: Map<String, Int> = emptyMap(),
    ): List<ConversationItem> {
        return conversations.map { conv ->
            val otherId = if (conv.user1Id == uid) conv.user2Id else conv.user1Id
            val profile = try {
                userRepo.getProfile(otherId).first()
            } catch (_: Exception) {
                null
            }
            ConversationItem(
                conversation = conv,
                otherUser = profile,
                unreadCount = counts[conv.id] ?: 0,
            )
        }
    }

    // ── 加载通知列表 ────────────────────────────────────────

    private fun loadNotifications() {
        val uid = _currentUserId ?: run {
            _notificationsState.value = UiState.Success(emptyList())
            return
        }

        viewModelScope.launch {
            notificationRepo.getNotifications(uid)
                .catch { e ->
                    Log.e(TAG, "加载通知失败", e)
                    _notificationsState.value = UiState.Error("加载失败，请下拉重试")
                    emit(emptyList())
                }
                .collectLatest { notifications ->
                    _notificationsState.value = UiState.Success(notifications)
                }
        }

        viewModelScope.launch {
            notificationRepo.getUnreadCount(uid)
                .catch { emit(0) }
                .collectLatest { count ->
                    _unreadNotificationCount.value = count
                }
        }
    }

    // ── 刷新 ────────────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val uid = _currentUserId ?: return@launch
                if (_selectedTab.value == 0) {
                    messageRepo.refreshConversations()
                } else {
                    notificationRepo.refreshNotifications(uid)
                }
            } catch (e: Exception) {
                Log.e(TAG, "刷新失败", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ── 通知操作 ────────────────────────────────────────────

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            try {
                notificationRepo.markAsRead(id)
            } catch (e: Exception) {
                Log.e(TAG, "标记已读失败", e)
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                val uid = _currentUserId ?: return@launch
                notificationRepo.markAllAsRead(uid)
            } catch (e: Exception) {
                Log.e(TAG, "全部已读失败", e)
            }
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            try {
                notificationRepo.deleteNotification(id)
            } catch (e: Exception) {
                Log.e(TAG, "删除通知失败", e)
            }
        }
    }

    /**
     * 点击通知卡片时调用。根据通知的 refType 和 refId 解析导航目标路由，
     * 通过 [navOnNotification] 发射路由字符串供 MessageScreen 消费。
     */
    fun onNotificationClick(notification: NotificationDto) {
        // Auto mark as read
        if (!notification.isRead) {
            markNotificationAsRead(notification.id)
        }

        val data = mapOf(
            "type" to notification.type,
            "ref_type" to (notification.refType ?: ""),
            "ref_id" to (notification.refId ?: ""),
            "notification_id" to notification.id,
        )
        val target = deepLinkHandler.resolve(data)
        _navOnNotification.value = target.route
    }

    /** MessageScreen 消费通知导航事件后调用。 */
    fun onNavNotificationConsumed() {
        _navOnNotification.value = null
    }

    // ── Helper ──────────────────────────────────────────────

    private suspend fun countUnread(conversationId: String, currentUserId: String): Int {
        return try {
            messageRepo.getMessages(conversationId).first()
                .count { !it.isRead && it.senderId != currentUserId }
        } catch (e: Exception) {
            0
        }
    }
}
