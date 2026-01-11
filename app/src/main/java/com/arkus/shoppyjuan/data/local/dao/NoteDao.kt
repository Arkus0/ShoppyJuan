package com.arkus.shoppyjuan.data.local.dao

import androidx.room.*
import com.arkus.shoppyjuan.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE listId = :listId AND itemId IS NULL ORDER BY createdAt DESC")
    fun getListNotes(listId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE itemId = :itemId ORDER BY createdAt DESC")
    fun getItemNotes(itemId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE listId = :listId ORDER BY createdAt DESC")
    fun getAllNotesForList(listId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: String)

    @Query("DELETE FROM notes WHERE listId = :listId")
    suspend fun deleteNotesByListId(listId: String)

    @Query("DELETE FROM notes WHERE itemId = :itemId")
    suspend fun deleteNotesByItemId(itemId: String)

    @Query("SELECT COUNT(*) FROM notes WHERE listId = :listId AND itemId IS NULL")
    suspend fun getListNoteCount(listId: String): Int

    @Query("SELECT COUNT(*) FROM notes WHERE itemId = :itemId")
    suspend fun getItemNoteCount(itemId: String): Int
}
