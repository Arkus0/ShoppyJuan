package com.arkus.shoppyjuan.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ListItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["listId"]),
        Index(value = ["itemId"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val listId: String,
    val itemId: String? = null,
    val userId: String,
    val userName: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
