package com.example.travelcompanion.ui.analysis_prediction.analysis

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripState

class AnalysisViewModel(private val repository: TravelCompanionRepository): ViewModel() {
    val labels: MutableList<String> = mutableListOf()
    val values: MutableList<Float> = mutableListOf()
    var spinnerSelection = MutableLiveData(0)

    fun getCompletedTrips(): List<Trip> {
        return repository.getTripsListByState(TripState.COMPLETED)
    }
}

class AnalysisViewModelFactory(private val repository: TravelCompanionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalysisViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}