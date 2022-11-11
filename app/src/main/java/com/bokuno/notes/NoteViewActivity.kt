package com.bokuno.notes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import com.bokuno.notes.databinding.ActivityNoteViewBinding
import java.text.SimpleDateFormat

class NoteViewActivity : AppCompatActivity() {
    private lateinit var binding:ActivityNoteViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityNoteViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent=intent
        val title = intent.getStringExtra("title")
        val note = intent.getStringExtra("note")
        val location = intent.getStringExtra("location")
        val formatter = SimpleDateFormat("dd-MM-yyyy 'at' HH:mm")
        val createdAt = formatter.format(intent.getLongExtra("createdAt",0))

        binding.tvTitle.text=SpannableStringBuilder()
            .bold { append("Title : ") }
            .append("$title")
        if(location!=null) {
            binding.tvLocation.text = SpannableStringBuilder()
                .bold { append("Location : ") }
                .append("$location")
        }
        else{
            binding.tvLocation.text = SpannableStringBuilder()
                .bold { append("Location : ") }
                .append("Not Specified")
        }
        binding.tvCreatedAt.text=SpannableStringBuilder()
            .bold { append("Time Created : ") }
            .append("$createdAt")
        binding.tvNote.text=SpannableStringBuilder()
            .bold { append("Note : ") }
            .append("$note")
    }
}

