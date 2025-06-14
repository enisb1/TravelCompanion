package com.example.travelcompanion.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.travelcompanion.R
import com.example.travelcompanion.workers.ActivityRecognitionReceiver
import com.example.travelcompanion.workers.InactivityReminderWorker
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    companion object {
        const val TRACK_CAR = "trackCar"
        const val TRACK_BICYCLE = "trackBicycle"
        const val TRACK_RUNNING = "trackRunning"
    }

    private val viewModel: SettingsViewModel by viewModels()

    private val notificationPermissionRequestCode = 1001

    private lateinit var activityRecognitionPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var notificationPermissionLauncherToActivityRecognition: ActivityResultLauncher<String>

    private lateinit var prefs: SharedPreferences

    private lateinit var carLayout: FrameLayout
    private lateinit var bicycleLayout: FrameLayout
    private lateinit var runningLayout: FrameLayout
    private lateinit var numberPicker: NumberPicker
    private lateinit var etTrips: EditText
    private lateinit var etDistance: EditText

    // booleans for toast message
    private var activityRecognitionSet = false

    private var pendingTripsGoal: Int = 0
    private var pendingDistanceGoal: Int = 0
    private var pendingInactivityDays: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        // don't keep state when navigating outside the fragment
        if (requireActivity().isChangingConfigurations.not()) {
            viewModel.resetData()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireContext().getSharedPreferences("settings", 0)
        etTrips = view.findViewById(R.id.etMonthlyTripsGoal)
        etDistance = view.findViewById(R.id.etMonthlyDistanceGoal)
        numberPicker = view.findViewById(R.id.np_inactivity_days)
        val btnSave = view.findViewById<Button>(R.id.btnSaveGoals)
        carLayout = view.findViewById(R.id.car_layout)
        bicycleLayout = view.findViewById(R.id.bicycle_layout)
        runningLayout = view.findViewById(R.id.running_layout)

        initializePermissionLaunchers()

        val daysOptions = Array(31) { i -> if (i == 0) "Off" else i.toString() }
        numberPicker.minValue = 0
        numberPicker.maxValue = 30
        numberPicker.displayedValues = daysOptions

        val inactivityDays =
            if (viewModel.inactivityDays != 0)
                viewModel.inactivityDays
            else
                prefs.getInt("inactivity_days", 0)
        numberPicker.value = inactivityDays // 0 = Off, 1-30 = days
        viewModel.inactivityDays = inactivityDays

        val workRequest = PeriodicWorkRequestBuilder<InactivityReminderWorker>(1, TimeUnit.DAYS).build()


        val monthlyTripsGoal = viewModel.monthlyTripsGoal ?: prefs.getInt("monthlyTripsGoal", 0)
        val monthlyDistanceGoal = viewModel.monthlyDistanceGoal ?: prefs.getInt("monthlyDistanceGoal", 0)

        if(monthlyTripsGoal > 0)
            etTrips.setText(monthlyTripsGoal.toString())
        if(monthlyDistanceGoal > 0)
            etDistance.setText(monthlyDistanceGoal.toString())

        etTrips.addTextChangedListener {
            val tripsGoal = it.toString().toIntOrNull()
            viewModel.monthlyTripsGoal = tripsGoal
        }

        etDistance.addTextChangedListener {
            val distanceGoal = it.toString().toIntOrNull()
            viewModel.monthlyDistanceGoal = distanceGoal
        }

        numberPicker.setOnValueChangedListener { _, _, newVal ->
            viewModel.inactivityDays = newVal
        }

        btnSave.setOnClickListener {
            val tripsGoal = etTrips.text.toString().toIntOrNull() ?: 0
            val distanceGoal = etDistance.text.toString().toIntOrNull() ?: 0
            saveSettings(tripsGoal, distanceGoal)
        }
        
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "inactivity_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        // activity recognition
        val trackCar: Boolean = viewModel.isCarSelected ?: prefs.getBoolean(TRACK_CAR, false)
        viewModel.isCarSelected = trackCar

        val trackBicycle: Boolean = viewModel.isBicycleSelected ?: prefs.getBoolean(TRACK_BICYCLE, false)
        viewModel.isBicycleSelected = trackBicycle

        val trackRunning = viewModel.isRunningSelected ?: prefs.getBoolean(TRACK_RUNNING, false)
        viewModel.isRunningSelected = trackRunning

        if (trackCar)
            carLayout.isSelected = true
        if (trackBicycle)
            bicycleLayout.isSelected = true
        if (trackRunning)
            runningLayout.isSelected = true

        carLayout.setOnClickListener {
            it.isSelected = !it.isSelected
            viewModel.isCarSelected = it.isSelected
        }
        bicycleLayout.setOnClickListener {
            it.isSelected = !it.isSelected
            viewModel.isBicycleSelected = it.isSelected
        }
        runningLayout.setOnClickListener {
            it.isSelected = !it.isSelected
            viewModel.isRunningSelected = it.isSelected
        }
    }

    private fun saveSettings(tripsGoal: Int, distanceGoal: Int) {
        if (viewModel.inactivityDays == 0 && !needToLaunchActivityRecognition()) {
            // Turn off reminder
            WorkManager.getInstance(requireContext()).cancelUniqueWork("inactivity_reminder")
            saveGoalsAndInactivityDays(tripsGoal, distanceGoal, viewModel.inactivityDays)
            Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
        } else {
            // Requests notification permission if not already granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (viewModel.inactivityDays == 0) { // needToLaunchActivityRecognition() = true
                    activityRecognitionSet = true
                    saveGoalsAndInactivityDays(tripsGoal, distanceGoal, 0)
                    notificationPermissionLauncherToActivityRecognition.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else { // selectedInactivityDays > 0
                    pendingTripsGoal = tripsGoal
                    pendingDistanceGoal = distanceGoal
                    pendingInactivityDays = viewModel.inactivityDays
                    if (needToLaunchActivityRecognition())
                        activityRecognitionSet = true
                    notificationPermissionLauncherToActivityRecognition.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                saveGoalsAndInactivityDays(tripsGoal, distanceGoal, viewModel.inactivityDays)
                if (needToLaunchActivityRecognition())
                    permissionCheckForActivityRecognition()
                else {
                    prefs.edit {
                        putBoolean(TRACK_CAR, carLayout.isSelected)
                            .putBoolean(TRACK_BICYCLE, bicycleLayout.isSelected)
                            .putBoolean(TRACK_RUNNING, runningLayout.isSelected)
                    }
                    Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initializePermissionLaunchers() {
        activityRecognitionPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) @SuppressLint("MissingPermission") { isGranted ->
            if (isGranted) {
                startNewActivityRecognition()
                Toast.makeText(requireContext(), getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            }
            else {
                clearActivityRecognitionTrackingChoices()
                Toast.makeText(requireContext(), getString(R.string.activity_recognition_permission_missing), Toast.LENGTH_SHORT).show()
            }
        }

        notificationPermissionLauncherToActivityRecognition = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                if (pendingInactivityDays > 0) {
                    saveGoalsAndInactivityDays(pendingTripsGoal, pendingDistanceGoal, pendingInactivityDays)
                }
                if (activityRecognitionSet) {
                    permissionCheckForActivityRecognition()
                    activityRecognitionSet = false
                }
                else
                    Toast.makeText(requireContext(), getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.notification_permission_required), Toast.LENGTH_SHORT).show()
                numberPicker.value = 0  // reset inactivity days number picker
                clearActivityRecognitionTrackingChoices()   // reset selection of tracking choices
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

    // notification permission is already granted
    private fun permissionCheckForActivityRecognition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                startNewActivityRecognition()
                Toast.makeText(requireContext(), getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            }
        } else {    // no runtime permission needed
            startNewActivityRecognition()
            Toast.makeText(requireContext(), getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    private fun startNewActivityRecognition() {
        val intent  = Intent("TRANSITIONS_RECEIVER_ACTION")
            .setClass(requireContext(), ActivityRecognitionReceiver::class.java)
        val mActivityTransitionsPendingIntent =
            PendingIntent.getBroadcast(requireContext(), 70, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val activityRecognitionClient = ActivityRecognition.getClient(requireContext())

        activityRecognitionClient.removeActivityTransitionUpdates(mActivityTransitionsPendingIntent).addOnCompleteListener {
            val transitions = getTransitions()
            if (transitions.isNotEmpty()) { // if user selected some transition to track
                val request = ActivityTransitionRequest(transitions)
                activityRecognitionClient.requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent)
                    .addOnSuccessListener {
                        prefs.edit {
                            putBoolean(TRACK_CAR, carLayout.isSelected)
                            .putBoolean(TRACK_BICYCLE, bicycleLayout.isSelected)
                            .putBoolean(TRACK_RUNNING, runningLayout.isSelected)
                        }
                        Log.d("ActivityTransition", "Started successfully")
                }.addOnFailureListener {
                    Log.e("ActivityTransition", "Failed to start updates")
                }
            } else {
                Log.d("ActivityTransition", "User disabled activity updates")
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

    private fun saveGoalsAndInactivityDays(tripsGoal: Int, distanceGoal: Int, inactivityDays: Int) {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        prefs.edit {
            putInt("monthlyTripsGoal", tripsGoal)
            putInt("monthlyDistanceGoal", distanceGoal)
        }
        prefs.edit { putInt("inactivity_days", inactivityDays) }

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