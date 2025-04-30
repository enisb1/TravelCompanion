package com.example.travelcompanion.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TripDao {
    @Insert
    suspend fun insertTrip(trip: Trip)

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("SELECT * FROM trip_table")
    fun getAllTrips(): LiveData<List<Trip>>

    @Query("SELECT * FROM trip_table WHERE trip_state = :state")
    fun getTripsByState(state: TripState): LiveData<List<Trip>>

    @Query("SELECT * FROM trip_table WHERE trip_id = :id")
    suspend fun getTripById(id: Int): Trip?

    @Query("SELECT * FROM trip_table WHERE trip_start_date BETWEEN :startDate AND :endDate")
    suspend fun getTripsByDateRange(startDate: Long, endDate: Long): List<Trip>
}