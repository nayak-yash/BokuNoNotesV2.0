package com.bokuno.notes.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bokuno.notes.models.Note

@Database(
    entities = [Note::class],
    version = 1
)

abstract class NotesDB :RoomDatabase(){

    abstract fun getNoteDao(): NoteDao

}