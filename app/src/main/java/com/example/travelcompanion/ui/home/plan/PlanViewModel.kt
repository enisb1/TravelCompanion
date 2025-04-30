package com.example.travelcompanion.ui.home.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.db.Trip
import com.example.travelcompanion.db.TripDao
import com.example.travelcompanion.db.TripState
import com.example.travelcompanion.db.TripType
import kotlinx.coroutines.launch
import java.util.Date

class PlanViewModel(private val dao: TripDao) : ViewModel() {
    val plans = dao.getTripsByState(TripState.PLANNED)

    fun savePlan(startDate: Date, type: TripType, destination: String) {
        // Convert Date to milliseconds since epoch
        val dateInMillis = startDate.time
        val trip = Trip(
            id = 0,
            start_date = dateInMillis,
            type = type,
            destination = destination,
            state = TripState.PLANNED)
        viewModelScope.launch {
            dao.insertTrip(trip)
        }
    }

    fun updatePlan(trip: Trip) {
        viewModelScope.launch {
            dao.updateTrip(trip)
        }
    }

    fun deletePlan(trip: Trip) {
        viewModelScope.launch {
            dao.deleteTrip(trip)
        }
    }

}