package com.bokuno.notes.ui.fragments.notes

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import androidx.lifecycle.Observer
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bokuno.notes.NoteAdapter
import com.bokuno.notes.PDFGenerator
import com.bokuno.notes.R
import com.bokuno.notes.databinding.FragmentMainBinding
import com.bokuno.notes.models.Note
import com.bokuno.notes.utils.Constants.Companion.SEARCH_NEWS_TIME_DELAY
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

@AndroidEntryPoint
class MainFragment : Fragment() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var factor : String
    private var order : Int =0
    private lateinit var mAdapter: NoteAdapter
    private lateinit var tempNote :Note
    private lateinit var noteList: MutableList<Note>
    private lateinit var searchList: MutableList<Note>
    private companion object{
        private const val REQUEST_CODE: Int=101
        private const val TAG="Mainxy"
        private const val VIEW_PRIVATE = 202
        private const val UNHIDE = 303
    }
    private val pdfGenerator= PDFGenerator()
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val noteViewModel by viewModels<NotesViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        binding.btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_noteFragment)
        }
        sharedPref= activity!!.getSharedPreferences("SortPref", Context.MODE_PRIVATE)
        editor=sharedPref.edit()
        factor = sharedPref.getString("factor","createdAt")!!   // sorting factor can be creation time or title
        order = sharedPref.getInt("order", 1)   // 1 for descending and 0 for ascending

        binding.btnSortFactor.setOnClickListener{
            showPopup(binding.btnSortFactor)
        }

        binding.btnSortOrder.setImageResource(if (order ==1) R.drawable.ic_downward else R.drawable.ic_upward)

        binding.btnSortOrder.setOnClickListener{
            if(order==1) {
                order = 0
                binding.btnSortOrder.setImageResource(R.drawable.ic_upward)
                editor.apply {
                    putInt("order", 0)
                    apply()
                }
            }
            else{
                order = 1
                binding.btnSortOrder.setImageResource(R.drawable.ic_downward)
                editor.apply {
                    putInt("order", 1)
                    apply()
                }
            }
            noteList.reverse()
        }
        bindSearchBar()
    }

    private fun bindSearchBar() {
        noteList = ArrayList<Note>()
        binding.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            var job: Job? = null
            override fun onQueryTextChange(query: String): Boolean {
                    job?.cancel()
                    job = MainScope().launch {
                        delay(SEARCH_NEWS_TIME_DELAY)
                        query?.let {
                            if(query.isNotEmpty()) {
                                noteList.clear()
                                for(note in noteList){
                                    if(note.title!!.contains(query)){
                                        searchList.add(note)
                                    }
                                }
                            }
                            noteList = searchList
                        }
                    }
                return false
            }
        })
    }

    private fun showPopup(view : View) {
        val popup = PopupMenu(activity, view)
        popup.inflate(R.menu.sort_popup_menu)
        popup.show()
        popup.setOnMenuItemClickListener {
            factor = sharedPref.getString("factor","createdAt")!!
            when (it!!.itemId) {
                R.id.btnDateCreated -> {
                    if(factor=="createdAt"){
                        return@setOnMenuItemClickListener true
                    }
                    if(order == 1){
                        noteList.sortByDescending {it.createdAt}
                    }
                    else{
                        noteList.sortBy { it.createdAt }
                    }
                    editor.apply{
                        putString("factor","createdAt")
                        apply()
                    }
                }
                R.id.btnTitle -> {
                    if(factor=="title"){
                        return@setOnMenuItemClickListener true
                    }
                    if(order == 1){
                        noteList.sortByDescending{ it.title }
                    }
                    else{
                        noteList.sortBy { it.title }
                    }
                    editor.apply{
                        putString("factor","title")
                        apply()
                    }
                }
            }
            true

        }
    }

    override fun onStart() {
        super.onStart()
        setUpRecyclerView()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpRecyclerView() {

        noteViewModel.getAllNotes().observe(viewLifecycleOwner) {
            noteList = it
            factor = sharedPref.getString("factor", "createdAt")!!
            order = sharedPref.getInt("order", 1)
            mAdapter = NoteAdapter(::onItemClicked, ::onLongItemClicked)
            sortList(noteList, order, factor)
            mAdapter.submitList(noteList)
            binding.recyclerView.adapter = mAdapter
            binding.recyclerView.layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }

    }

    private fun sortList(noteList: MutableList<Note>, order: Int, factor: String) {
        if (factor == "createdAt") {
            if (order == 1) {
                noteList.sortByDescending { it.createdAt }
            }
            else {
                noteList.sortBy { it.createdAt }
            }
        }
        else{
            if (order == 1) {
                noteList.sortByDescending { it.title }
            }
            else {
                noteList.sortBy { it.title }
            }
        }
    }


    // open in long click
    private fun showBottomDialog(item: Note) {
        val dialog = BottomSheetDialog(context!!)
        dialog.setContentView(layoutInflater.inflate(R.layout.bottom_popup,null))
        dialog.show()
        val btnDelete=dialog.findViewById<LinearLayout>(R.id.btnDelete)
        val btnShare=dialog.findViewById<LinearLayout>(R.id.btnShare)
        val btnSave=dialog.findViewById<LinearLayout>(R.id.btnSave)
        val btnHide=dialog.findViewById<LinearLayout>(R.id.btnHide)
        val btnFavorite=dialog.findViewById<LinearLayout>(R.id.btnFavorite)
        val btnStatus=dialog.findViewById<LinearLayout>(R.id.btnStatus)
        val tvStatus=btnStatus!!.findViewById<TextView>(R.id.tvStatus)
        val tvFavorite=btnFavorite!!.findViewById<TextView>(R.id.tvFavorite)
        val tvHide=btnHide!!.findViewById<TextView>(R.id.tvHide)
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

        if(item.isPrivate == true){
            tvHide.text="Unhide"
        }
        else{
            tvHide.text="Hide"
        }
        btnDelete!!.setOnClickListener{
            deleteItem(item)
            dialog.dismiss()
        }
        btnShare!!.setOnClickListener{
            //shareFileLink(item)
            dialog.dismiss()
        }
        btnSave!!.setOnClickListener{
            //saveAsFile(item)
            dialog.dismiss()
        }
        btnStatus.setOnClickListener{
            if(item.status == null) item.status = false
            else item.status = true;
            noteViewModel.updateNote(note = item)
            dialog.dismiss()
        }
        btnHide.setOnClickListener{
            if(item.isPrivate){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val km = activity!!.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                    if (km.isKeyguardSecure) {
                        val authIntent = km.createConfirmDeviceCredentialIntent(null, null)
                        tempNote=item
                        startActivityForResult(authIntent, UNHIDE)
                    }
                }
            }
            else{
                item.isPrivate = !item.isPrivate
                noteViewModel.updateNote(note = item)
            }
            dialog.dismiss()
        }
        btnFavorite.setOnClickListener{
            item.isFavorite = !item.isFavorite
            noteViewModel.updateNote(note = item)
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





    private fun deleteItem(item: Note) {
        noteViewModel.deleteNote(item)
    }





     fun onItemClicked(item: Note) {
        if (item.isPrivate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val km = activity!!.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                if (km.isKeyguardSecure) {
                    val authIntent = km.createConfirmDeviceCredentialIntent(null, null)
                    tempNote = item
                    startActivityForResult(authIntent, VIEW_PRIVATE)
                }
            }
        }
        else{
            noteViewIntent(item)
        }
    }

    private fun noteViewIntent(item: Note) {
        findNavController().navigate(R.id.action_mainFragment_to_noteFragment)
    }

    fun onLongItemClicked(item: Note) {
        showBottomDialog(item)
    }
}