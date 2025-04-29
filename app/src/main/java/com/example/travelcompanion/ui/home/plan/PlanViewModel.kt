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

    fun savePlan(date: Date, type: TripType, destination: String) {
        // Convert Date to milliseconds since epoch
        val dateInMillis = date.time
        val trip = Trip(0, dateInMillis, type, destination, TripState.PLANNED)
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