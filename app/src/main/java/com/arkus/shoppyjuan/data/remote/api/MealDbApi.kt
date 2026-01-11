package com.arkus.shoppyjuan.data.remote.api

import com.arkus.shoppyjuan.data.remote.dto.MealCategoriesResponse
import com.arkus.shoppyjuan.data.remote.dto.MealDetailResponse
import com.arkus.shoppyjuan.data.remote.dto.MealSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MealDbApi {

    @GET("search.php")
    suspend fun searchMeals(
        @Query("s") query: String
    ): MealSearchResponse

    @GET("lookup.php")
    suspend fun getMealById(
        @Query("i") id: String
    ): MealDetailResponse

    @GET("random.php")
    suspend fun getRandomMeal(): MealDetailResponse

    @GET("categories.php")
    suspend fun getCategories(): MealCategoriesResponse

    @GET("filter.php")
    suspend fun filterByCategory(
        @Query("c") category: String
    ): MealSearchResponse

    @GET("filter.php")
    suspend fun filterByArea(
        @Query("a") area: String
    ): MealSearchResponse

    companion object {
        const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"
    }
}
