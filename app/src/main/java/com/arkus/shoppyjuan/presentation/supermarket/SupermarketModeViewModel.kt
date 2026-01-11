package com.arkus.shoppyjuan.presentation.supermarket

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.domain.model.ListItem
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupermarketModeUiState(
    val listName: String? = null,
    val items: List<ListItem> = emptyList(),
    val totalItems: Int = 0,
    val checkedItems: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SupermarketModeViewModel @Inject constructor(
    private val repository: ShoppingListRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])

    private val _uiState = MutableStateFlow(SupermarketModeUiState())
    val uiState: StateFlow<SupermarketModeUiState> = _uiState.asStateFlow()

    init {
        loadListData()
    }

    private fun loadListData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getListById(listId).collect { list ->
                    _uiState.update { it.copy(listName = list?.name) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }

        viewModelScope.launch {
            try {
                repository.getItemsByListId(listId).collect { items ->
                    val checkedCount = items.count { it.checked }
                    _uiState.update {
                        it.copy(
                            items = items,
                            totalItems = items.size,
                            checkedItems = checkedCount,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun toggleItemChecked(itemId: String, checked: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleItemChecked(itemId, checked)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun markUnavailable(itemId: String) {
        viewModelScope.launch {
            try {
                // Mark as checked and add "No había" note
                repository.toggleItemChecked(itemId, true)
                repository.updateItemNote(itemId, "No había en el supermercado")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
