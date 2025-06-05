package com.example.travelcompanion.db.locations

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface TripLocationDao {
    @Insert
    fun insertLocation(tripLocation: TripLocation)
}