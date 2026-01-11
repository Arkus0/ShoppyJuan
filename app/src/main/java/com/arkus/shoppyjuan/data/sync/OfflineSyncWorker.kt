package com.arkus.shoppyjuan.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.arkus.shoppyjuan.data.local.ShoppyDatabase
import com.arkus.shoppyjuan.data.local.entity.SyncActionType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: ShoppyDatabase,
    private val supabaseClient: SupabaseClient
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "offline_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun scheduleImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    private val pendingSyncDao = database.pendingSyncDao()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        return try {
            val pendingActions = pendingSyncDao.getPendingActionsToSync(50)

            if (pendingActions.isEmpty()) {
                return Result.success()
            }

            var successCount = 0
            var failCount = 0

            for (action in pendingActions) {
                try {
                    val success = processAction(action)
                    if (success) {
                        pendingSyncDao.deleteActionById(action.id)
                        successCount++
                    } else {
                        pendingSyncDao.incrementRetryCount(action.id, "Sync failed")
                        failCount++
                    }
                } catch (e: Exception) {
                    pendingSyncDao.incrementRetryCount(action.id, e.message)
                    failCount++
                }
            }

            // Clean up actions that have failed too many times
            pendingSyncDao.deleteFailedActions(maxRetries = 5)

            if (failCount > 0 && successCount == 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun processAction(action: com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity): Boolean {
        return try {
            when (action.actionType) {
                SyncActionType.CREATE_LIST -> syncCreateList(action)
                SyncActionType.UPDATE_LIST -> syncUpdateList(action)
                SyncActionType.DELETE_LIST -> syncDeleteList(action)
                SyncActionType.CREATE_ITEM -> syncCreateItem(action)
                SyncActionType.UPDATE_ITEM -> syncUpdateItem(action)
                SyncActionType.DELETE_ITEM -> syncDeleteItem(action)
                SyncActionType.CHECK_ITEM -> syncCheckItem(action)
                SyncActionType.CREATE_NOTE -> syncCreateNote(action)
                SyncActionType.DELETE_NOTE -> syncDeleteNote(action)
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun syncCreateList(action: com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity): Boolean {
        val listData = json.decodeFromString<Map<String, String>>(action.payloadJson)
        supabaseClient.postgrest["shopping_lists"].insert(listData)
        return true
    }

    private suspend fun syncUpdateList(action: com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity): Boolean {
        val listData = json.decodeFromString<Map<String, String>>(action.payloadJson)
        supabaseClient.postgrest["shopping_lists"]
            .update(listData) {
                filter { eq("id", action.entityId) }
            }
        return true
    }

    private suspend fun syncDeleteList(action: com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity): Boolean {
        supabaseClient.postgrest["shopping_lists"]
            .delete {
                filter { eq("id", action.entityId) }
            }
        return true
    }

    private suspend fun syncCreateItem(action: com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity): Boolean {
        val itemData = json.decodeFromString<Map<String, String>>(action.payloadJson)
        supabaseClient.postgrest["list_items"].insert(itemData)
        return true
    }

    private suspend fun syncUpdateItem(action: com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity): Boolean {
        val itemData = json.decodeFromString<Map<String, String>>(action.payloadJson)
        supabaseClient.postgrest["list_items"]
            .update(itemData) {
                filter { eq("id", action.entityId) }
            }
        return true
    }

    private suspend fun syncDeleteItem(action: com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity): Boolean {
        supabaseClient.postgrest["list_items"]
            .delete {
                filter { eq("id", action.entityId) }
            }
        return true
    }

    private suspend fun syncCheckItem(action: com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity): Boolean {
        val checkData = json.decodeFromString<Map<String, Boolean>>(action.payloadJson)
        supabaseClient.postgrest["list_items"]
            .update(checkData) {
                filter { eq("id", action.entityId) }
            }
        return true
    }

    private suspend fun syncCreateNote(action: com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity): Boolean {
        val noteData = json.decodeFromString<Map<String, String>>(action.payloadJson)
        supabaseClient.postgrest["notes"].insert(noteData)
        return true
    }

    private suspend fun syncDeleteNote(action: com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity): Boolean {
        supabaseClient.postgrest["notes"]
            .delete {
                filter { eq("id", action.entityId) }
            }
        return true
    }
}
