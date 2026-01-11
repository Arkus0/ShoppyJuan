package com.arkus.shoppyjuan.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "list_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class ListItemEntity(
    @PrimaryKey
    val id: String,
    val listId: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val category: String? = null,
    val checked: Boolean = false,
    val addedBy: String? = null,
    val checkedBy: String? = null,
    val assignedTo: String? = null,
    val position: Int = 0,
    val imageUrl: String? = null,
    val note: String? = null,
    val tags: String? = null, // JSON string array
    val emoji: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
