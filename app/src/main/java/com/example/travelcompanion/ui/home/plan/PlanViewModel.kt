package com.example.travelcompanion.ui.home.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

class PlanViewModelFactory(private val repository: TravelCompanionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}