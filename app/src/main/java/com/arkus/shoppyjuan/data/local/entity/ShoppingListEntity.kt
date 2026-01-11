package com.arkus.shoppyjuan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val ownerId: String,
    val shareCode: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Recurrence settings
    val isRecurring: Boolean = false,
    val recurrenceSettingsJson: String? = null, // Serialized RecurrenceSettings
    val nextRecurrenceAt: Long? = null,
    val lastResetAt: Long? = null
)
