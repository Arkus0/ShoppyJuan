package com.arkus.shoppyjuan.di

import com.arkus.shoppyjuan.data.repository.RecipeRepositoryImpl
import com.arkus.shoppyjuan.data.repository.ShoppingListRepositoryImpl
import com.arkus.shoppyjuan.domain.repository.RecipeRepository
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindShoppingListRepository(
        impl: ShoppingListRepositoryImpl
    ): ShoppingListRepository

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(
        impl: RecipeRepositoryImpl
    ): RecipeRepository
}
