package com.example.travelcompanion.db

import android.app.Application
import androidx.lifecycle.LiveData
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripDao
import com.example.travelcompanion.db.trip.TripState
import com.example.travelcompanion.db.trip.TripType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Date

class TravelCompanionRepository(app: Application) {

    private var tripDao : TripDao

    init {
        val db = TravelCompanionDatabase.getInstance(app)
        tripDao = db.tripDao()
    }

    fun insertTrip(startDate: Date, type: TripType, destination: String, state: TripState) {
        // Convert Date to milliseconds since epoch
        val dateInMillis = startDate.time
        val trip = Trip(
            id = 0,
            start_date = dateInMillis,
            type = type,
            destination = destination,
            state = state)
        GlobalScope.launch {
            tripDao.insertTrip(trip)
        }
    }

    fun updateTrip(trip: Trip) {
        GlobalScope.launch {
            tripDao.updateTrip(trip)
        }
    }

    fun deleteTrip(trip: Trip) {
        GlobalScope.launch {
            tripDao.deleteTrip(trip)
        }
    }

    fun getAllTrips(): LiveData<List<Trip>> {
        return tripDao.getAllTrips()
    }

    fun getTripsByState(state: TripState): LiveData<List<Trip>> {
        return tripDao.getTripsByState(state)
    }

    // Not sure this will work
    suspend fun getTripById(id: Int): Trip? {
        return tripDao.getTripById(id)
    }

    // Not sure this will work
    suspend fun getTripsByDateRange(startDate: Long, endDate: Long): List<Trip> {
        return tripDao.getTripsByDateRange(startDate, endDate)
    }

}