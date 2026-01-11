package com.arkus.shoppyjuan.domain.model

import com.arkus.shoppyjuan.data.local.entity.ListItemEntity

data class ListItem(
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
    val tags: List<String> = emptyList(),
    val emoji: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun ListItemEntity.toDomain(): ListItem {
    return ListItem(
        id = id,
        listId = listId,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category,
        checked = checked,
        addedBy = addedBy,
        checkedBy = checkedBy,
        assignedTo = assignedTo,
        position = position,
        imageUrl = imageUrl,
        note = note,
        tags = tags?.split(",") ?: emptyList(),
        emoji = emoji,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ListItem.toEntity(): ListItemEntity {
    return ListItemEntity(
        id = id,
        listId = listId,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category,
        checked = checked,
        addedBy = addedBy,
        checkedBy = checkedBy,
        assignedTo = assignedTo,
        position = position,
        imageUrl = imageUrl,
        note = note,
        tags = tags.joinToString(","),
        emoji = emoji,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
