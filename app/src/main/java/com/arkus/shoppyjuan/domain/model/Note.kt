package com.arkus.shoppyjuan.domain.model

import com.arkus.shoppyjuan.data.local.entity.NoteEntity

data class Note(
    val id: String,
    val listId: String,
    val itemId: String? = null, // If null, it's a list-level note
    val userId: String,
    val userName: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun NoteEntity.toDomain(): Note {
    return Note(
        id = id,
        listId = listId,
        itemId = itemId,
        userId = userId,
        userName = userName,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        listId = listId,
        itemId = itemId,
        userId = userId,
        userName = userName,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
