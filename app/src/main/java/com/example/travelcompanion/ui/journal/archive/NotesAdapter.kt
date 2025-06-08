package com.example.travelcompanion.ui.journal.archive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.notes.Note

class NotesAdapter(private val notes: List<Note>): RecyclerView.Adapter<NotesAdapter.NotesViewHolder>() {

    inner class NotesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tripTitleTxtView = itemView.findViewById<TextView>(R.id.trip_title)
        val noteTitleTxtView = itemView.findViewById<TextView>(R.id.note_title)
        val noteContextTxtView = itemView.findViewById<TextView>(R.id.note_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recview_item_archive_notes, parent, false)
        return NotesViewHolder(view)
    }

    override fun getItemCount(): Int {
        return notes.size
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        val note = notes[position]
        holder.noteTitleTxtView.text = note.title
        holder.noteContextTxtView.text = note.content
    }

}