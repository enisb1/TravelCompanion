package com.example.travelcompanion.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_table")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "trip_id")
    var id: Int,

    @ColumnInfo(name = "trip_date")
    var date: Long,

    @ColumnInfo(name = "trip_type")
    var type: TripType,

    @ColumnInfo(name = "trip_destination")
    var destination: String,

    @ColumnInfo(name = "trip_state")
    var state: TripState
)
