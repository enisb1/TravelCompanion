package com.example.travelcompanion.ui.home.start

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class StartViewModel : ViewModel() {
    val locationsList: LiveData<List<Location>> = TrackingRepository.locationList
}