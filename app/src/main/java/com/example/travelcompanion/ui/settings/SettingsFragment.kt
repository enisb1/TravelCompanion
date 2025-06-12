package com.example.travelcompanion.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import com.example.travelcompanion.R
import androidx.core.content.edit

class SettingsFragment : Fragment() {

    companion object {
        const val TRACK_CAR = "trackCar"
        const val TRACK_BICYCLE = "trackBicycle"
        const val TRACK_RUNNING = "trackRunning"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().getSharedPreferences("goals", 0)
        val etTrips = view.findViewById<EditText>(R.id.etMonthlyTripsGoal)
        val etDistance = view.findViewById<EditText>(R.id.etMonthlyDistanceGoal)
        val btnSave = view.findViewById<Button>(R.id.btnSaveGoals)
        val carLayout = view.findViewById<FrameLayout>(R.id.car_layout)
        val bicycleLayout = view.findViewById<FrameLayout>(R.id.bicycle_layout)
        val runningLayout = view.findViewById<FrameLayout>(R.id.running_layout)

        // Load existing objectives if available
        etTrips.setText(prefs.getInt("monthlyTripsGoal", 0).takeIf { it > 0 }?.toString() ?: "")
        etDistance.setText(
            prefs.getInt("monthlyDistanceGoal", 0).takeIf { it > 0 }?.toString() ?: ""
        )

        btnSave.setOnClickListener {
            val tripsGoal = etTrips.text.toString().toIntOrNull() ?: 0
            val distanceGoal = etDistance.text.toString().toIntOrNull() ?: 0
            prefs.edit {
                putInt("monthlyTripsGoal", tripsGoal)
                    .putInt("monthlyDistanceGoal", distanceGoal)
                    .putBoolean(TRACK_CAR, carLayout.isSelected)
                    .putBoolean(TRACK_BICYCLE, bicycleLayout.isSelected)
                    .putBoolean(TRACK_RUNNING, runningLayout.isSelected)
            }
            Toast.makeText(requireContext(), "Objectives set!", Toast.LENGTH_SHORT).show()
        }

        // activity recognition
        val trackCar = prefs.getBoolean(TRACK_CAR, false)
        val trackBicycle = prefs.getBoolean(TRACK_BICYCLE, false)
        val trackRunning = prefs.getBoolean(TRACK_RUNNING, false)
        if (trackCar)
            carLayout.isSelected = true
        if (trackBicycle)
            bicycleLayout.isSelected = true
        if (trackRunning)
            runningLayout.isSelected = true

        for (frameLayout in listOf(carLayout, bicycleLayout, runningLayout)) {
            frameLayout.setOnClickListener {
                it.isSelected = !it.isSelected
            }
        }
    }
}