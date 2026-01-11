package com.arkus.shoppyjuan.presentation.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.domain.model.Recipe
import com.arkus.shoppyjuan.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipesUiState(
    val recipes: List<Recipe> = emptyList(),
    val favoriteRecipes: List<Recipe> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Cache for search results to avoid re-fetching
    private val searchCache = mutableMapOf<String, List<Recipe>>()
    private var allRecipesCache: List<Recipe>? = null

    init {
        loadInitialData()
        setupSearchDebounce()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load recipes and favorites in parallel
            launch {
                repository.getAllRecipes()
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { recipes ->
                        allRecipesCache = recipes
                        val categories = recipes.mapNotNull { it.category }.distinct().sorted()
                        _uiState.update {
                            it.copy(
                                recipes = recipes,
                                categories = categories,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            }

            launch {
                repository.getFavoriteRecipes()
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { favorites ->
                        _uiState.update { it.copy(favoriteRecipes = favorites) }
                    }
            }
        }
    }

    private fun setupSearchDebounce() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after user stops typing
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    private suspend fun performSearch(query: String) {
        val trimmedQuery = query.trim().lowercase()

        if (trimmedQuery.isBlank()) {
            // Reset to all recipes
            _uiState.update {
                it.copy(
                    recipes = allRecipesCache ?: emptyList(),
                    isSearching = false
                )
            }
            return
        }

        // Check cache first
        searchCache[trimmedQuery]?.let { cachedResults ->
            _uiState.update {
                it.copy(recipes = cachedResults, isSearching = false)
            }
            return
        }

        // Perform search
        _uiState.update { it.copy(isSearching = true) }

        try {
            repository.searchRecipes(query)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isSearching = false) }
                }
                .collect { recipes ->
                    // Cache the results
                    searchCache[trimmedQuery] = recipes
                    _uiState.update {
                        it.copy(recipes = recipes, isSearching = false)
                    }
                }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isSearching = false) }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun filterByCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }

        viewModelScope.launch {
            val allRecipes = allRecipesCache ?: return@launch

            val filteredRecipes = if (category == null) {
                allRecipes
            } else {
                allRecipes.filter { it.category == category }
            }

            _uiState.update { it.copy(recipes = filteredRecipes) }
        }
    }

    fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(recipeId, isFavorite)

                // Update local state optimistically
                _uiState.update { state ->
                    val updatedRecipes = state.recipes.map { recipe ->
                        if (recipe.id == recipeId) recipe.copy(isFavorite = isFavorite)
                        else recipe
                    }
                    state.copy(recipes = updatedRecipes)
                }

                // Update cache
                allRecipesCache = allRecipesCache?.map { recipe ->
                    if (recipe.id == recipeId) recipe.copy(isFavorite = isFavorite)
                    else recipe
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                repository.saveRecipe(recipe)
                // Clear cache to force refresh
                searchCache.clear()
                allRecipesCache = null
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecipe(recipeId)

                // Update local state optimistically
                _uiState.update { state ->
                    state.copy(
                        recipes = state.recipes.filter { it.id != recipeId },
                        favoriteRecipes = state.favoriteRecipes.filter { it.id != recipeId }
                    )
                }

                // Update cache
                allRecipesCache = allRecipesCache?.filter { it.id != recipeId }
                searchCache.clear()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun refresh() {
        searchCache.clear()
        allRecipesCache = null
        loadInitialData()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
