package com.bokuno.notes.ui.fragments.notes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bokuno.notes.R
import com.bokuno.notes.databinding.FragmentBottomSheetBinding
import com.bokuno.notes.models.Note
import com.bokuno.notes.utils.InternetConnection
import com.bokuno.notes.utils.PDFGenerator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BottomSheetFragment() : BottomSheetDialogFragment() {

    private lateinit var mHostActivity: Activity
    private lateinit var tempNote: Note
    private val noteViewModel by viewModels<NotesViewModel>()
    private lateinit var binding: FragmentBottomSheetBinding
    private lateinit var item: Note

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val pdfGenerator= PDFGenerator()
    private companion object{
        private const val REQUEST_CODE: Int=101
        private const val TAG = "BMSFxy"
        private const val UNHIDE = 303
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInitialData()
        bindObserver()
    }

    private fun bindObserver() {
        val btnDelete = binding.btnDelete
        val btnShare = binding.btnShare
        val btnSave = binding.btnSave
        val btnHide = binding.btnHide
        val btnFavorite = binding.btnFavorite
        val btnStatus = binding.btnStatus
        val tvStatus = binding.tvStatus
        val tvFavorite =  binding.tvFavorite
        if(item.status == false){
            tvStatus.text="Mark as Done"
        }
        else if(item.status == true){
            btnStatus.visibility=View.GONE
        }

        if(item.isFavorite){
            tvFavorite.text="Mark as unfavorite"
        }
        else{
            tvFavorite.text="Mark as favorite"
        }

        if(item.isPrivate){
            btnHide.visibility = View.GONE
        }
        btnDelete.setOnClickListener{
            noteViewModel.deleteNote(item)
            dismiss()
        }
        btnShare.setOnClickListener{
            shareFileLink(item)
            dismiss()
        }
        btnSave.setOnClickListener{
            saveAsFile(item)
            dismiss()
        }
        btnStatus.setOnClickListener{
            if(item.status == null) item.status = false
            else item.status = true
            noteViewModel.updateNote(item)
            dismiss()
        }
        btnHide.setOnClickListener{
            item.isPrivate = true
            noteViewModel.updateNote(item)
            dismiss()
        }
        btnFavorite.setOnClickListener{
            item.isFavorite = !item.isFavorite
            noteViewModel.updateNote(item)
            dismiss()
        }
    }


    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mHostActivity = activity
    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.size > 0) {

                val writeStorage = grantResults[0] === PackageManager.PERMISSION_GRANTED
                val readStorage = grantResults[1] === PackageManager.PERMISSION_GRANTED
                if (writeStorage && readStorage) {
                    Toast.makeText(activity, "Permission Granted..", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Permission Denied.", Toast.LENGTH_SHORT).show()

                }
            }
        }
    }


    fun shareFileLink(note: Note) {
        if (InternetConnection.checkForInternet(context!!)) {
            if (ContextCompat.checkSelfPermission(
                    context!!.applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context!!.applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                pdfGenerator.flag="SHARE"
                pdfGenerator.createPdf(context!!,note,activity!!)
                val uri = Uri.fromFile(pdfGenerator.file)
                val storage = FirebaseStorage.getInstance()
                val pdfRef = storage.reference.child("pdf/${uri.lastPathSegment}")
                pdfRef.putFile(uri).addOnSuccessListener {
                    it.task.continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let {
                                Toast.makeText(activity, "Couldn't share", Toast.LENGTH_SHORT).show()
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
                            ContextCompat.startActivity(
                                activity!!,
                                Intent.createChooser(shareIntent, "Share Options"),
                                null
                            )
                        } else {
                            Toast.makeText(activity, "Couldn't share", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                ActivityCompat.requestPermissions(
                    activity!!,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_CODE
                )
            }
        }
        else{
            Toast.makeText(activity, "Enable network", Toast.LENGTH_SHORT).show()
        }
    }



    @SuppressLint("SimpleDateFormat")
    fun saveAsFile(item: Note) {
        if (ContextCompat.checkSelfPermission(context!!.applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context!!.applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            pdfGenerator.flag="SAVE"
            pdfGenerator.createPdf(context!!,item,activity!!)
        } else {
            ActivityCompat.requestPermissions(activity!!, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), REQUEST_CODE
            )
        }
    }

    private fun setInitialData() {
        val jsonNote = arguments?.getString("note")
        item = Gson().fromJson(jsonNote, Note::class.java)
    }
}