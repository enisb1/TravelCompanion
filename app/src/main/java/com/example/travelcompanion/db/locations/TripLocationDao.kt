package com.example.travelcompanion.db.locations

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TripLocationDao {
    @Insert
    fun insertLocation(tripLocation: TripLocation)

    @Query("SELECT * FROM  locations_table")
    fun getLocations(): List<TripLocation>
}