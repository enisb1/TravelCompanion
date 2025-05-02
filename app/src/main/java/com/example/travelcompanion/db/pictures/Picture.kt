package com.example.travelcompanion.db.pictures

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.travelcompanion.db.trip.Trip

@Entity(
    tableName = "picture_table",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["trip_id"],
            childColumns = ["trip_id"]
        )
    ],
    indices = [Index(value = ["trip_id"])]  // look up structure
)
data class Picture(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "picture_id")
    var id: Long,

    @ColumnInfo(name = "trip_id")
    var tripId: Long = 0,    // default is 0, will be defined when it's time to add note to db

    @ColumnInfo(name = "picture_date")
    var timestamp: Long,

    @ColumnInfo(name = "picture_uri")
    var uri: String,
)