package com.arkus.shoppyjuan.domain.model

import com.arkus.shoppyjuan.data.local.entity.RecipeEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val category: String? = null,
    val area: String? = null,
    val instructions: String? = null,
    val imageUrl: String? = null,
    val ingredients: List<String> = emptyList(),
    val measures: List<String> = emptyList(),
    val userId: String? = null,
    val isFavorite: Boolean = false,
    val shareCode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun RecipeEntity.toDomain(): Recipe {
    return Recipe(
        id = id,
        name = name,
        category = category,
        area = area,
        instructions = instructions,
        imageUrl = imageUrl,
        ingredients = try {
            Json.decodeFromString(ListSerializer(String.serializer()), ingredients)
        } catch (e: Exception) {
            emptyList()
        },
        measures = try {
            Json.decodeFromString(ListSerializer(String.serializer()), measures)
        } catch (e: Exception) {
            emptyList()
        },
        userId = userId,
        isFavorite = isFavorite,
        shareCode = shareCode,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Recipe.toEntity(): RecipeEntity {
    return RecipeEntity(
        id = id,
        name = name,
        category = category,
        area = area,
        instructions = instructions,
        imageUrl = imageUrl,
        ingredients = Json.encodeToString(ListSerializer(String.serializer()), ingredients),
        measures = Json.encodeToString(ListSerializer(String.serializer()), measures),
        userId = userId,
        isFavorite = isFavorite,
        shareCode = shareCode,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
