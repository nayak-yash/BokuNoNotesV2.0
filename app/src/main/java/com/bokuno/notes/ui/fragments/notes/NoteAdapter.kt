@file:Suppress("DEPRECATION")

package com.bokuno.notes

import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bokuno.notes.models.Note
import java.text.SimpleDateFormat


class NoteAdapter(
    private val onItemClicked:(Note) -> Unit,
    private val onLongItemClicked:(Note) -> Unit): ListAdapter<Note, NoteAdapter.NoteViewHolder>(ComparatorDiffUtil()) {

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteText: TextView = itemView.findViewById(R.id.tvNote)
        val titleText: TextView = itemView.findViewById(R.id.tvTitle)
        val createdAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        val location: TextView = itemView.findViewById(R.id.tvLocation)
        val favorite: ImageView = itemView.findViewById(R.id.ivFavorite)
        val status: ImageView= itemView.findViewById(R.id.ivStatus)
        val unsee:ImageView = itemView.findViewById(R.id.ivUnsee)

        fun bind(model: Note) {
            itemView.setOnClickListener{
                onItemClicked(model)
            }
            itemView.setOnLongClickListener{
                onLongItemClicked(model)
                return@setOnLongClickListener true
            }
            if(model.isPrivate){
                noteText.visibility = View.GONE
                titleText.visibility = View.GONE
                createdAt.visibility = View.GONE
                status.visibility = View.GONE
                favorite.visibility = View.GONE
                location.visibility = View.GONE
                unsee.visibility = View.VISIBLE
                return
            }
            else{
                noteText.visibility = View.VISIBLE
                titleText.visibility = View.VISIBLE
                createdAt.visibility = View.VISIBLE
                status.visibility = View.VISIBLE
                favorite.visibility = View.VISIBLE
                location.visibility = View.VISIBLE
                unsee.visibility = View.GONE
            }
            noteText.text=model.text
            titleText.text=model.title
            val formatter = SimpleDateFormat("dd-MM-yyyy 'at' HH:mm")
            val timeFormatted = formatter.format(model.createdAt)
            createdAt.text=timeFormatted
            if(model.status==null){
                status.visibility=View.GONE
            }
            else if(model.status==false){
                status.visibility=View.VISIBLE
                status.setImageResource(R.drawable.ic_todo)
            }
            else{
                status.visibility=View.VISIBLE
                status.setImageResource(R.drawable.ic_done)
            }

            if(model.isFavorite == false){
                favorite.visibility=View.GONE
            }
            else{
                favorite.visibility=View.VISIBLE
            }
            if(model.location == null){
                location.visibility=View.GONE
            }
            else {
                location.visibility=View.VISIBLE
                location.text = model.location
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}
class ComparatorDiffUtil : DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem.noteId == newItem.noteId
    }

    override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem == newItem
    }
}