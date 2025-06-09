package com.example.travelcompanion.ui.journal.archive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.notes.Note
import java.text.SimpleDateFormat
import java.util.Locale

class NotesAdapter(
    private val notes: List<Note>,
    private val tripIdToTitle: Map<Long, String>,
    private val tripIdToDestination: Map<Long, String>
): RecyclerView.Adapter<NotesAdapter.NotesViewHolder>() {

    inner class NotesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tripTitleTxtView = itemView.findViewById<TextView>(R.id.trip_title)
        val tripDestinationTxtView = itemView.findViewById<TextView>(R.id.trip_destination)
        val noteTitleTxtView = itemView.findViewById<TextView>(R.id.note_title)
        val noteDateTxtView = itemView.findViewById<TextView>(R.id.note_date)
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
        holder.tripTitleTxtView.text = tripIdToTitle[note.tripId]
        holder.tripDestinationTxtView.text = tripIdToDestination[note.tripId]
        holder.noteDateTxtView.text =
            SimpleDateFormat("HH:mm, dd MMM yyyy", Locale.getDefault()).format(note.date)
        holder.noteContextTxtView.text = note.content
    }

}