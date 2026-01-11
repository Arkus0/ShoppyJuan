package com.arkus.shoppyjuan.data.repository

import com.arkus.shoppyjuan.data.local.dao.NoteDao
import com.arkus.shoppyjuan.domain.model.Note
import com.arkus.shoppyjuan.domain.model.toDomain
import com.arkus.shoppyjuan.domain.model.toEntity
import com.arkus.shoppyjuan.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getListNotes(listId: String): Flow<List<Note>> {
        return noteDao.getListNotes(listId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getItemNotes(itemId: String): Flow<List<Note>> {
        return noteDao.getItemNotes(itemId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllNotesForList(listId: String): Flow<List<Note>> {
        return noteDao.getAllNotesForList(listId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(noteId: String): Note? {
        return noteDao.getNoteById(noteId)?.toDomain()
    }

    override suspend fun addNote(note: Note) {
        noteDao.insertNote(note.toEntity())
    }

    override suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.toEntity())
    }

    override suspend fun deleteNote(noteId: String) {
        noteDao.deleteNoteById(noteId)
    }

    override suspend fun deleteNotesByListId(listId: String) {
        noteDao.deleteNotesByListId(listId)
    }

    override suspend fun deleteNotesByItemId(itemId: String) {
        noteDao.deleteNotesByItemId(itemId)
    }

    override suspend fun getListNoteCount(listId: String): Int {
        return noteDao.getListNoteCount(listId)
    }

    override suspend fun getItemNoteCount(itemId: String): Int {
        return noteDao.getItemNoteCount(itemId)
    }
}
