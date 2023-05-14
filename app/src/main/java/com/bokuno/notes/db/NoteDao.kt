package com.bokuno.notes.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.bokuno.notes.models.Note

@Dao
interface NoteDao{

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Query("SELECT * FROM notes")
    fun getAllNotes(): LiveData<MutableList<Note>>

    @Delete
    suspend fun deleteNote(note: Note)

}