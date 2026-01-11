package com.arkus.shoppyjuan.data.local.dao

import androidx.room.*
import com.arkus.shoppyjuan.data.local.entity.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    fun getListById(listId: String): Flow<ShoppingListEntity?>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    suspend fun getListByIdOnce(listId: String): ShoppingListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ShoppingListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLists(lists: List<ShoppingListEntity>)

    @Update
    suspend fun updateList(list: ShoppingListEntity)

    @Delete
    suspend fun deleteList(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteListById(listId: String)

    @Query("UPDATE shopping_lists SET isArchived = :isArchived WHERE id = :listId")
    suspend fun setArchiveStatus(listId: String, isArchived: Boolean)

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAll()

    // Recurrence queries
    @Query("SELECT * FROM shopping_lists WHERE isRecurring = 1 AND nextRecurrenceAt IS NOT NULL")
    fun getRecurringLists(): Flow<List<ShoppingListEntity>>

    @Query("""
        UPDATE shopping_lists
        SET isRecurring = :isRecurring,
            recurrenceSettingsJson = :settingsJson,
            nextRecurrenceAt = :nextRecurrenceAt
        WHERE id = :listId
    """)
    suspend fun setRecurrenceSettings(
        listId: String,
        isRecurring: Boolean,
        settingsJson: String?,
        nextRecurrenceAt: Long?
    )

    @Query("""
        UPDATE shopping_lists
        SET nextRecurrenceAt = :nextRecurrenceAt,
            lastResetAt = :lastResetAt
        WHERE id = :listId
    """)
    suspend fun updateRecurrence(
        listId: String,
        nextRecurrenceAt: Long,
        lastResetAt: Long
    )
}
