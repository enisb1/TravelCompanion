package com.example.travelcompanion.ui.home.plan

import androidx.lifecycle.ViewModel
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripState
import com.example.travelcompanion.db.trip.TripType
import java.util.Date

class PlanViewModel(private val repository: TravelCompanionRepository) : ViewModel() {
    val plans = repository.getTripsByState(TripState.PLANNED)


    fun insertPlan(startDate: Date, type: TripType, destination: String) {
        repository.insertTrip(
            startDate = startDate,
            type = type,
            destination = destination,
            state = TripState.PLANNED
        )
    }

    fun updatePlan(trip: Trip) {
        repository.updateTrip(trip)
    }

    fun deletePlan(trip: Trip) {
        repository.deleteTrip(trip)
    }

}