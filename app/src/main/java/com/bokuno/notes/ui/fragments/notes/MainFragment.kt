package com.bokuno.notes.ui.fragments.notes

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bokuno.notes.NoteAdapter
import com.bokuno.notes.R
import com.bokuno.notes.databinding.FragmentMainBinding
import com.bokuno.notes.models.Note
import com.bokuno.notes.utils.BiometricAuthListener
import com.bokuno.notes.utils.BiometricUtils
import com.bokuno.notes.utils.Constants.Companion.SEARCH_NOTES_TIME_DELAY
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment(), BiometricAuthListener {

    private var tempNote: Note? = null
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var factor : String
    private var order : Int =0
    private lateinit var mAdapter: NoteAdapter
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val noteViewModel by viewModels<NotesViewModel>()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPref= activity!!.getSharedPreferences("SortPref", Context.MODE_PRIVATE)
        editor=sharedPref.edit()
        factor = sharedPref.getString("factor","createdAt")!!   // sorting factor can be creation time or title
        order = sharedPref.getInt("order", 1)   // 1 for descending and 0 for ascending
        setUpRecyclerView()
        binding.btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_noteFragment)
        }

        binding.btnSortFactor.setOnClickListener{
            showPopup(binding.btnSortFactor)
        }

        binding.btnSortOrder.setImageResource(if (order == 1) R.drawable.ic_downward else R.drawable.ic_upward)

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
            updateRecyclerView(factor,order)
        }
        bindSearchBar()
    }

    private fun bindSearchBar() {

        binding.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            var job: Job? = null
            override fun onQueryTextChange(query: String): Boolean {
                    job?.cancel()
                    job = MainScope().launch {
                        delay(SEARCH_NOTES_TIME_DELAY)
                        if(query.isNotEmpty()) {
                            val newQuery = "%$query%"
                            if(view!=null){
                                noteViewModel.getSearchNotes(newQuery).observe(viewLifecycleOwner){
                                    mAdapter.submitList(it)
                                }
                            }
                        }
                        else{
                            updateRecyclerView(factor,order)
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
                    editor.apply{
                        putString("factor","createdAt")
                        apply()
                    }
                    factor="createdAt"
                    updateRecyclerView(factor,order)
                }
                R.id.btnTitle -> {
                    if(factor=="title"){
                        return@setOnMenuItemClickListener true
                    }
                    editor.apply{
                        putString("factor","title")
                        apply()
                    }
                    factor="title"
                    updateRecyclerView(factor,order)
                }
            }
            true

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpRecyclerView() {
        mAdapter = NoteAdapter(::onItemClicked, ::onLongItemClicked)
        binding.recyclerView.apply {
            adapter = mAdapter
            binding.recyclerView.layoutManager =
                GridLayoutManager(context,2)

        }
        updateRecyclerView(factor,order)
    }

    private fun updateRecyclerView(factor: String, order: Int) {
        if(factor == "title"){
            if(view!=null){
                noteViewModel.getNotesSortedByTitle(order).observe(viewLifecycleOwner) {
                    it?.let {
                        mAdapter.submitList(it)
                    }
                }
            }
        }
        else{
            if(view!=null){
                noteViewModel.getNotesSortedByDate(order).observe(viewLifecycleOwner) {
                    it?.let {
                        mAdapter.submitList(it)
                    }
                }
            }
        }
    }

     private fun onItemClicked(item: Note) {
        if (item.isPrivate) {
            if (BiometricUtils.isBiometricReady(context!!)) {
                tempNote=item
                BiometricUtils.showBiometricPrompt(
                    activity = activity as AppCompatActivity,
                    listener = this,
                    cryptoObject = null,
                )
            }
            else {
                Toast.makeText(activity, "No biometric feature perform on this device", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        else{
            noteViewIntent(item)
        }
    }

    override fun onBiometricAuthenticateError(error: Int, errMsg: String) {
        tempNote = null
        Toast.makeText(context, errMsg, Toast.LENGTH_SHORT)
            .show()
    }

    override fun onBiometricAuthenticateFailed() {
        tempNote = null
    }

    override fun onBiometricAuthenticateSuccess(result: BiometricPrompt.AuthenticationResult) {
        tempNote?.let { noteViewIntent(it) }
    }

    private fun noteViewIntent(item: Note) {
        val bundle = Bundle()
        bundle.putString("note",Gson().toJson(item))
        findNavController().navigate(R.id.action_mainFragment_to_noteFragment,bundle)
    }

    private fun onLongItemClicked(item: Note) {
        val bundle = Bundle()
        bundle.putString("note",Gson().toJson(item))
        findNavController().navigate(R.id.action_mainFragment_to_bottomSheetFragment,bundle)
    }

}