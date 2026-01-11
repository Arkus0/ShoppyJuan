package com.arkus.shoppyjuan.domain.model

import com.arkus.shoppyjuan.data.local.entity.ShoppingListEntity

data class ShoppingList(
    val id: String,
    val name: String,
    val ownerId: String,
    val shareCode: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0,
    val uncheckedCount: Int = 0
)

fun ShoppingListEntity.toDomain(): ShoppingList {
    return ShoppingList(
        id = id,
        name = name,
        ownerId = ownerId,
        shareCode = shareCode,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ShoppingList.toEntity(): ShoppingListEntity {
    return ShoppingListEntity(
        id = id,
        name = name,
        ownerId = ownerId,
        shareCode = shareCode,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
