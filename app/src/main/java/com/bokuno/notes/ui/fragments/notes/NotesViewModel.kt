package com.bokuno.notes.ui.fragments.notes

import com.bokuno.notes.models.Note
import com.bokuno.notes.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class NotesViewModel @Inject constructor(private val notesRepo: NotesRepository) : ViewModel(){

    fun addNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            notesRepo.addNote(note)
        }
    }

    fun getAllNotes() = notesRepo.getAllNotes()

    fun updateNote(note: Note){
        viewModelScope.launch(Dispatchers.IO) {
            notesRepo.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            notesRepo.deleteNote(note)
        }
    }

}