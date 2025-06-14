package com.example.travelcompanion.ui.journal.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.locations.TripLocation
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripType

class MapViewModel(private val repository: TravelCompanionRepository): ViewModel() {
    var spinnerSelection: Int = 0

    fun getLocations(): List<TripLocation> {
        return repository.getLocations()
    }

    fun getLocationsByTripType(type: TripType): List<TripLocation> {
        return repository.getLocationsByTripType(type)
    }

    fun getTripById(tripId: Long): Trip? {
        return repository.getTripById(tripId)
    }
}

class MapViewModelFactory(private val repository: TravelCompanionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}