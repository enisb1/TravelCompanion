package com.example.travelcompanion.db.notes

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.travelcompanion.db.pictures.Picture

@Dao
interface NoteDao {
    @Insert
    fun insertNote(note: Note)

    @Update
    fun updateNote(note: Note)

    @Delete
    fun deleteNote(note: Note)

    @Query("SELECT * FROM note_table WHERE trip_id = :tripId")
    fun getNotesByTripId(tripId: Long): List<Note>

    @Query("SELECT * FROM note_table")
    fun getAllNotes(): List<Note>
}