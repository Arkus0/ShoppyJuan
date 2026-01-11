package com.arkus.shoppyjuan.presentation.listdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.domain.model.ListItem
import com.arkus.shoppyjuan.domain.model.ShoppingList
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ListDetailUiState(
    val list: ShoppingList? = null,
    val items: List<ListItem> = emptyList(),
    val uncheckedItems: List<ListItem> = emptyList(),
    val checkedItems: List<ListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    private val repository: ShoppingListRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])

    private val _uiState = MutableStateFlow(ListDetailUiState())
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    init {
        loadListDetails()
    }

    private fun loadListDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Load list info
                repository.getListById(listId).collect { list ->
                    _uiState.update { it.copy(list = list) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }

        viewModelScope.launch {
            try {
                // Load all items
                repository.getItemsByListId(listId).collect { items ->
                    _uiState.update { it.copy(items = items, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }

        viewModelScope.launch {
            try {
                // Load unchecked items
                repository.getUncheckedItems(listId).collect { items ->
                    _uiState.update { it.copy(uncheckedItems = items) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }

        viewModelScope.launch {
            try {
                // Load checked items
                repository.getCheckedItems(listId).collect { items ->
                    _uiState.update { it.copy(checkedItems = items) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addItem(
        name: String,
        quantity: Double = 1.0,
        unit: String? = null,
        category: String? = null
    ) {
        viewModelScope.launch {
            try {
                val item = ListItem(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    category = category,
                    position = _uiState.value.items.size
                )
                repository.addItem(item)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
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

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                repository.deleteItem(itemId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateItem(item: ListItem) {
        viewModelScope.launch {
            try {
                repository.updateItem(item)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearCheckedItems() {
        viewModelScope.launch {
            try {
                repository.deleteCheckedItems(listId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
