package com.example.travelcompanion.db.trip

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_table")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "trip_id")
    var id: Int,

    @ColumnInfo(name = "trip_start_date")
    var startTimestamp: Long,

    @ColumnInfo(name = "trip_end_date")
    var endTimestamp: Long = 0,

    @ColumnInfo(name = "trip_type")
    var type: TripType,

    @ColumnInfo(name = "trip_destination")
    var destination: String,

    @ColumnInfo(name = "trip_state")
    var state: TripState,

    @ColumnInfo(name = "trip_duration")
    var duration: Long = 0L, // Duration in seconds

    @ColumnInfo(name = "trip_distance")
    var distance: Double = 0.0, // Distance in meters
)
