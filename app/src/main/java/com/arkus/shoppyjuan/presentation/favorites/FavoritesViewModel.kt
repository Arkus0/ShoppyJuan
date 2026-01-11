package com.arkus.shoppyjuan.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.domain.model.FavoriteItem
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import com.arkus.shoppyjuan.domain.util.ProductCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<FavoriteItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: ShoppingListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getFavoriteItems().collect { favorites ->
                    _uiState.update {
                        it.copy(
                            favorites = favorites,
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

    fun addFavorite(
        name: String,
        quantity: Double = 1.0,
        unit: String? = null
    ) {
        viewModelScope.launch {
            try {
                val detectedCategory = ProductCategory.detectCategory(name)
                val favorite = FavoriteItem(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    category = detectedCategory.label,
                    emoji = detectedCategory.emoji
                )
                repository.addFavoriteItem(favorite)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteFavorite(favoriteId: String) {
        viewModelScope.launch {
            try {
                repository.deleteFavoriteItem(favoriteId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
