package com.arkus.shoppyjuan.data.repository

import com.arkus.shoppyjuan.data.local.dao.FavoriteItemDao
import com.arkus.shoppyjuan.data.local.dao.ListItemDao
import com.arkus.shoppyjuan.data.local.dao.ShoppingListDao
import com.arkus.shoppyjuan.domain.model.*
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import com.arkus.shoppyjuan.domain.util.ListTemplate
import com.arkus.shoppyjuan.domain.util.ProductCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ShoppingListRepositoryImpl @Inject constructor(
    private val shoppingListDao: ShoppingListDao,
    private val listItemDao: ListItemDao,
    private val favoriteItemDao: FavoriteItemDao
) : ShoppingListRepository {

    override fun getAllLists(): Flow<List<ShoppingList>> {
        return shoppingListDao.getAllLists().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getArchivedLists(): Flow<List<ShoppingList>> {
        return shoppingListDao.getArchivedLists().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getListById(listId: String): Flow<ShoppingList?> {
        return shoppingListDao.getListById(listId).map { it?.toDomain() }
    }

    override suspend fun createList(name: String, ownerId: String): ShoppingList {
        val list = ShoppingList(
            id = UUID.randomUUID().toString(),
            name = name,
            ownerId = ownerId,
            shareCode = generateShareCode()
        )
        shoppingListDao.insertList(list.toEntity())
        return list
    }

    override suspend fun createListFromTemplate(template: ListTemplate, ownerId: String): ShoppingList {
        val list = createList(template.name, ownerId)

        // Add template items to the list
        template.items.forEach { templateItem ->
            val category = ProductCategory.detectCategory(templateItem.name)
            val item = ListItem(
                id = UUID.randomUUID().toString(),
                listId = list.id,
                name = templateItem.name,
                quantity = templateItem.quantity,
                unit = templateItem.unit,
                category = category.label,
                emoji = category.emoji
            )
            listItemDao.insertItem(item.toEntity())
        }

        return list
    }

    override suspend fun duplicateList(listId: String): ShoppingList {
        val originalList = shoppingListDao.getListById(listId).first()?.toDomain()
            ?: throw IllegalArgumentException("List not found")

        val newList = createList("${originalList.name} (Copia)", originalList.ownerId)

        // Copy all items
        val items = listItemDao.getItemsByListId(listId).first()
        items.forEach { itemEntity ->
            val item = itemEntity.toDomain().copy(
                id = UUID.randomUUID().toString(),
                listId = newList.id,
                checked = false
            )
            listItemDao.insertItem(item.toEntity())
        }

        return newList
    }

    override suspend fun joinListByCode(code: String, userId: String): ShoppingList? {
        // Find list by share code
        val lists = shoppingListDao.getAllLists().first()
        val list = lists.find { it.shareCode == code }?.toDomain()

        // TODO: Add user to list collaborators in Supabase

        return list
    }

    override suspend fun updateList(list: ShoppingList) {
        shoppingListDao.updateList(list.toEntity())
    }

    override suspend fun deleteList(listId: String) {
        shoppingListDao.deleteListById(listId)
    }

    override suspend fun archiveList(listId: String, isArchived: Boolean) {
        shoppingListDao.setArchiveStatus(listId, isArchived)
    }

    override fun getItemsByListId(listId: String): Flow<List<ListItem>> {
        return listItemDao.getItemsByListId(listId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getUncheckedItems(listId: String): Flow<List<ListItem>> {
        return listItemDao.getUncheckedItems(listId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCheckedItems(listId: String): Flow<List<ListItem>> {
        return listItemDao.getCheckedItems(listId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addItem(item: ListItem) {
        listItemDao.insertItem(item.toEntity())
    }

    override suspend fun updateItem(item: ListItem) {
        listItemDao.updateItem(item.toEntity())
    }

    override suspend fun deleteItem(itemId: String) {
        listItemDao.deleteItemById(itemId)
    }

    override suspend fun toggleItemChecked(itemId: String, checked: Boolean) {
        listItemDao.setItemChecked(itemId, checked)
    }

    override suspend fun updateItemNote(itemId: String, note: String?) {
        listItemDao.updateItemNote(itemId, note, System.currentTimeMillis())
    }

    override suspend fun deleteCheckedItems(listId: String) {
        listItemDao.deleteCheckedItems(listId)
    }

    override suspend fun getItemCount(listId: String): Int {
        return listItemDao.getItemCount(listId)
    }

    override suspend fun getUncheckedItemCount(listId: String): Int {
        return listItemDao.getUncheckedItemCount(listId)
    }

    override suspend fun updateItemPosition(itemId: String, position: Int) {
        listItemDao.updateItemPosition(itemId, position)
    }

    // Favorites
    override fun getFavoriteItems(): Flow<List<FavoriteItem>> {
        return favoriteItemDao.getAllFavorites().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addFavoriteItem(favorite: FavoriteItem) {
        favoriteItemDao.insertFavorite(favorite.toEntity())
    }

    override suspend fun deleteFavoriteItem(favoriteId: String) {
        favoriteItemDao.deleteFavoriteById(favoriteId)
    }

    private fun generateShareCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
