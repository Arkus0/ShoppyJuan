package com.arkus.shoppyjuan.data.repository

import com.arkus.shoppyjuan.data.local.dao.RecipeDao
import com.arkus.shoppyjuan.data.remote.api.MealDbApi
import com.arkus.shoppyjuan.data.remote.mapper.toRecipe
import com.arkus.shoppyjuan.domain.model.Recipe
import com.arkus.shoppyjuan.domain.model.toDomain
import com.arkus.shoppyjuan.domain.model.toEntity
import com.arkus.shoppyjuan.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecipeRepositoryImpl @Inject constructor(
    private val recipeDao: RecipeDao,
    private val mealDbApi: MealDbApi
) : RecipeRepository {

    override fun getAllRecipes(): Flow<List<Recipe>> {
        return recipeDao.getAllRecipes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteRecipes(): Flow<List<Recipe>> {
        return recipeDao.getFavoriteRecipes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecipeById(recipeId: String): Flow<Recipe?> {
        return recipeDao.getRecipeById(recipeId).map { it?.toDomain() }
    }

    override fun searchRecipes(query: String): Flow<List<Recipe>> {
        return recipeDao.searchRecipes(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveRecipe(recipe: Recipe) {
        recipeDao.insertRecipe(recipe.toEntity())
    }

    override suspend fun deleteRecipe(recipeId: String) {
        recipeDao.deleteRecipeById(recipeId)
    }

    override suspend fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        recipeDao.setFavoriteStatus(recipeId, isFavorite)
    }

    override suspend fun searchRecipesOnline(query: String): List<Recipe> {
        return try {
            val response = mealDbApi.searchMeals(query)
            response.meals?.map { it.toRecipe() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getRandomRecipe(): Recipe? {
        return try {
            val response = mealDbApi.getRandomMeal()
            response.meals?.firstOrNull()?.toRecipe()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getRecipeDetailsById(recipeId: String): Recipe? {
        return try {
            val response = mealDbApi.getMealById(recipeId)
            response.meals?.firstOrNull()?.toRecipe()
        } catch (e: Exception) {
            null
        }
    }
}
