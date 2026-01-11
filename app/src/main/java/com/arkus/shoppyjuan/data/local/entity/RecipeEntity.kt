package com.arkus.shoppyjuan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String? = null,
    val area: String? = null,
    val instructions: String? = null,
    val imageUrl: String? = null,
    val ingredients: String, // JSON string array
    val measures: String, // JSON string array
    val userId: String? = null,
    val isFavorite: Boolean = false,
    val shareCode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
