package com.arkus.shoppyjuan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class SyncActionType {
    CREATE_LIST,
    UPDATE_LIST,
    DELETE_LIST,
    CREATE_ITEM,
    UPDATE_ITEM,
    DELETE_ITEM,
    CHECK_ITEM,
    CREATE_NOTE,
    DELETE_NOTE
}

@Serializable
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: SyncActionType,
    val entityId: String, // ID of the list/item/note
    val parentId: String? = null, // For items/notes, the list ID
    val payloadJson: String, // Serialized data to sync
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)
