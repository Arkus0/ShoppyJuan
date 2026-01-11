package com.arkus.shoppyjuan.presentation.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.domain.model.Recipe
import com.arkus.shoppyjuan.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipesUiState(
    val recipes: List<Recipe> = emptyList(),
    val favoriteRecipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadRecipes()
        loadFavorites()
    }

    private fun loadRecipes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllRecipes().collect { recipes ->
                    _uiState.update {
                        it.copy(
                            recipes = recipes,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            try {
                repository.getFavoriteRecipes().collect { favorites ->
                    _uiState.update { it.copy(favoriteRecipes = favorites) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun searchRecipes(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    loadRecipes()
                } else {
                    repository.searchRecipes(query).collect { recipes ->
                        _uiState.update { it.copy(recipes = recipes) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleFavorite(recipeId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(recipeId, isFavorite)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                repository.saveRecipe(recipe)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecipe(recipeId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
