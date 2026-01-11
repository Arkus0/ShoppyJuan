package com.arkus.shoppyjuan.presentation.listdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.data.barcode.BarcodeScannerManager
import com.arkus.shoppyjuan.data.realtime.RealtimeManager
import com.arkus.shoppyjuan.data.speech.VoiceInputManager
import com.arkus.shoppyjuan.domain.model.ListItem
import com.arkus.shoppyjuan.domain.model.Note
import com.arkus.shoppyjuan.domain.model.ShoppingList
import com.arkus.shoppyjuan.domain.repository.NoteRepository
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import com.arkus.shoppyjuan.domain.util.ProductCategory
import com.arkus.shoppyjuan.presentation.components.OnlineUser
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
    val notes: List<Note> = emptyList(),
    val noteCount: Int = 0,
    val onlineUsers: List<OnlineUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    private val repository: ShoppingListRepository,
    private val noteRepository: NoteRepository,
    private val realtimeManager: RealtimeManager,
    val voiceInputManager: VoiceInputManager,
    val barcodeScannerManager: BarcodeScannerManager,
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

        viewModelScope.launch {
            try {
                // Load notes
                noteRepository.getListNotes(listId).collect { notes ->
                    _uiState.update {
                        it.copy(notes = notes, noteCount = notes.size)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }

        // Track presence
        startPresenceTracking()
    }

    private fun startPresenceTracking() {
        viewModelScope.launch {
            try {
                val currentUserId = "current_user_id" // TODO: Get from AuthRepository
                realtimeManager.trackPresence(listId, currentUserId).collect { presenceMap ->
                    val onlineUsers = presenceMap.map { (userId, isOnline) ->
                        OnlineUser(
                            userId = userId,
                            userName = "Usuario ${userId.take(4)}", // TODO: Get real name from user service
                            isOnline = isOnline
                        )
                    }
                    _uiState.update { it.copy(onlineUsers = onlineUsers) }
                }
            } catch (e: Exception) {
                // Silently fail - presence is not critical
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                realtimeManager.unsubscribeFromList(listId)
            } catch (e: Exception) {
                // Ignore cleanup errors
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
                // Detect category and emoji automatically
                val detectedCategory = ProductCategory.detectCategory(name)
                val emoji = detectedCategory.emoji

                val item = ListItem(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    category = category ?: detectedCategory.label,
                    emoji = emoji,
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

    // Note management
    fun addNote(content: String, userId: String, userName: String) {
        viewModelScope.launch {
            try {
                val note = Note(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    itemId = null, // List-level note
                    userId = userId,
                    userName = userName,
                    content = content
                )
                noteRepository.addNote(note)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                noteRepository.deleteNote(noteId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
