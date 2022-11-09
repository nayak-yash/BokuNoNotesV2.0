@file:Suppress("DEPRECATION")

package com.bokuno.notes

import android.graphics.Color
import android.graphics.Color.parseColor
import android.graphics.ColorFilter
import android.opengl.Visibility
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bokuno.notes.models.Note
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.text.SimpleDateFormat


class NoteAdapter(private val item: ArrayList<Note>,private val listener:INoteAdapter): RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteText: TextView = itemView.findViewById(R.id.tvNote)
        val titleText: TextView = itemView.findViewById(R.id.tvTitle)
        val createdAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        val location: TextView = itemView.findViewById(R.id.tvLocation)
        val favorite: ImageView = itemView.findViewById(R.id.ivFavorite)
        val status: ImageView= itemView.findViewById(R.id.ivStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view= LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        val viewHolder=NoteViewHolder(view)
        view.setOnClickListener{
            listener.onItemClicked(item[viewHolder.adapterPosition])      }
        view.setOnLongClickListener{
            listener.onLongItemClicked(item[viewHolder.adapterPosition])
            return@setOnLongClickListener true
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val model=item[position]
        holder.noteText.text=model.text
        holder.titleText.text=model.title
        val formatter = SimpleDateFormat("dd-MM-yyyy 'at' HH:mm")
        val createdAt = formatter.format(model.createdAt)
        holder.createdAt.text=createdAt
        if(model.status==null){
            holder.status.visibility=View.GONE
        }
        else if(model.status==false){
            holder.status.visibility=View.VISIBLE
            holder.status.setImageResource(R.drawable.ic_todo)
        }
        else{
            holder.status.visibility=View.VISIBLE
            holder.status.setImageResource(R.drawable.ic_done)
        }

        if(model.isFavorite == false){
            holder.favorite.visibility=View.GONE
        }
        else{
            holder.favorite.visibility=View.VISIBLE
        }
        if(model.location == null){
            holder.location.visibility=View.GONE
        }
        else {
            holder.location.visibility=View.VISIBLE
            holder.location.text = model.location
        }
    }

    override fun getItemCount(): Int {
        return item.size
    }
}
interface INoteAdapter{
    fun onItemClicked(item:Note)
    fun onLongItemClicked(item:Note)
}