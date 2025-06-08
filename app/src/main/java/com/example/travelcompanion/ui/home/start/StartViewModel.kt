package com.example.travelcompanion.ui.home.start

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.locations.TripLocation
import com.example.travelcompanion.db.notes.Note
import com.example.travelcompanion.db.pictures.Picture
import com.example.travelcompanion.db.trip.TripState
import com.example.travelcompanion.db.trip.TripType
import java.util.Date

class StartViewModel(private val repository: TravelCompanionRepository) : ViewModel() {
    val locationsList: LiveData<List<TripLocation>> = TrackingRepository.locationList
    val timerSeconds: LiveData<Long> = TrackingRepository.timerSeconds

    fun saveTrip(title: String, startDate: Date, type: TripType, destination: String, state: TripState
    ): Long {
        val tripId = repository.insertTrip(
            title = title,
            startDate,
            type,
            destination,
            state,
            TrackingRepository.timerSeconds.value ?: 0,
            TrackingRepository.currentDistance
        )

        return tripId
    }

    fun saveLocations(tripId: Long) {
        locationsList.value?.forEach{ it.tripId = tripId }
        repository.saveLocations(locationsList.value ?: listOf())
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