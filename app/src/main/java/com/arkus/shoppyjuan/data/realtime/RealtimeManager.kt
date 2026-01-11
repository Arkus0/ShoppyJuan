package com.arkus.shoppyjuan.data.realtime

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.presenceChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        channel.subscribe()

        return itemChanges.map { it ?: RealtimeEvent.ListUpdated(listId) }
    }

    /**
     * Track online presence of collaborators
     */
    fun trackPresence(listId: String, userId: String): Flow<Map<String, Boolean>> {
        val channel = supabase.realtime.channel("presence:$listId")

        // Track own presence
        channel.track(mapOf("user_id" to userId, "online" to true))

        // Subscribe to presence changes
        val presenceFlow = channel.presenceChangeFlow()

        channel.subscribe()

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
            supabase.realtime.removeChannel("list:$listId")
            supabase.realtime.removeChannel("presence:$listId")
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
