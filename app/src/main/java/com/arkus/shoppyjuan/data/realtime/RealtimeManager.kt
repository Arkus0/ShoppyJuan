package com.arkus.shoppyjuan.data.realtime

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.presenceChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

sealed class RealtimeEvent {
    data class ItemAdded(val itemId: String) : RealtimeEvent()
    data class ItemUpdated(val itemId: String) : RealtimeEvent()
    data class ItemDeleted(val itemId: String) : RealtimeEvent()
    data class ListUpdated(val listId: String) : RealtimeEvent()
    data class UserPresence(val userId: String, val online: Boolean) : RealtimeEvent()
}

@Singleton
class RealtimeManager @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val TAG = "RealtimeManager"

    /**
     * Subscribe to real-time changes on a shopping list
     */
    fun subscribeToList(listId: String): Flow<RealtimeEvent> {
        val channel = supabase.realtime.channel("list:$listId")

        // Subscribe to list_items changes
        val itemChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "list_items"
            filter = "list_id=eq.$listId"
        }.map { action ->
            when (action) {
                is PostgresAction.Insert -> {
                    val record = action.record as? JsonObject
                    val itemId = record?.get("id")?.jsonPrimitive?.content ?: ""
                    RealtimeEvent.ItemAdded(itemId)
                }
                is PostgresAction.Update -> {
                    val record = action.record as? JsonObject
                    val itemId = record?.get("id")?.jsonPrimitive?.content ?: ""
                    RealtimeEvent.ItemUpdated(itemId)
                }
                is PostgresAction.Delete -> {
                    val oldRecord = action.oldRecord as? JsonObject
                    val itemId = oldRecord?.get("id")?.jsonPrimitive?.content ?: ""
                    RealtimeEvent.ItemDeleted(itemId)
                }
                else -> null
            }
        }

        // Subscribe the channel
        CoroutineScope(Dispatchers.IO).launch { channel.subscribe() }

        return itemChanges.map { it ?: RealtimeEvent.ListUpdated(listId) }
    }

    /**
     * Track online presence of collaborators
     */
    fun trackPresence(listId: String, userId: String): Flow<Map<String, Boolean>> {
        val channel = supabase.realtime.channel("presence:$listId")

        // Track own presence and subscribe
        CoroutineScope(Dispatchers.IO).launch {
            channel.track(buildJsonObject {
                put("user_id", userId)
                put("online", true)
            })
            channel.subscribe()
        }

        // Subscribe to presence changes
        val presenceFlow = channel.presenceChangeFlow()

        return presenceFlow.map { presenceChange ->
            val onlineUsers = mutableMapOf<String, Boolean>()

            presenceChange.joins.forEach { (key, presence) ->
                val presenceUserId = presence.presenceRef
                onlineUsers[presenceUserId] = true
            }

            presenceChange.leaves.forEach { (key, presence) ->
                val presenceUserId = presence.presenceRef
                onlineUsers[presenceUserId] = false
            }

            Log.d(TAG, "Presence changed: $onlineUsers")
            onlineUsers
        }
    }

    /**
     * Unsubscribe from a list channel
     */
    suspend fun unsubscribeFromList(listId: String) {
        try {
            val listChannel = supabase.realtime.channel("list:$listId")
            val presenceChannel = supabase.realtime.channel("presence:$listId")
            supabase.realtime.removeChannel(listChannel)
            supabase.realtime.removeChannel(presenceChannel)
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from list", e)
        }
    }

    /**
     * Send a broadcast message to all collaborators
     */
    suspend fun broadcastToList(listId: String, event: String, payload: Map<String, Any>) {
        try {
            val channel = supabase.realtime.channel("list:$listId")
            channel.subscribe()
            // TODO: Implement broadcast when Supabase Kotlin SDK supports it
            Log.d(TAG, "Broadcasting event: $event to list: $listId")
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting to list", e)
        }
    }
}
