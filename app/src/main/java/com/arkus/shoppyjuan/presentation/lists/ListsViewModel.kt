package com.arkus.shoppyjuan.presentation.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.domain.model.ShoppingList
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListsUiState(
    val lists: List<ShoppingList> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val repository: ShoppingListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    init {
        loadLists()
    }

    private fun loadLists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllLists().collect { lists ->
                    _uiState.update {
                        it.copy(
                            lists = lists,
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

    fun createList(name: String) {
        viewModelScope.launch {
            try {
                // TODO: Get actual user ID from auth
                repository.createList(name, "temp-user-id")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteList(listId: String) {
        viewModelScope.launch {
            try {
                repository.deleteList(listId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun archiveList(listId: String) {
        viewModelScope.launch {
            try {
                repository.archiveList(listId, true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
