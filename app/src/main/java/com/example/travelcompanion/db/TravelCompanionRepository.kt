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

    var dao : TripDao

    init {
        val db = TravelCompanionDatabase.getInstance(app)
        dao = db.tripDao()
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
            dao.insertTrip(trip)
        }
    }

    fun updateTrip(trip: Trip) {
        GlobalScope.launch {
            dao.updateTrip(trip)
        }
    }

    fun deleteTrip(trip: Trip) {
        GlobalScope.launch {
            dao.deleteTrip(trip)
        }
    }

    fun getAllTrips(): LiveData<List<Trip>> {
        return dao.getAllTrips()
    }

    fun getTripsByState(state: TripState): LiveData<List<Trip>> {
        return dao.getTripsByState(state)
    }

    // Not sure this will work
    suspend fun getTripById(id: Int): Trip? {
        return dao.getTripById(id)
    }

    // Not sure this will work
    suspend fun getTripsByDateRange(startDate: Long, endDate: Long): List<Trip> {
        return dao.getTripsByDateRange(startDate, endDate)
    }

}