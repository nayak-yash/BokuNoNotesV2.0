package com.bokuno.notes

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query.Direction
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener,INoteAdapter {



    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var factor : String
    private var order : Int =0
    private lateinit var mAdapter:NoteAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var mNoteDao: NoteDao
    private lateinit var noteList: ArrayList<Note>
    private companion object{
        private const val REQUEST_CODE: Int=101
        private const val TAG="Mainxy"
    }
    private val pdfGenerator= PDFGenerator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mNoteDao= NoteDao()
        binding.btnAdd.setOnClickListener {
            val cIntent=Intent(this,CreateNoteActivity::class.java)
            startActivity(cIntent)
        }
        noteList=ArrayList()
        sharedPref= getSharedPreferences("SortPref",Context.MODE_PRIVATE)
        editor=sharedPref.edit()
        factor = sharedPref.getString("factor","createdAt")!!
        order = sharedPref.getInt("order", 1)

        binding.btnSortFactor.setOnClickListener{
            showPopup(binding.btnSortFactor)
        }

        binding.btnSortOrder.setImageResource(if (order ==1) R.drawable.ic_downward else R.drawable.ic_upward)

        binding.btnSortOrder.setOnClickListener{
            if(order==1) {
                binding.btnSortOrder.setImageResource(R.drawable.ic_upward)
                editor.apply {
                    putInt("order", 0)
                    apply()
                }
            }
            else{
                binding.btnSortOrder.setImageResource(R.drawable.ic_downward)
                editor.apply {
                    putInt("order", 1)
                    apply()
                }
            }
            setUpRecyclerView()
        }
        binding.searchBar.setOnQueryTextListener(this)
    }

    private fun showPopup(view : View) {
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.sort_popup_menu)
        popup.show()
        popup.setOnMenuItemClickListener {
            when (it!!.itemId) {
                R.id.btnDateCreated -> {
                    editor.apply{
                    putString("factor","createdAt")
                    apply()
                }
                }
                R.id.btnTitle -> {
                    editor.apply{
                    putString("factor","title")
                    apply()
                }
                }
            }
            setUpRecyclerView()
            true

        }
    }

    override fun onStart() {
        super.onStart()
        setUpRecyclerView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==R.id.btnLogout){
            Firebase.auth.signOut()
            val logoutIntent= Intent(this,LoginActivity::class.java)
            logoutIntent.flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity((logoutIntent))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpRecyclerView() {
        factor = sharedPref.getString("factor","createdAt")!!
        order = sharedPref.getInt("order", 1)
        mAdapter = NoteAdapter(noteList, this)
        binding.recyclerView.adapter = mAdapter
        binding.recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
        mNoteDao.noteCollection
            .whereEqualTo("userId", mNoteDao.mAuth.currentUser?.uid)
            .orderBy(factor,if(order==1) Direction.DESCENDING else Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->

                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                snapshots?.let {
                    for (dc in it!!.documentChanges) {
                        val note=dc.document.toObject<Note>()
                        Log.i(TAG,"${note.title}")
                        if(dc.type==DocumentChange.Type.ADDED) {
                            if(noteList.contains(note)){
                                noteList.remove(note)
                            }
                            noteList.add(note)
                        }
                        else if(dc.type==DocumentChange.Type.REMOVED){
                                noteList.remove(note)
                        }
                        else if(dc.type==DocumentChange.Type.MODIFIED){
                            noteList.set(dc.oldIndex,note)
                    }
                        mAdapter.notifyDataSetChanged()
                    }
                }
            }
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }
// for searching item within noteList
    override fun onQueryTextChange(search: String?): Boolean {
        noteList.clear()
        factor = sharedPref.getString("factor","createdAt")!!
        order = sharedPref.getInt("order", 1)
        mNoteDao.noteCollection
            .whereEqualTo("userId",mNoteDao.mAuth.currentUser?.uid)
            .orderBy(factor,if(order==1) Direction.DESCENDING else Direction.ASCENDING)
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

// open in long click
    private fun showBottomDialog(item: Note) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_popup)
        val btnDelete=dialog.findViewById<LinearLayout>(R.id.btnDelete)
        val btnShare=dialog.findViewById<LinearLayout>(R.id.btnShare)
        val btnSave=dialog.findViewById<LinearLayout>(R.id.btnSave)
//        val btnHide=dialog.findViewById<LinearLayout>(R.id.btnHide)
        val btnFavorite=dialog.findViewById<LinearLayout>(R.id.btnFavorite)
        val btnStatus=dialog.findViewById<LinearLayout>(R.id.btnStatus)
        val tvStatus=btnStatus.findViewById<TextView>(R.id.tvStatus)
        val tvFavorite=btnFavorite.findViewById<TextView>(R.id.tvFavorite)
//        val tvHide=btnHide.findViewById<TextView>(R.id.tvHide)
        if(item.status ==false){
            tvStatus.text="Mark as Done"
        }
        else if(item.status==true){
            btnStatus.visibility=View.GONE
        }

        if(item.isFavorite == false ){
            tvFavorite.text="Mark as favorite"
        }
        else{
            tvFavorite.text="Mark as unfavorite"
        }

//        if(item.isPrivate == true){
//            tvHide.text="Unhide"
//        }
//        else{
//            tvHide.text="Hide"
//        }
        btnDelete.setOnClickListener{
            deleteItem(item)
            dialog.dismiss()
        }
        btnShare.setOnClickListener{
            shareFileLink(item)
            dialog.dismiss()
        }
        btnSave.setOnClickListener{
            saveAsFile(item)
            dialog.dismiss()
        }
        btnStatus.setOnClickListener{
            mNoteDao.editNote(item, 1)    // 1 for changing status
            dialog.dismiss()
        }
//        btnHide.setOnClickListener{
//            mNoteDao.editNote(item, 2)    // 2 for hiding/unhiding note
//            dialog.dismiss()
//        }
        btnFavorite.setOnClickListener{
            mNoteDao.editNote(item, 3)    // 3 for marking note as favorite/unfavorite
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(TRANSPARENT))
        dialog.window?.attributes?.windowAnimations=R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun isOnline(context: Context): Boolean {
        if (context == null) return false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }

    private fun shareFileLink(note: Note) {
        if (isOnline(this)) {
            if (ContextCompat.checkSelfPermission(
                    getApplicationContext(),
                    READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    getApplicationContext(),
                    WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                pdfGenerator.flag="SHARE"
                pdfGenerator.createPdf(this, note, this)
                val uri = Uri.fromFile(pdfGenerator.file)
                val storage = FirebaseStorage.getInstance()
                val pdfRef = storage.reference.child("pdf/${uri.lastPathSegment}")
                pdfRef.putFile(uri).addOnSuccessListener {
                    it.task.continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let {
                                Toast.makeText(this, "Couldn't share", Toast.LENGTH_SHORT).show()
                                throw it
                            }
                        }
                        pdfRef.downloadUrl
                    }.addOnCompleteListener {
                        if (it.isSuccessful) {
                            val link = it.result.toString()
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "text/plain"
                            shareIntent.putExtra(Intent.EXTRA_TEXT, link)
                            shareIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                            startActivity(shareIntent)
                        } else {
                            Toast.makeText(this, "Couldn't share", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE
                )
            }
        }
        else{
            Toast.makeText(this, "Enable network", Toast.LENGTH_SHORT).show()
        }
    }


    @SuppressLint("SimpleDateFormat")
    private fun saveAsFile(item: Note) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            pdfGenerator.flag="SAVE"
            pdfGenerator.createPdf(this,item,this)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
        }
    }

    private fun deleteItem(item: Note) {
        mNoteDao.deleteNote(item)
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
        val noteViewIntent=Intent(this,NoteViewActivity::class.java)
        noteViewIntent.putExtra("title",item.title)
        noteViewIntent.putExtra("note",item.text)
        noteViewIntent.putExtra("createdAt",item.createdAt)
        noteViewIntent.putExtra("location",item.location)
        startActivity(noteViewIntent)
    }

    override fun onLongItemClicked(item: Note) {
        showBottomDialog(item)
    }
}

