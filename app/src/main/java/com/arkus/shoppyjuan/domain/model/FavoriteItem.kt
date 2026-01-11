package com.arkus.shoppyjuan.domain.model

import com.arkus.shoppyjuan.data.local.entity.FavoriteItemEntity

data class FavoriteItem(
    val id: String,
    val userId: String = "",
    val name: String,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val category: String? = null,
    val emoji: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

fun FavoriteItemEntity.toDomain(): FavoriteItem {
    return FavoriteItem(
        id = id,
        userId = userId,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category,
        emoji = emoji,
        createdAt = createdAt
    )
}

fun FavoriteItem.toEntity(): FavoriteItemEntity {
    return FavoriteItemEntity(
        id = id,
        userId = userId,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category,
        emoji = emoji,
        createdAt = createdAt
    )
}
