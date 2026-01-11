package com.arkus.shoppyjuan.data.repository

import com.arkus.shoppyjuan.data.local.dao.ListItemDao
import com.arkus.shoppyjuan.data.local.dao.ShoppingListDao
import com.arkus.shoppyjuan.domain.model.*
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ShoppingListRepositoryImpl @Inject constructor(
    private val shoppingListDao: ShoppingListDao,
    private val listItemDao: ListItemDao
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

    override suspend fun deleteCheckedItems(listId: String) {
        listItemDao.deleteCheckedItems(listId)
    }

    override suspend fun getItemCount(listId: String): Int {
        return listItemDao.getItemCount(listId)
    }

    override suspend fun getUncheckedItemCount(listId: String): Int {
        return listItemDao.getUncheckedItemCount(listId)
    }

    private fun generateShareCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
