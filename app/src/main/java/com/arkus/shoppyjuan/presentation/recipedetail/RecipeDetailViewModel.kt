package com.arkus.shoppyjuan.presentation.recipedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.domain.model.Recipe
import com.arkus.shoppyjuan.domain.model.ShoppingList
import com.arkus.shoppyjuan.domain.repository.RecipeRepository
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val lists: List<ShoppingList> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val exportSuccess: Boolean = false
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val listRepository: ShoppingListRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle["recipeId"])

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    init {
        loadRecipe()
        loadLists()
    }

    private fun loadRecipe() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                recipeRepository.getRecipeById(recipeId).collect { recipe ->
                    _uiState.update { it.copy(recipe = recipe, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadLists() {
        viewModelScope.launch {
            try {
                listRepository.getAllLists().collect { lists ->
                    _uiState.update { it.copy(lists = lists) }
                }
            } catch (e: Exception) {
                // Silently fail - not critical
            }
        }
    }

    fun exportToList(listId: String, multiplier: Int = 1) {
        viewModelScope.launch {
            try {
                recipeRepository.exportIngredientsToList(recipeId, listId, multiplier)
                _uiState.update { it.copy(exportSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                val isFavorite = _uiState.value.recipe?.isFavorite ?: false
                recipeRepository.toggleFavorite(recipeId, !isFavorite)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearExportSuccess() {
        _uiState.update { it.copy(exportSuccess = false) }
    }
}
