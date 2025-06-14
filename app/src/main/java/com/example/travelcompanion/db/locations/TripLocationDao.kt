package com.example.travelcompanion.db.locations

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.travelcompanion.db.trip.TripType

@Dao
interface TripLocationDao {
    @Insert
    fun insertLocation(tripLocation: TripLocation)

    @Query("SELECT * FROM  locations_table")
    fun getLocations(): List<TripLocation>

    @Query("""
    SELECT locations_table.* 
    FROM locations_table
    INNER JOIN trip_table ON locations_table.trip_id = trip_table.trip_id
    WHERE trip_table.trip_type = :type
""")
    fun getLocationsByTripType(type: TripType): List<TripLocation>
}