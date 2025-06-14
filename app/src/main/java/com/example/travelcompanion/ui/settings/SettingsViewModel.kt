package com.example.travelcompanion.ui.settings

import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {
    var inactivityDays = 0
    var isCarSelected: Boolean? = null
    var isBicycleSelected: Boolean? = null
    var isRunningSelected: Boolean? = null
    var monthlyTripsGoal: Int? = null
    var monthlyDistanceGoal: Int? = null

    fun resetData() {
        inactivityDays = 0
        isCarSelected = null
        isBicycleSelected = null
        isRunningSelected = null
        monthlyTripsGoal = null
        monthlyDistanceGoal = null
    }
}