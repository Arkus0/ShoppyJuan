package com.arkus.shoppyjuan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "favorite_items")
data class FavoriteItemEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val category: String? = null,
    val emoji: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
