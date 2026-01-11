package com.arkus.shoppyjuan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.arkus.shoppyjuan.presentation.auth.AuthScreen
import com.arkus.shoppyjuan.presentation.favorites.FavoritesScreen
import com.arkus.shoppyjuan.presentation.home.HomeScreen
import com.arkus.shoppyjuan.presentation.listdetail.ListDetailScreen
import com.arkus.shoppyjuan.presentation.prices.PriceComparisonScreen
import com.arkus.shoppyjuan.presentation.prices.PriceComparisonViewModel
import com.arkus.shoppyjuan.presentation.profile.ProfileScreen
import com.arkus.shoppyjuan.presentation.recipedetail.RecipeDetailScreen
import com.arkus.shoppyjuan.presentation.recipes.RecipesScreen
import com.arkus.shoppyjuan.presentation.supermarket.SupermarketModeScreen

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object ListDetail : Screen("list/{listId}") {
        fun createRoute(listId: String) = "list/$listId"
    }
    object SupermarketMode : Screen("supermarket/{listId}") {
        fun createRoute(listId: String) = "supermarket/$listId"
    }
    object PriceComparison : Screen("prices/{listId}") {
        fun createRoute(listId: String) = "prices/$listId"
    }
    object Recipes : Screen("recipes")
    object RecipeDetail : Screen("recipe/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe/$recipeId"
    }
    object Favorites : Screen("favorites")
    object Profile : Screen("profile")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth
        composable(Screen.Auth.route) {
            AuthScreen(navController)
        }

        // Home/Lists
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        // List Detail
        composable(
            route = Screen.ListDetail.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType }
            )
        ) {
            ListDetailScreen(navController)
        }

        // Supermarket Mode
        composable(
            route = Screen.SupermarketMode.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType }
            )
        ) {
            SupermarketModeScreen(navController)
        }

        // Price Comparison
        composable(
            route = Screen.PriceComparison.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType }
            )
        ) {
            val viewModel: PriceComparisonViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            PriceComparisonScreen(
                uiState = uiState,
                onViewModeChange = viewModel::setViewMode,
                onVerifyPrice = viewModel::verifyPrice,
                onShowOpenPricesLogin = viewModel::showOpenPricesLogin,
                onShowContributeDialog = viewModel::showContributeDialog,
                onLoginToOpenPrices = viewModel::loginToOpenPrices,
                onLogoutFromOpenPrices = viewModel::logoutFromOpenPrices,
                onContributeAllReceipts = viewModel::contributeAllReceipts,
                onBack = { navController.popBackStack() }
            )
        }

        // Recipes
        composable(Screen.Recipes.route) {
            RecipesScreen(navController)
        }

        // Recipe Detail
        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(
                navArgument("recipeId") { type = NavType.StringType }
            )
        ) {
            RecipeDetailScreen(navController)
        }

        // Favorites
        composable(Screen.Favorites.route) {
            FavoritesScreen(navController)
        }

        // Profile
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
    }
}
