package com.bokuno.notes.daos

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
        GlobalScope.launch {
            val createdAt = System.currentTimeMillis()
            val note = Note(title, note, createdAt, mAuth.currentUser?.uid.toString(), location)
            noteCollection.document().set(note)
        }
    }

    fun deleteNote(note: Note) {
        GlobalScope.launch(Dispatchers.IO) {
            val query = noteCollection
                .whereEqualTo("userId", mAuth.currentUser?.uid)
                .whereEqualTo("createdAt", note.createdAt)
                .get().await()
            if (query.documents.isNotEmpty()) {
                for(document in query){
                    try{
                        noteCollection.document(document.id).delete().await()
                    }
                    catch(e : Exception){
                        kotlinx.coroutines.withContext(Dispatchers.Main){
                            Toast.makeText(null,e.message,Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}