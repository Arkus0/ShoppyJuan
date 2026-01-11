package com.arkus.shoppyjuan.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.arkus.shoppyjuan.presentation.home.HomeScreen
import com.arkus.shoppyjuan.presentation.listdetail.ListDetailScreen
import com.arkus.shoppyjuan.presentation.lists.ListsScreen
import com.arkus.shoppyjuan.presentation.recipes.RecipesScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Lists : Screen("lists")
    object ListDetail : Screen("list/{listId}") {
        fun createRoute(listId: String) = "list/$listId"
    }
    object Recipes : Screen("recipes")
    object Favorites : Screen("favorites")
    object Profile : Screen("profile")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.Lists.route) {
            ListsScreen(navController = navController)
        }

        composable(
            route = Screen.ListDetail.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType }
            )
        ) {
            ListDetailScreen(navController = navController)
        }

        composable(Screen.Recipes.route) {
            RecipesScreen(navController = navController)
        }

        composable(Screen.Favorites.route) {
            // TODO: FavoritesScreen
        }

        composable(Screen.Profile.route) {
            // TODO: ProfileScreen
        }
    }
}
