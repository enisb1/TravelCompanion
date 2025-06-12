package com.example.travelcompanion.ui.settings

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.travelcompanion.R
import androidx.core.content.edit
import com.example.travelcompanion.workers.ActivityRecognitionReceiver
import com.google.android.gms.location.ActivityRecognition

class SettingsFragment : Fragment() {

    companion object {
        const val TRACK_CAR = "trackCar"
        const val TRACK_BICYCLE = "trackBicycle"
        const val TRACK_RUNNING = "trackRunning"
    }

    private lateinit var requestPermissionLauncherForActivityRecognition: ActivityResultLauncher<String>

    private lateinit var prefs: SharedPreferences

    private lateinit var carLayout: FrameLayout
    private lateinit var bicycleLayout: FrameLayout
    private lateinit var runningLayout: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireContext().getSharedPreferences("goals", 0)   //TODO: change to name "settings"
        val etTrips = view.findViewById<EditText>(R.id.etMonthlyTripsGoal)
        val etDistance = view.findViewById<EditText>(R.id.etMonthlyDistanceGoal)
        val btnSave = view.findViewById<Button>(R.id.btnSaveGoals)
        carLayout = view.findViewById(R.id.car_layout)
        bicycleLayout = view.findViewById(R.id.bicycle_layout)
        runningLayout = view.findViewById(R.id.running_layout)

        requestPermissionLauncherForActivityRecognition = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startNewActivityRecognition()
                Toast.makeText(requireContext(), "Settings set!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Activity recognition permission is required", Toast.LENGTH_SHORT).show()
            }
        }

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
            }
            launchActivityRecognition()
            //Toast.makeText(requireContext(), "Objectives set!", Toast.LENGTH_SHORT).show()
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

    private fun launchActivityRecognition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncherForActivityRecognition.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                startNewActivityRecognition()
            }
        } else {    // no runtime permission needed
            startNewActivityRecognition()
        }
    }

    private fun startNewActivityRecognition() {
        val activityRecognitionClient = ActivityRecognition.getClient(requireContext())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ActivityRecognitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        activityRecognitionClient.removeActivityUpdates(pendingIntent).addOnCompleteListener {
            // Now request updates again (based on user settings)
            if (carLayout.isSelected || bicycleLayout.isSelected || runningLayout.isSelected) {
                activityRecognitionClient.requestActivityUpdates(
                    10_000, // interval in ms
                    pendingIntent
                ).addOnSuccessListener {
                    prefs.edit {
                        putBoolean(TRACK_CAR, carLayout.isSelected)
                        .putBoolean(TRACK_BICYCLE, bicycleLayout.isSelected)
                        .putBoolean(TRACK_RUNNING, runningLayout.isSelected)
                    }
                    bicycleLayout.isSelected
                    Toast.makeText(requireContext(), "Settings set!", Toast.LENGTH_SHORT).show()
                    Log.d("ActivityUpdates", "Started successfully.")
                }.addOnFailureListener {
                    Log.e("ActivityUpdates", "Failed to start updates", it)
                    Toast.makeText(requireContext(), "Failed to set activity recognition tracking!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("ActivityUpdates", "User disabled activity updates.")
                Toast.makeText(requireContext(), "Settings set!", Toast.LENGTH_SHORT).show()
            }
        }
    }

}