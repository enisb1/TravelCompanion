package com.example.travelcompanion.ui.home.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripState
import com.example.travelcompanion.db.trip.TripType
import java.util.Date

class TripViewModel(private val repository: TravelCompanionRepository) : ViewModel() {
    val plans = repository.getTripsByState(TripState.PLANNED)
    val completedTrips = repository.getTripsByState(TripState.COMPLETED)

    fun insertPlan(title: String, startDate: Date, type: TripType, destination: String) {
        repository.insertTrip(
            title = title,
            start = startDate.time,
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
        if (modelClass.isAssignableFrom(TripViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class CompletedTripViewModelFactory(private val repository: TravelCompanionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}