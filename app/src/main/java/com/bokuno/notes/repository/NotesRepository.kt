package com.bokuno.notes.repository

import androidx.lifecycle.LiveData
import com.bokuno.notes.api.API
import com.bokuno.notes.db.NotesDB
import com.bokuno.notes.models.HelpRequest
import com.bokuno.notes.models.HelpResponse
import com.bokuno.notes.models.Note
import okhttp3.RequestBody
import javax.inject.Inject

class NotesRepository @Inject constructor(private val db: NotesDB, private val api: API) {

    suspend fun addNote(note: Note) = db.getNoteDao().addNote(note)

    suspend fun updateNote(note: Note) = db.getNoteDao().updateNote(note)

    suspend fun deleteNote(note: Note) = db.getNoteDao().deleteNote(note)

    fun getNotesSortedByTitle(isAsc: Int): LiveData<List<Note>> = db.getNoteDao().getNotesSortedByTitle(isAsc)

    fun getNotesSortedByDate(isAsc: Int): LiveData<List<Note>> = db.getNoteDao().getNotesSortedByDate(isAsc)

    fun getSearchNotes(query: String): LiveData<List<Note>> = db.getNoteDao().getSearchNotes(query)

    suspend fun getPrompt(contentType: String, authorization: String, requestBody: RequestBody): HelpResponse = api.getPrompt(contentType,authorization,requestBody)
}