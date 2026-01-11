package com.arkus.shoppyjuan.domain.repository

import com.arkus.shoppyjuan.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun getAllRecipes(): Flow<List<Recipe>>
    fun getFavoriteRecipes(): Flow<List<Recipe>>
    fun getRecipeById(recipeId: String): Flow<Recipe?>
    fun searchRecipes(query: String): Flow<List<Recipe>>
    suspend fun saveRecipe(recipe: Recipe)
    suspend fun deleteRecipe(recipeId: String)
    suspend fun toggleFavorite(recipeId: String, isFavorite: Boolean)
    suspend fun searchRecipesOnline(query: String): List<Recipe>
    suspend fun exportIngredientsToList(recipeId: String, listId: String, multiplier: Int = 1)
}
