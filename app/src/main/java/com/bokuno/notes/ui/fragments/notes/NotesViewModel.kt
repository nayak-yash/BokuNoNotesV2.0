package com.bokuno.notes.ui.fragments.notes

import android.app.Application
import androidx.lifecycle.*
import com.bokuno.notes.models.Note
import com.bokuno.notes.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.bokuno.notes.models.HelpRequest
import com.bokuno.notes.utils.Constants.Companion.API_KEY
import com.bokuno.notes.utils.InternetConnection
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody

@HiltViewModel
class NotesViewModel @Inject constructor(private val notesRepo: NotesRepository,
                                         application: Application
) : AndroidViewModel(application){

    private val context = getApplication<Application>().applicationContext

    private val _response = MutableLiveData<String>()
    val response: LiveData<String>
        get() = _response

    fun addNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            notesRepo.addNote(note)
        }
    }

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

    fun getNotesSortedByTitle(isAsc: Int) = notesRepo.getNotesSortedByTitle(isAsc)


    fun getNotesSortedByDate(isAsc: Int) = notesRepo.getNotesSortedByDate(isAsc)


    fun getSearchNotes(query: String) = notesRepo.getSearchNotes(query)

    fun sendMessage(helpRequest: HelpRequest): Boolean {
        if(!InternetConnection.checkForInternet(context)){
            return false
        }
        val contentType = "application/json"
        val authorization = "Bearer $API_KEY"
        val requestBody = RequestBody.create(MediaType.parse(contentType),
        Gson().toJson(
            helpRequest
        ))
        viewModelScope.launch {
            val response = notesRepo.getPrompt(contentType,authorization,requestBody)
            _response.value = response.choices.first().text
        }
        return true
    }

}