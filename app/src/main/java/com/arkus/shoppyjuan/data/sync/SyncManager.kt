package com.arkus.shoppyjuan.data.sync

import android.content.Context
import com.arkus.shoppyjuan.data.local.dao.PendingSyncDao
import com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity
import com.arkus.shoppyjuan.data.local.entity.SyncActionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncStatus {
    object Synced : SyncStatus()
    object Syncing : SyncStatus()
    data class Pending(val count: Int) : SyncStatus()
    object Offline : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val pendingSyncDao: PendingSyncDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Synced)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(scope, SharingStarted.Eagerly, true)

    val pendingActionsCount: StateFlow<Int> = pendingSyncDao.getPendingCount()
        .stateIn(scope, SharingStarted.Eagerly, 0)

    init {
        // Watch network and pending actions to update sync status
        scope.launch {
            combine(
                networkMonitor.isOnline,
                pendingSyncDao.getPendingCount()
            ) { online, pendingCount ->
                when {
                    !online -> SyncStatus.Offline
                    pendingCount > 0 -> SyncStatus.Pending(pendingCount)
                    else -> SyncStatus.Synced
                }
            }.collect { status ->
                _syncStatus.value = status
            }
        }

        // Trigger sync when network becomes available
        scope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    val pendingCount = pendingSyncDao.getPendingCountOnce()
                    if (pendingCount > 0) {
                        triggerSync()
                    }
                }
        }
    }

    fun initialize() {
        // Schedule periodic sync
        OfflineSyncWorker.schedulePeriodicSync(context)
    }

    fun triggerSync() {
        OfflineSyncWorker.scheduleImmediateSync(context)
        _syncStatus.value = SyncStatus.Syncing
    }

    // Helper methods to queue sync actions

    suspend fun queueListCreate(listId: String, data: Map<String, Any?>) {
        if (!networkMonitor.isCurrentlyOnline()) {
            pendingSyncDao.insertAction(
                PendingSyncEntity(
                    actionType = SyncActionType.CREATE_LIST,
                    entityId = listId,
                    payloadJson = json.encodeToString(data.filterValues { it != null })
                )
            )
        }
    }

    suspend fun queueListUpdate(listId: String, data: Map<String, Any?>) {
        if (!networkMonitor.isCurrentlyOnline()) {
            // Remove any existing update action for this list
            pendingSyncDao.deleteActionByEntityAndType(listId, SyncActionType.UPDATE_LIST)
            pendingSyncDao.insertAction(
                PendingSyncEntity(
                    actionType = SyncActionType.UPDATE_LIST,
                    entityId = listId,
                    payloadJson = json.encodeToString(data.filterValues { it != null })
                )
            )
        }
    }

    suspend fun queueListDelete(listId: String) {
        if (!networkMonitor.isCurrentlyOnline()) {
            // Remove all pending actions for this list
            pendingSyncDao.deleteActionsForEntity(listId)
            pendingSyncDao.insertAction(
                PendingSyncEntity(
                    actionType = SyncActionType.DELETE_LIST,
                    entityId = listId,
                    payloadJson = "{}"
                )
            )
        }
    }

    suspend fun queueItemCreate(itemId: String, listId: String, data: Map<String, Any?>) {
        if (!networkMonitor.isCurrentlyOnline()) {
            pendingSyncDao.insertAction(
                PendingSyncEntity(
                    actionType = SyncActionType.CREATE_ITEM,
                    entityId = itemId,
                    parentId = listId,
                    payloadJson = json.encodeToString(data.filterValues { it != null })
                )
            )
        }
    }

    suspend fun queueItemUpdate(itemId: String, listId: String, data: Map<String, Any?>) {
        if (!networkMonitor.isCurrentlyOnline()) {
            pendingSyncDao.deleteActionByEntityAndType(itemId, SyncActionType.UPDATE_ITEM)
            pendingSyncDao.insertAction(
                PendingSyncEntity(
                    actionType = SyncActionType.UPDATE_ITEM,
                    entityId = itemId,
                    parentId = listId,
                    payloadJson = json.encodeToString(data.filterValues { it != null })
                )
            )
        }
    }

    suspend fun queueItemDelete(itemId: String) {
        if (!networkMonitor.isCurrentlyOnline()) {
            pendingSyncDao.deleteActionsForEntity(itemId)
            pendingSyncDao.insertAction(
                PendingSyncEntity(
                    actionType = SyncActionType.DELETE_ITEM,
                    entityId = itemId,
                    payloadJson = "{}"
                )
            )
        }
    }

    suspend fun queueItemCheck(itemId: String, checked: Boolean) {
        if (!networkMonitor.isCurrentlyOnline()) {
            pendingSyncDao.deleteActionByEntityAndType(itemId, SyncActionType.CHECK_ITEM)
            pendingSyncDao.insertAction(
                PendingSyncEntity(
                    actionType = SyncActionType.CHECK_ITEM,
                    entityId = itemId,
                    payloadJson = json.encodeToString(mapOf("checked" to checked))
                )
            )
        }
    }

    suspend fun queueNoteCreate(noteId: String, listId: String, data: Map<String, Any?>) {
        if (!networkMonitor.isCurrentlyOnline()) {
            pendingSyncDao.insertAction(
                PendingSyncEntity(
                    actionType = SyncActionType.CREATE_NOTE,
                    entityId = noteId,
                    parentId = listId,
                    payloadJson = json.encodeToString(data.filterValues { it != null })
                )
            )
        }
    }

    suspend fun queueNoteDelete(noteId: String) {
        if (!networkMonitor.isCurrentlyOnline()) {
            pendingSyncDao.deleteActionsForEntity(noteId)
            pendingSyncDao.insertAction(
                PendingSyncEntity(
                    actionType = SyncActionType.DELETE_NOTE,
                    entityId = noteId,
                    payloadJson = "{}"
                )
            )
        }
    }
}
