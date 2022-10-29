package com.bokuno.notes

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Color.TRANSPARENT
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bokuno.notes.daos.NoteDao
import com.bokuno.notes.databinding.ActivityMainBinding
import com.bokuno.notes.models.Note
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener,INoteAdapter {


    private lateinit var mAdapter:NoteAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var mNoteDao: NoteDao
    private lateinit var noteList: ArrayList<Note>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mNoteDao= NoteDao()
        binding.btnAdd.setOnClickListener {
            val cIntent=Intent(this,CreateNoteActivity::class.java)
            startActivity(cIntent)
        }
        binding.searchBar.setOnQueryTextListener(this)
    }

    override fun onStart() {
        super.onStart()
        setUpRecyclerView()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setUpRecyclerView() {
        noteList=ArrayList<Note>()
        mAdapter= NoteAdapter(noteList,this)
        binding.recyclerView.adapter=mAdapter
        binding.recyclerView.layoutManager=StaggeredGridLayoutManager(2,LinearLayoutManager.VERTICAL)

        mNoteDao.noteCollection
            .whereEqualTo("userId",mNoteDao.mAuth.currentUser?.uid)
            .orderBy("createdAt",Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                snapshots?.let{
                    for (dc in snapshots!!.documentChanges) {
                        val note=dc.document.toObject<Note>()
                        if (dc.type == DocumentChange.Type.ADDED) {
                            noteList.add(note)
                        }
                        if(dc.type == DocumentChange.Type.REMOVED) {
                            noteList.remove(note)
                        }
                        mAdapter.notifyDataSetChanged()
                    }
                }
            }

        }


    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(search: String?): Boolean {
        noteList.clear()
        mNoteDao.noteCollection
            .whereEqualTo("userId",mNoteDao.mAuth.currentUser?.uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    val note=dc.document.toObject<Note>()
                    if(note.title!!.contains(search!!))
                        noteList.add(note)
                }
                mAdapter.notifyDataSetChanged()
            }
        return true
    }

    private companion object{
        private val REQUEST_CODE: Int=101
        private const val TAG="Mainxy"
    }
    private fun showBottomDialog(item: Note) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_popup)
        val btnDelete=dialog.findViewById<LinearLayout>(R.id.btnDelete)
        val btnShare=dialog.findViewById<LinearLayout>(R.id.btnShare)
        val btnSave=dialog.findViewById<LinearLayout>(R.id.btnSave)

        btnDelete.setOnClickListener{
            deleteItem(item)
            dialog.hide()
        }
        btnShare.setOnClickListener{
            dialog.hide()
        }
        btnSave.setOnClickListener{
            saveAsFile(item)
            dialog.hide()
        }
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(TRANSPARENT))
        dialog.window?.attributes?.windowAnimations=R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun deleteItem(item: Note) {
        mNoteDao.deleteNote(item)
    }

    @SuppressLint("SimpleDateFormat")
    private fun saveAsFile(item: Note) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            val pdfGenerator = PDFGenerator()
            pdfGenerator.createPdf(this,item,this)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.size > 0) {

                val writeStorage = grantResults[0] === PackageManager.PERMISSION_GRANTED
                val readStorage = grantResults[1] === PackageManager.PERMISSION_GRANTED
                if (writeStorage && readStorage) {
                    Toast.makeText(this, "Permission Granted..", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission Denied.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }


    override fun onItemClicked(item: Note) {

    }

    override fun onLongItemClicked(item: Note) {
        showBottomDialog(item)
    }
}

