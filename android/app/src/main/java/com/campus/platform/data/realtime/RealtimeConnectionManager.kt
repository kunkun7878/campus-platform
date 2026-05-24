package com.campus.platform.data.realtime

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.PostgresChangeFilter
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeConnectionManager @Inject constructor(
    private val supabase: SupabaseClient,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _incomingMessages = MutableSharedFlow<RealtimeMessagePayload>(
        replay = 0,
        extraBufferCapacity = 256,
    )
    val incomingMessages: Flow<RealtimeMessagePayload> = _incomingMessages.asSharedFlow()

    private val _incomingGroupMessages = MutableSharedFlow<GroupMessagePayload>(
        replay = 0,
        extraBufferCapacity = 256,
    )
    val incomingGroupMessages: Flow<GroupMessagePayload> = _incomingGroupMessages.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val activeChannels = mutableMapOf<String, RealtimeChannel>()

    fun connect() {
        scope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                supabase.realtime.connect()
                _connectionState.value = ConnectionState.CONNECTED
                Log.d(TAG, "Realtime connected")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Realtime connect failed", e)
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                activeChannels.values.forEach { ch ->
                    try { supabase.realtime.removeChannel(ch) } catch (_: Exception) {}
                }
                activeChannels.clear()
                supabase.realtime.disconnect()
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.d(TAG, "Realtime disconnected")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Realtime disconnect error", e)
            }
        }
    }

    fun subscribeToConversation(conversationId: String) {
        val subKey = "msg-$conversationId"
        if (activeChannels.containsKey(subKey)) return

        scope.launch {
            try {
                if (_connectionState.value != ConnectionState.CONNECTED) {
                    supabase.realtime.connect()
                    _connectionState.value = ConnectionState.CONNECTED
                }

                val channel = supabase.realtime.channel(subKey)
                activeChannels[subKey] = channel

                val flow = channel.postgresChangeFlow<PostgresAction>(
                    schema = "public",
                    filter = {
                        table = "messages"
                        // Note: column-level filter on PostgresChangeFilter is private in SDK 3.1.2.
                        // Client-side filtering by conversation_id is done in the flow collector below.
                    }
                )
                channel.subscribe()

                Log.d(TAG, "subscribeToConversation($conversationId) — subscribed")

                flow.collect { action ->
                    if (action is PostgresAction.Insert) {
                        try {
                            val record = action.record as? JsonObject ?: return@collect
                            val payload = Json.decodeFromJsonElement<RealtimeMessagePayload>(record)
                            if (payload.conversationId == conversationId) {
                                _incomingMessages.emit(payload)
                            }
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (e: Exception) {
                            Log.e(TAG, "msg CDC parse error", e)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "subscribeToConversation($conversationId) failed", e)
                activeChannels.remove(subKey)
            }
        }
    }

    fun unsubscribeFromConversation(conversationId: String) {
        val subKey = "msg-$conversationId"
        scope.launch {
            try {
                activeChannels[subKey]?.let { supabase.realtime.removeChannel(it) }
            } catch (_: Exception) { }
            activeChannels.remove(subKey)
        }
    }

    fun subscribeToGroupMessages(groupId: String) {
        val subKey = "grp-$groupId"
        if (activeChannels.containsKey(subKey)) return

        scope.launch {
            try {
                if (_connectionState.value != ConnectionState.CONNECTED) {
                    supabase.realtime.connect()
                    _connectionState.value = ConnectionState.CONNECTED
                }

                val channel = supabase.realtime.channel(subKey)
                activeChannels[subKey] = channel

                val flow = channel.postgresChangeFlow<PostgresAction>(
                    schema = "public",
                    filter = {
                        table = "group_messages"
                        // Note: column-level filter on PostgresChangeFilter is private in SDK 3.1.2.
                        // Client-side filtering by group_id is done in the flow collector below.
                    }
                )
                channel.subscribe()

                Log.d(TAG, "subscribeToGroupMessages($groupId) — subscribed")

                flow.collect { action ->
                    if (action is PostgresAction.Insert) {
                        try {
                            val record = action.record as? JsonObject ?: return@collect
                            val payload = Json.decodeFromJsonElement<GroupMessagePayload>(record)
                            if (payload.groupId == groupId) {
                                _incomingGroupMessages.emit(payload)
                            }
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (e: Exception) {
                            Log.e(TAG, "group msg CDC parse error", e)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "subscribeToGroupMessages($groupId) failed", e)
                activeChannels.remove(subKey)
            }
        }
    }

    fun unsubscribeFromGroupMessages(groupId: String) {
        val subKey = "grp-$groupId"
        scope.launch {
            try {
                activeChannels[subKey]?.let { supabase.realtime.removeChannel(it) }
            } catch (_: Exception) { }
            activeChannels.remove(subKey)
        }
    }

    // ── Types ──────────────────────────────────────────────

    @Serializable
    data class RealtimeMessagePayload(
        val id: String,
        @SerialName("conversation_id") val conversationId: String = "",
        @SerialName("sender_id") val senderId: String = "",
        val content: String = "",
        @SerialName("is_read") val isRead: Boolean = false,
        @SerialName("created_at") val createdAt: String? = null,
    )

    @Serializable
    data class GroupMessagePayload(
        val id: String,
        @SerialName("group_id") val groupId: String = "",
        @SerialName("sender_id") val senderId: String = "",
        val content: String = "",
        @SerialName("message_type") val messageType: String = "user",
        @SerialName("created_at") val createdAt: String? = null,
    )

    enum class ConnectionState {
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
    }

    companion object {
        private const val TAG = "RealtimeManager"
    }
}
