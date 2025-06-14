package com.example.travelcompanion.ui.journal.archive

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.notes.Note
import com.example.travelcompanion.db.pictures.Picture
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripState

class ArchiveViewModel(private val repository: TravelCompanionRepository) : ViewModel() {
    var spinnerSelection: Int = 1

    private val _completedTrips: LiveData<List<Trip>> = repository.getTripsByState(TripState.COMPLETED)
    val completedTrips: LiveData<List<Trip>> = _completedTrips

    fun getAllPictures(): List<Picture> {
        return repository.getAllPictures()
    }

    fun getPicturesByTripId(tripId: Long): List<Picture> {
        return repository.getPicturesByTripId(tripId)
    }

    fun getAllNotes(): List<Note> {
        return repository.getAllNotes()
    }

    fun getNotesByTripId(tripId: Long): List<Note> {
        return repository.getNotesByTripId(tripId)
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