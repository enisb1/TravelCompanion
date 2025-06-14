package com.example.travelcompanion.db.locations

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.travelcompanion.db.trip.Trip

@Entity(
    tableName = "locations_table",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["trip_id"],
            childColumns = ["trip_id"]
        )
    ],
    indices = [Index(value = ["trip_id"])]  // look up structure
)
data class TripLocation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "location_id")
    var id: Long,

    @ColumnInfo(name = "trip_id")
    var tripId: Long = 0,    // default is 0, will be defined when it's time to add location to db

    var timestamp: Long,

    var latitude: Double,

    var longitude: Double,
)