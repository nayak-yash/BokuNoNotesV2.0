package com.bokuno.notes.ui.fragments.notes

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.bokuno.notes.models.Note
import com.bokuno.notes.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.bokuno.notes.models.HelpRequest
import com.bokuno.notes.models.HelpResponse
import com.bokuno.notes.utils.Constants.Companion.API_KEY
import com.bokuno.notes.utils.InternetConnection
import com.bokuno.notes.utils.NetworkResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject

@HiltViewModel
class NotesViewModel @Inject constructor(private val notesRepo: NotesRepository,
                                         application: Application
) : AndroidViewModel(application){


    private val context = getApplication<Application>().applicationContext

    private val _statusLiveData = MutableLiveData<NetworkResult<HelpResponse>>()
    val statusLiveData: LiveData<NetworkResult<HelpResponse>>
        get() = _statusLiveData


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

    fun sendMessage(helpRequest: HelpRequest) {
        if(!InternetConnection.checkForInternet(context)){
            _statusLiveData.postValue(NetworkResult.Error("No internet connection"))
        }
        else{
            _statusLiveData.postValue(NetworkResult.Loading())
            val contentType = "application/json"
            val authorization = "Bearer $API_KEY"
            val requestBody = RequestBody.create(MediaType.parse(contentType),
            Gson().toJson(
                helpRequest
            ))
            viewModelScope.launch {
                val response = notesRepo.getPrompt(contentType,authorization,requestBody)
                if (response.isSuccessful && response.body() != null) {
                    _statusLiveData.postValue(NetworkResult.Success(response.body()!!))
                } else if (response.errorBody() != null) {
                    val errorObj = JSONObject(response.errorBody()!!.charStream().readText())
                    _statusLiveData.postValue(NetworkResult.Error(errorObj.getString("message")))
                } else {
                    _statusLiveData.postValue(NetworkResult.Error("Something Went Wrong"))
                }
            }
        }
    }
}