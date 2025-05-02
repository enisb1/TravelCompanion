package com.example.travelcompanion.db.notes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.travelcompanion.db.trip.Trip

@Entity(tableName = "note_table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "note_id")
    var id: Long,

    var tripId: Long = 0,    // default is 0, will be defined when it's time to add note to db

    @ColumnInfo(name = "note_timestamp")
    var date: Long,

    @ColumnInfo(name = "note_title")
    var title: String,

    @ColumnInfo(name = "note_content")
    var content: String,
)