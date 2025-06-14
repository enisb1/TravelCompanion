package com.example.travelcompanion.db.trip

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TripDao {
    @Insert
    fun insertTrip(trip: Trip): Long

    @Update
    fun updateTrip(trip: Trip)

    @Delete
    fun deleteTrip(trip: Trip)

    @Query("SELECT * FROM trip_table")
    fun getAllTrips(): LiveData<List<Trip>>

    @Query("SELECT * FROM trip_table WHERE trip_state = :state")
    fun getTripsByState(state: TripState): LiveData<List<Trip>>

    @Query("SELECT * FROM trip_table WHERE trip_state = :state")
    fun getTripsListByState(state: TripState): List<Trip>

    @Query("SELECT * FROM trip_table WHERE trip_id = :id")
    fun getTripById(id: Long): Trip?

    @Query("SELECT * FROM trip_table WHERE trip_state = \"COMPLETED\" AND trip_start_date BETWEEN :startDate AND :endDate AND trip_end_date BETWEEN :startDate AND :endDate ORDER BY trip_start_date ASC")
    suspend fun getTripsByDateRange(startDate: Long, endDate: Long): List<Trip>

    @Query("SELECT DISTINCT trip_destination FROM trip_table WHERE trip_destination IS NOT NULL AND trip_destination != ''")
    suspend fun getDistinctDestinations(): List<String>
}