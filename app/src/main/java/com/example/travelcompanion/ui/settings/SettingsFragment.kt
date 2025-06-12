package com.example.travelcompanion.ui.settings

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.content.Context
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.example.travelcompanion.R
import androidx.core.content.edit
import com.example.travelcompanion.workers.ActivityRecognitionReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import android.widget.NumberPicker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.travelcompanion.workers.InactivityReminderWorker

class SettingsFragment : Fragment() {

    companion object {
        const val TRACK_CAR = "trackCar"
        const val TRACK_BICYCLE = "trackBicycle"
        const val TRACK_RUNNING = "trackRunning"
    }
    
    private val notificationPermissionRequestCode = 1001

    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var activityRecognitionAllPermissionsLauncher: ActivityResultLauncher<String>

    private lateinit var activityRecognitionSinglePermissionLauncher: ActivityResultLauncher<String>

    private lateinit var prefs: SharedPreferences

    private lateinit var carLayout: FrameLayout
    private lateinit var bicycleLayout: FrameLayout
    private lateinit var runningLayout: FrameLayout
    private lateinit var numberPicker: NumberPicker

    // booleans for toast message
    private var notificationPermissionNeededAndNotGiven = false

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

        // checks first for activity recognition permission and then for notification permission
        activityRecognitionAllPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {  // activity recognition permission granted, now notification permission is needed
                notificationPermissionCheckForActivityTracking()
            } else {
                Toast.makeText(requireContext(), "Activity recognition not set due to missing permission", Toast.LENGTH_SHORT).show()
                clearActivityRecognitionTrackingChoices()
            }
        }

        // used only for final permission check regarding activity recognition (after notification
        // check done by reminder for inactivity, if this permission is granted, or after activity
        // recognition permission is given in the full permission check done for activity recognition)
        activityRecognitionSinglePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startNewActivityRecognition()
                Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
            }
            else {
                clearActivityRecognitionTrackingChoices()
                Toast.makeText(requireContext(), "Activity recognition not possible due " +
                        "to missing permissions", Toast.LENGTH_SHORT).show()
            }
        }

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                saveSettings(pendingTripsGoal, pendingDistanceGoal, pendingInactivityDays)
                if (needToLaunchActivityRecognition())
                    permissionCheckForActivityRecognition()
                else
                    Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
            } else {
                // Save settings without enabling the reminder
                saveSettings(pendingTripsGoal, pendingDistanceGoal, 0)
                numberPicker.value = 0
                Toast.makeText(requireContext(), "Notification permission is required", Toast.LENGTH_SHORT).show()
                clearActivityRecognitionTrackingChoices()
            }
        }

        numberPicker = view.findViewById<NumberPicker>(R.id.np_inactivity_days)
        val daysOptions = Array(31) { i -> if (i == 0) "Off" else i.toString() }
        numberPicker.minValue = 0
        numberPicker.maxValue = 30
        numberPicker.displayedValues = daysOptions

        val settingsPrefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedInactivityDays = settingsPrefs.getInt("inactivity_days", 0)
        numberPicker.value = savedInactivityDays // 0 = Off, 1-30 = days

        var selectedInactivityDays = numberPicker.value
        val workRequest = PeriodicWorkRequestBuilder<InactivityReminderWorker>(1, TimeUnit.DAYS).build()


        // Load existing objectives if available
        etTrips.setText(prefs.getInt("monthlyTripsGoal", 0).takeIf { it > 0 }?.toString() ?: "")
        etDistance.setText(
            prefs.getInt("monthlyDistanceGoal", 0).takeIf { it > 0 }?.toString() ?: ""
        )

        selectedInactivityDays = savedInactivityDays

        numberPicker.setOnValueChangedListener { _, _, newVal ->
            selectedInactivityDays = newVal
        }

        btnSave.setOnClickListener {
            val tripsGoal = etTrips.text.toString().toIntOrNull() ?: 0
            val distanceGoal = etDistance.text.toString().toIntOrNull() ?: 0

            // inactivity reminder and objectives
            if (selectedInactivityDays == 0 && !needToLaunchActivityRecognition()) {
                // Turn off reminder
                WorkManager.getInstance(requireContext()).cancelUniqueWork("inactivity_reminder")
                saveSettings(tripsGoal, distanceGoal, selectedInactivityDays)
                // show toast message
                Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
            } else if (selectedInactivityDays > 0) {
                // Requests notification permission if not already granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingTripsGoal = tripsGoal
                    pendingDistanceGoal = distanceGoal
                    pendingInactivityDays = selectedInactivityDays

                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    saveSettings(tripsGoal, distanceGoal, selectedInactivityDays)
                    if (needToLaunchActivityRecognition())
                        permissionCheckForActivityRecognition()
                }
            }
            else {  // needToLaunchActivityRecognition() == true && selectedInactivityDays == 0
                saveSettings(tripsGoal, distanceGoal, 0)
                fullPermissionCheckForActivityTracking()
            }
        }
        
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "inactivity_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

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

    private fun clearActivityRecognitionTrackingChoices() {
        for (layout in listOf(carLayout, bicycleLayout, runningLayout))
            layout.isSelected = false
    }

    private fun needToLaunchActivityRecognition(): Boolean {
        return carLayout.isSelected || bicycleLayout.isSelected || runningLayout.isSelected
    }

    private fun fullPermissionCheckForActivityTracking() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                activityRecognitionAllPermissionsLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                notificationPermissionCheckForActivityTracking()
            }
        } else {    // no runtime permission needed and api not high enough to request permissions for notifications
            startNewActivityRecognition()
            Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
        }
    }

    // notification permission is already granted
    private fun permissionCheckForActivityRecognition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                activityRecognitionSinglePermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                startNewActivityRecognition()
                Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
            }
        } else {    // no runtime permission needed and api not high enough to request permissions for notifications
            startNewActivityRecognition()
            Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun notificationPermissionCheckForActivityTracking() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                activityRecognitionSinglePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startNewActivityRecognition()
                Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
            }
        } else {
            startNewActivityRecognition()
            Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    private fun startNewActivityRecognition() {
        val activityRecognitionClient = ActivityRecognition.getClient(requireContext())
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            322,
            Intent(requireContext(), ActivityRecognitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent).addOnCompleteListener {
            val transitions = getTransitions()
            if (transitions.isNotEmpty()) { // if user selected some transition to track
                val request = ActivityTransitionRequest(transitions)
                activityRecognitionClient.requestActivityTransitionUpdates(request, pendingIntent)
                    .addOnSuccessListener {
                        prefs.edit {
                            putBoolean(TRACK_CAR, carLayout.isSelected)
                            .putBoolean(TRACK_BICYCLE, bicycleLayout.isSelected)
                            .putBoolean(TRACK_RUNNING, runningLayout.isSelected)
                        }
                        Log.d("Transition", "Started successfully")
                }.addOnFailureListener {
                    Log.e("Transition", "Failed to start updates")
                }
            } else {
                Log.d("Transition", "User disabled activity updates")
            }
        }
    }

    private fun getTransitions(): List<ActivityTransition> {
        val transitions = mutableListOf<ActivityTransition>()

        if (carLayout.isSelected)
            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

        if (bicycleLayout.isSelected)
            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_BICYCLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

        if (runningLayout.isSelected)
            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

        return transitions.toList()
    }

    private var pendingTripsGoal: Int = 0
    private var pendingDistanceGoal: Int = 0
    private var pendingInactivityDays: Int = 0

    private fun saveSettings(tripsGoal: Int, distanceGoal: Int, inactivityDays: Int) {
        val prefs = requireContext().getSharedPreferences("goals", Context.MODE_PRIVATE)

        prefs.edit {
            putInt("monthlyTripsGoal", tripsGoal)
            putInt("monthlyDistanceGoal", distanceGoal)
        }
        val settingsPrefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        settingsPrefs.edit { putInt("inactivity_days", inactivityDays) }

        if (inactivityDays == 0) {
            WorkManager.getInstance(requireContext()).cancelUniqueWork("inactivity_reminder")
        } else {
            // Comment out the following if you want to test notifications
            // (Re)enqueue the worker with the new settings
            val workRequest = PeriodicWorkRequestBuilder<InactivityReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "inactivity_reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            // Uncomment the following if you want to test notifications (will take 1 minute to trigger)
            /*
            val workRequest = OneTimeWorkRequestBuilder<InactivityReminderWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            context?.let { it1 -> WorkManager.getInstance(it1).enqueue(workRequest) }
            */
        }
    }
}