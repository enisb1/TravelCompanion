package com.example.travelcompanion.ui.home.start

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.notes.Note
import com.example.travelcompanion.db.pictures.Picture
import com.example.travelcompanion.db.trip.TripState
import com.example.travelcompanion.db.trip.TripType
import java.util.Date

class StartViewModel(private val repository: TravelCompanionRepository) : ViewModel() {
    val locationsList: LiveData<List<Location>> = TrackingRepository.locationList
    val timerSeconds: LiveData<Long> = TrackingRepository.timerSeconds

    fun saveTrip(date: Date, type: TripType, destination: String, state: TripState
    ): Long {
        return repository.insertTrip(date, type, destination, state)
    }

    fun saveNote(note: Note) {
        repository.saveNote(note)
    }

    fun savePicture(picture: Picture) {
        repository.savePicture(picture)
    }

    fun resetTrackingData() {
        TrackingRepository.resetData()
    }
}

class StartViewModelFactory(private val repository: TravelCompanionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StartViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}