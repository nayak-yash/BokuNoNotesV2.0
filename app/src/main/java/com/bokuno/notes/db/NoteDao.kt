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

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes ORDER BY " +
            "CASE WHEN :isAsc = 0 THEN title END ASC, " +
            "CASE WHEN :isAsc = 1 THEN title END DESC ")
    fun getNotesSortedByTitle(isAsc : Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes ORDER BY " +
            "CASE WHEN :isAsc = 0 THEN createdAt END ASC, " +
            "CASE WHEN :isAsc = 1 THEN createdAt END DESC ")
    fun getNotesSortedByDate(isAsc : Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE :search OR text LIKE :search ")
    fun getSearchNotes(search: String): LiveData<List<Note>>

}