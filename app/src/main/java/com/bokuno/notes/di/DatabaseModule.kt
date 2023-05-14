package com.bokuno.notes.di

import android.content.Context
import androidx.room.Room
import com.bokuno.notes.db.NotesDB
import com.bokuno.notes.repository.NotesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideNotesDatabase(@ApplicationContext context: Context): NotesDB {
        return Room.databaseBuilder(
            context,
            NotesDB::class.java,
            "notes_db.db"
        ).build()
    }
}