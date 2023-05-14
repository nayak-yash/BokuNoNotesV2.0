package com.bokuno.notes.repository

import androidx.lifecycle.LiveData
import com.bokuno.notes.db.NotesDB
import com.bokuno.notes.models.Note
import javax.inject.Inject

class NotesRepository @Inject constructor(private val db: NotesDB) {

    suspend fun addNote(note: Note) = db.getNoteDao().addNote(note)

    suspend fun updateNote(note: Note) = db.getNoteDao().updateNote(note)

    suspend fun deleteNote(note: Note) = db.getNoteDao().deleteNote(note)

    fun getAllNotes(): LiveData<MutableList<Note>> = db.getNoteDao().getAllNotes()

}