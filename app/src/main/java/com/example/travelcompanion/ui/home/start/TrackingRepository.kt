package com.example.travelcompanion.ui.home.start

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.travelcompanion.db.locations.TripLocation

object TrackingRepository {
    private val _locationList = MutableLiveData<List<TripLocation>>(emptyList())
    val locationList: LiveData<List<TripLocation>> = _locationList

    private val _timerSeconds = MutableLiveData<Long>()
    val timerSeconds: LiveData<Long> = _timerSeconds

    private var distance: Double = 0.0
    val currentDistance: Double
        get() = distance

    fun addLocation(tripLocation: TripLocation) {
        val updatedLocationList = _locationList.value?.toMutableList() ?: mutableListOf()
        updatedLocationList.add(tripLocation)
        _locationList.postValue(updatedLocationList)
    }

    fun incrementTimerValue() {
        _timerSeconds.postValue((_timerSeconds.value ?: 0) + 1 )
    }

    fun resetData() {
        _locationList.value = emptyList()
        _timerSeconds.value = 0L
        distance = 0.0
    }

    fun incrementDistance(metres: Double) {
        distance += metres
    }

}