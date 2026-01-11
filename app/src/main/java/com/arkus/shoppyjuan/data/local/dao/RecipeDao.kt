package com.arkus.shoppyjuan.data.local.dao

import androidx.room.*
import com.arkus.shoppyjuan.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    fun getRecipeById(recipeId: String): Flow<RecipeEntity?>

    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    suspend fun getRecipeByIdOnce(recipeId: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE category = :category ORDER BY createdAt DESC")
    fun getRecipesByCategory(category: String): Flow<List<RecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipeById(recipeId: String)

    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :recipeId")
    suspend fun setFavoriteStatus(recipeId: String, isFavorite: Boolean)

    @Query("SELECT * FROM recipes WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchRecipes(query: String): Flow<List<RecipeEntity>>
}
