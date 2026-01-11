package com.arkus.shoppyjuan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.arkus.shoppyjuan.data.local.dao.*
import com.arkus.shoppyjuan.data.local.entity.*

@Database(
    entities = [
        ShoppingListEntity::class,
        ListItemEntity::class,
        RecipeEntity::class,
        FavoriteItemEntity::class,
        NoteEntity::class,
        FrequentItemEntity::class,
        PendingSyncEntity::class,
        // Price comparison entities
        StoreEntity::class,
        PriceRecordEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        PriceContributorEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class ShoppyDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun listItemDao(): ListItemDao
    abstract fun recipeDao(): RecipeDao
    abstract fun favoriteItemDao(): FavoriteItemDao
    abstract fun noteDao(): NoteDao
    abstract fun frequentItemDao(): FrequentItemDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun priceDao(): PriceDao

    companion object {
        const val DATABASE_NAME = "shoppy_juan_db"

        @Volatile
        private var INSTANCE: ShoppyDatabase? = null

        /**
         * Get database instance for non-DI contexts (e.g., widgets)
         */
        fun getInstance(context: Context): ShoppyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShoppyDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
