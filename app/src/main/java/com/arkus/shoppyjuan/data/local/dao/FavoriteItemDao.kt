package com.arkus.shoppyjuan.data.local.dao

import androidx.room.*
import com.arkus.shoppyjuan.data.local.entity.FavoriteItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteItemDao {
    @Query("SELECT * FROM favorite_items WHERE userId = :userId ORDER BY createdAt DESC")
    fun getFavoriteItems(userId: String): Flow<List<FavoriteItemEntity>>

    @Query("SELECT * FROM favorite_items WHERE userId = :userId AND category = :category")
    fun getFavoritesByCategory(userId: String, category: String): Flow<List<FavoriteItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteItemEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteItemEntity)

    @Query("DELETE FROM favorite_items WHERE id = :favoriteId")
    suspend fun deleteFavoriteById(favoriteId: String)

    @Query("DELETE FROM favorite_items WHERE userId = :userId")
    suspend fun deleteAllFavorites(userId: String)
}
