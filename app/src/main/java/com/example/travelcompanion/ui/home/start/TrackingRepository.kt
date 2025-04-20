package com.example.travelcompanion.ui.home.start

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object TrackingRepository {
    private val _locationList = MutableLiveData<List<Location>>()
    val locationList: LiveData<List<Location>> = _locationList

    fun addLocation(location: Location) {
        val updatedLocationList = _locationList.value?.toMutableList() ?: mutableListOf()
        updatedLocationList.add(location)
        _locationList.postValue(updatedLocationList)
    }
}