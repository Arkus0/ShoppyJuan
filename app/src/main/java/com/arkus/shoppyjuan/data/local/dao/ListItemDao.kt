package com.arkus.shoppyjuan.data.local.dao

import androidx.room.*
import com.arkus.shoppyjuan.data.local.entity.ListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListItemDao {
    @Query("SELECT * FROM list_items WHERE listId = :listId ORDER BY position ASC, createdAt ASC")
    fun getItemsByListId(listId: String): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM list_items WHERE listId = :listId AND checked = 0 ORDER BY position ASC")
    fun getUncheckedItems(listId: String): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM list_items WHERE listId = :listId AND checked = 1 ORDER BY position ASC")
    fun getCheckedItems(listId: String): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM list_items WHERE id = :itemId")
    suspend fun getItemById(itemId: String): ListItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ListItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ListItemEntity>)

    @Update
    suspend fun updateItem(item: ListItemEntity)

    @Delete
    suspend fun deleteItem(item: ListItemEntity)

    @Query("DELETE FROM list_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("DELETE FROM list_items WHERE listId = :listId")
    suspend fun deleteItemsByListId(listId: String)

    @Query("DELETE FROM list_items WHERE listId = :listId AND checked = 1")
    suspend fun deleteCheckedItems(listId: String)

    @Query("UPDATE list_items SET checked = :checked WHERE id = :itemId")
    suspend fun setItemChecked(itemId: String, checked: Boolean)

    @Query("UPDATE list_items SET position = :position WHERE id = :itemId")
    suspend fun updateItemPosition(itemId: String, position: Int)

    @Query("SELECT COUNT(*) FROM list_items WHERE listId = :listId")
    suspend fun getItemCount(listId: String): Int

    @Query("SELECT COUNT(*) FROM list_items WHERE listId = :listId AND checked = 0")
    suspend fun getUncheckedItemCount(listId: String): Int

    @Query("UPDATE list_items SET note = :note, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemNote(itemId: String, note: String?, updatedAt: Long)
}
