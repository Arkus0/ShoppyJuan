package com.arkus.shoppyjuan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arkus.shoppyjuan.data.local.dao.*
import com.arkus.shoppyjuan.data.local.entity.*

@Database(
    entities = [
        ShoppingListEntity::class,
        ListItemEntity::class,
        RecipeEntity::class,
        FavoriteItemEntity::class,
        NoteEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class ShoppyDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun listItemDao(): ListItemDao
    abstract fun recipeDao(): RecipeDao
    abstract fun favoriteItemDao(): FavoriteItemDao
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "shoppy_juan_db"
    }
}
