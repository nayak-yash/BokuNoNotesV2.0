package com.bokuno.notes.daos

import android.util.Log
import android.widget.Toast
import com.bokuno.notes.models.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NoteDao {
    val mDB = FirebaseFirestore.getInstance()
    val noteCollection = mDB.collection("notes")
    val mAuth = FirebaseAuth.getInstance()
    fun addNote(title: String, note: String, location: String?) {
        GlobalScope.launch(Dispatchers.IO) {
            val createdAt = System.currentTimeMillis()
            val noteDocument = noteCollection.document()
            val note = Note(
                title,
                note,
                createdAt,
                mAuth.currentUser?.uid.toString(),
                noteDocument.id,
                location
            )
            noteDocument.set(note).await()
        }
    }

    fun deleteNote(note: Note) {
        GlobalScope.launch(Dispatchers.IO) {
            try {

                val noteDocument = noteCollection.document(note.noteId!!)
                noteDocument.delete().await()
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    Toast.makeText(null, e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun editNote(note: Note, REQUEST_CODE: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            val document = noteCollection.document(note.noteId!!)
            try {
                if (REQUEST_CODE == 1) {
                    document.update("status", if (note.status == null) false else true).await()
                }
                else if(REQUEST_CODE == 2) {
                            document.update("isPrivate", if(note.isPrivate) false else true).await()
                }
                else if (REQUEST_CODE == 3) {
                    document.update("isFavorite", if (note.isFavorite) false else true).await()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    Toast.makeText(null, e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}