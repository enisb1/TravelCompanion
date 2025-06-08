package com.example.travelcompanion.ui.journal.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.pictures.Picture
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripState

class ArchiveViewModel(private val repository: TravelCompanionRepository) : ViewModel() {
    fun getCompletedTrips(): List<Trip> {
        return repository.getTripsListByState(TripState.COMPLETED)
    }

    fun getAllPictures(): List<Picture> {
        return repository.getAllPictures()
    }

    fun getPicturesByTripId(tripId: Long): List<Picture> {
        return repository.getPicturesByTripId(tripId)
    }
}

class ArchiveViewModelFactory(private val repository: TravelCompanionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArchiveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArchiveViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}