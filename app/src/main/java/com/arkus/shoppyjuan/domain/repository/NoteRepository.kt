package com.arkus.shoppyjuan.domain.repository

import com.arkus.shoppyjuan.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getListNotes(listId: String): Flow<List<Note>>
    fun getItemNotes(itemId: String): Flow<List<Note>>
    fun getAllNotesForList(listId: String): Flow<List<Note>>
    suspend fun getNoteById(noteId: String): Note?
    suspend fun addNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(noteId: String)
    suspend fun deleteNotesByListId(listId: String)
    suspend fun deleteNotesByItemId(itemId: String)
    suspend fun getListNoteCount(listId: String): Int
    suspend fun getItemNoteCount(itemId: String): Int
}
