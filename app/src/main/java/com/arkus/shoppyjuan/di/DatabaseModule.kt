package com.arkus.shoppyjuan.di

import android.content.Context
import androidx.room.Room
import com.arkus.shoppyjuan.data.local.ShoppyDatabase
import com.arkus.shoppyjuan.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideShoppyDatabase(
        @ApplicationContext context: Context
    ): ShoppyDatabase {
        return Room.databaseBuilder(
            context,
            ShoppyDatabase::class.java,
            ShoppyDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideShoppingListDao(database: ShoppyDatabase): ShoppingListDao {
        return database.shoppingListDao()
    }

    @Provides
    fun provideListItemDao(database: ShoppyDatabase): ListItemDao {
        return database.listItemDao()
    }

    @Provides
    fun provideRecipeDao(database: ShoppyDatabase): RecipeDao {
        return database.recipeDao()
    }

    @Provides
    fun provideFavoriteItemDao(database: ShoppyDatabase): FavoriteItemDao {
        return database.favoriteItemDao()
    }

    @Provides
    fun provideNoteDao(database: ShoppyDatabase): NoteDao {
        return database.noteDao()
    }
}
