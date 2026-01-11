package com.arkus.shoppyjuan.domain.repository

import com.arkus.shoppyjuan.domain.model.FavoriteItem
import com.arkus.shoppyjuan.domain.model.ListItem
import com.arkus.shoppyjuan.domain.model.ShoppingList
import com.arkus.shoppyjuan.domain.util.ListTemplate
import kotlinx.coroutines.flow.Flow

interface ShoppingListRepository {
    fun getAllLists(): Flow<List<ShoppingList>>
    fun getArchivedLists(): Flow<List<ShoppingList>>
    fun getListById(listId: String): Flow<ShoppingList?>
    suspend fun createList(name: String, ownerId: String): ShoppingList
    suspend fun createListFromTemplate(template: ListTemplate, ownerId: String): ShoppingList
    suspend fun updateList(list: ShoppingList)
    suspend fun deleteList(listId: String)
    suspend fun archiveList(listId: String, isArchived: Boolean)
    suspend fun duplicateList(listId: String): ShoppingList
    suspend fun joinListByCode(code: String, userId: String): ShoppingList?

    fun getItemsByListId(listId: String): Flow<List<ListItem>>
    fun getUncheckedItems(listId: String): Flow<List<ListItem>>
    fun getCheckedItems(listId: String): Flow<List<ListItem>>
    suspend fun addItem(item: ListItem)
    suspend fun updateItem(item: ListItem)
    suspend fun deleteItem(itemId: String)
    suspend fun toggleItemChecked(itemId: String, checked: Boolean)
    suspend fun updateItemNote(itemId: String, note: String?)
    suspend fun deleteCheckedItems(listId: String)
    suspend fun getItemCount(listId: String): Int
    suspend fun getUncheckedItemCount(listId: String): Int
    suspend fun updateItemPosition(itemId: String, position: Int)

    // Favorites
    fun getFavoriteItems(): Flow<List<FavoriteItem>>
    suspend fun addFavoriteItem(favorite: FavoriteItem)
    suspend fun deleteFavoriteItem(favoriteId: String)
}
