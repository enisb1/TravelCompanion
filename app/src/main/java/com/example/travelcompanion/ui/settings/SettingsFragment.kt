package com.example.travelcompanion.ui.settings

import android.content.Context
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Toast
import com.example.travelcompanion.R
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.travelcompanion.workers.InactivityReminderWorker
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder

class SettingsFragment : Fragment() {

    private val notificationPermissionRequestCode = 1001

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

        val numberPicker = view.findViewById<NumberPicker>(R.id.np_inactivity_days)
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
            prefs.edit {
                putInt("monthlyTripsGoal", tripsGoal)
                    .putInt("monthlyDistanceGoal", distanceGoal)
            }

            val settingsPrefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
            settingsPrefs.edit { putInt("inactivity_days", selectedInactivityDays) }

            if (selectedInactivityDays == 0) {
                // Turn off reminder
                WorkManager.getInstance(requireContext()).cancelUniqueWork("inactivity_reminder")
                Toast.makeText(requireContext(), "Reminder turned off.", Toast.LENGTH_SHORT).show()
            } else {
                // Requests notification permission if not already granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        notificationPermissionRequestCode
                    )
                }

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


                Toast.makeText(requireContext(), "Saved new settings", Toast.LENGTH_SHORT).show()
            }
        }

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "inactivity_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}