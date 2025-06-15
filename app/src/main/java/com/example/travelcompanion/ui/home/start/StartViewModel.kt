package com.example.travelcompanion.ui.home.start

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.locations.TripLocation
import com.example.travelcompanion.db.notes.Note
import com.example.travelcompanion.db.pictures.Picture
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripState
import com.example.travelcompanion.db.trip.TripType
import java.util.Date

class StartViewModel(private val repository: TravelCompanionRepository) : ViewModel() {
    val locationsList: LiveData<List<TripLocation>> = TrackingRepository.locationList
    val timerSeconds: LiveData<Long> = TrackingRepository.timerSeconds

    val notes: MutableList<Note> = mutableListOf()
    val pictures: MutableList<Picture> = mutableListOf()

    var start: Long = 0L
        private set

    fun setStart() {
        start = Date().time
    }

    private val _isTripStarted = MutableLiveData(false)
    val isTripStarted: LiveData<Boolean> = _isTripStarted

    fun startTrip() {
        _isTripStarted.value = true
    }

    fun stopTrip() {
        _isTripStarted.value = false
    }

    fun saveTrip(title: String, start: Long, type: TripType, destination: String, state: TripState
    ): Long {
        val tripId = repository.insertTrip(
            title = title,
            start,
            type,
            destination,
            state,
            TrackingRepository.timerSeconds.value ?: 0,
            TrackingRepository.currentDistance
        )

        return tripId
    }

    fun setTripToCompleted(trip: Trip) {
        trip.state = TripState.COMPLETED
        trip.startTimestamp = start
        trip.duration = TrackingRepository.timerSeconds.value ?: 0
        trip.endTimestamp = trip.startTimestamp + trip.duration
        trip.distance = TrackingRepository.currentDistance
        repository.updateTrip(trip)
    }

    fun getTripById(tripId: Long): Trip? {
        return repository.getTripById(tripId)
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
        notes.clear()
        pictures.clear()
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