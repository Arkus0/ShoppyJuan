package com.arkus.shoppyjuan.presentation.listdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.data.barcode.BarcodeScannerManager
import com.arkus.shoppyjuan.data.local.entity.FrequentItemEntity
import com.arkus.shoppyjuan.data.realtime.RealtimeManager
import com.arkus.shoppyjuan.data.repository.FrequentItemRepository
import com.arkus.shoppyjuan.data.speech.VoiceInputManager
import com.arkus.shoppyjuan.domain.model.ListItem
import com.arkus.shoppyjuan.domain.model.Note
import com.arkus.shoppyjuan.domain.model.ShoppingList
import com.arkus.shoppyjuan.domain.repository.NoteRepository
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import com.arkus.shoppyjuan.domain.user.CurrentUser
import com.arkus.shoppyjuan.domain.user.UserManager
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
    val currentUser: CurrentUser? = null,
    val frequentItems: List<FrequentItemEntity> = emptyList(),
    val suggestedItems: List<FrequentItemEntity> = emptyList(),
    val isReorderMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    private val repository: ShoppingListRepository,
    private val noteRepository: NoteRepository,
    private val realtimeManager: RealtimeManager,
    private val frequentItemRepository: FrequentItemRepository,
    private val userManager: UserManager,
    val voiceInputManager: VoiceInputManager,
    val barcodeScannerManager: BarcodeScannerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])

    private val _uiState = MutableStateFlow(ListDetailUiState())
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    // Expose current user info for the UI
    val currentUserId: String get() = userManager.currentUserId
    val currentUserName: String get() = userManager.currentUserName

    init {
        loadCurrentUser()
        loadListDetails()
        loadFrequentItems()
    }

    private fun loadCurrentUser() {
        userManager.refreshUser()
        _uiState.update { it.copy(currentUser = userManager.currentUser.value) }

        viewModelScope.launch {
            userManager.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    private fun loadListDetails() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            repository.getListById(listId)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(list = list, isLoading = false) }
                }
        }

        viewModelScope.launch {
            repository.getItemsByListId(listId)
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { items ->
                    val (unchecked, checked) = items.partition { !it.checked }
                    _uiState.update {
                        it.copy(
                            items = items,
                            uncheckedItems = unchecked.sortedBy { item -> item.position },
                            checkedItems = checked.sortedBy { item -> item.position },
                            isLoading = false
                        )
                    }
                }
        }

        viewModelScope.launch {
            noteRepository.getListNotes(listId)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { notes ->
                    _uiState.update { it.copy(notes = notes, noteCount = notes.size) }
                }
        }

        startPresenceTracking()
    }

    private fun loadFrequentItems() {
        viewModelScope.launch {
            frequentItemRepository.getTopFrequentItems(15)
                .catch { /* Ignore errors for suggestions */ }
                .collect { items ->
                    _uiState.update { it.copy(frequentItems = items) }
                }
        }
    }

    private fun startPresenceTracking() {
        viewModelScope.launch {
            try {
                val userId = userManager.currentUserId.ifEmpty { "anonymous_${UUID.randomUUID().toString().take(8)}" }
                realtimeManager.trackPresence(listId, userId)
                    .catch { /* Silently fail */ }
                    .collect { presenceMap ->
                        val onlineUsers = presenceMap.map { (id, isOnline) ->
                            OnlineUser(
                                userId = id,
                                userName = userManager.getDisplayName(id),
                                isOnline = isOnline
                            )
                        }
                        _uiState.update { it.copy(onlineUsers = onlineUsers) }
                    }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                realtimeManager.unsubscribeFromList(listId)
            } catch (e: Exception) { }
        }
    }

    // Search suggestions based on input
    fun searchSuggestions(query: String) {
        if (query.length < 2) {
            _uiState.update { it.copy(suggestedItems = emptyList()) }
            return
        }

        viewModelScope.launch {
            frequentItemRepository.searchFrequentItems(query, 5)
                .catch { /* Ignore errors */ }
                .collect { suggestions ->
                    _uiState.update { it.copy(suggestedItems = suggestions) }
                }
        }
    }

    fun clearSuggestions() {
        _uiState.update { it.copy(suggestedItems = emptyList()) }
    }

    // Toggle reorder mode
    fun toggleReorderMode() {
        _uiState.update { it.copy(isReorderMode = !it.isReorderMode) }
    }

    // Reorder items
    fun reorderItems(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                val currentItems = _uiState.value.uncheckedItems.toMutableList()
                if (fromIndex < 0 || fromIndex >= currentItems.size ||
                    toIndex < 0 || toIndex >= currentItems.size) {
                    return@launch
                }

                // Move item in list
                val movedItem = currentItems.removeAt(fromIndex)
                currentItems.add(toIndex, movedItem)

                // Update positions
                currentItems.forEachIndexed { index, item ->
                    if (item.position != index) {
                        repository.updateItemPosition(item.id, index)
                    }
                }

                // Optimistic update
                _uiState.update { it.copy(uncheckedItems = currentItems) }
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
                val detectedCategory = ProductCategory.detectCategory(name)
                val item = ListItem(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    category = category ?: detectedCategory.label,
                    emoji = detectedCategory.emoji,
                    position = _uiState.value.items.size,
                    addedBy = userManager.currentUserId.ifEmpty { null }
                )
                repository.addItem(item)

                // Track frequent item usage
                frequentItemRepository.trackItemUsage(
                    name = name,
                    category = item.category,
                    emoji = item.emoji,
                    unit = unit
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Add item from suggestion
    fun addItemFromSuggestion(suggestion: FrequentItemEntity) {
        addItem(
            name = suggestion.name,
            quantity = 1.0,
            unit = suggestion.defaultUnit,
            category = suggestion.category
        )
        clearSuggestions()
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

    fun addNote(content: String) {
        viewModelScope.launch {
            try {
                val note = Note(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    itemId = null,
                    userId = userManager.currentUserId.ifEmpty { "anonymous" },
                    userName = userManager.currentUserName,
                    content = content
                )
                noteRepository.addNote(note)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addNote(content: String, userId: String, userName: String) {
        viewModelScope.launch {
            try {
                val note = Note(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    itemId = null,
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
