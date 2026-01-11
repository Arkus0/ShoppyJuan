package com.arkus.shoppyjuan.data.local.dao

import androidx.room.*
import com.arkus.shoppyjuan.data.local.entity.PendingSyncEntity
import com.arkus.shoppyjuan.data.local.entity.SyncActionType
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    fun getAllPendingActions(): Flow<List<PendingSyncEntity>>

    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingActionsToSync(limit: Int = 50): List<PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_sync")
    suspend fun getPendingCountOnce(): Int

    @Insert
    suspend fun insertAction(action: PendingSyncEntity): Long

    @Update
    suspend fun updateAction(action: PendingSyncEntity)

    @Delete
    suspend fun deleteAction(action: PendingSyncEntity)

    @Query("DELETE FROM pending_sync WHERE id = :actionId")
    suspend fun deleteActionById(actionId: Long)

    @Query("DELETE FROM pending_sync WHERE entityId = :entityId AND actionType = :actionType")
    suspend fun deleteActionByEntityAndType(entityId: String, actionType: SyncActionType)

    @Query("DELETE FROM pending_sync WHERE entityId = :entityId")
    suspend fun deleteActionsForEntity(entityId: String)

    @Query("UPDATE pending_sync SET retryCount = retryCount + 1, lastError = :error WHERE id = :actionId")
    suspend fun incrementRetryCount(actionId: Long, error: String?)

    @Query("DELETE FROM pending_sync WHERE retryCount >= :maxRetries")
    suspend fun deleteFailedActions(maxRetries: Int = 5)

    @Query("DELETE FROM pending_sync")
    suspend fun deleteAll()
}
