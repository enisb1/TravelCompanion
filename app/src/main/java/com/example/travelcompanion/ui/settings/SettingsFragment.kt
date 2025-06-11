package com.example.travelcompanion.ui.settings

import android.content.Context
import android.os.Bundle
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

class SettingsFragment : Fragment() {

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
        val workRequest = PeriodicWorkRequestBuilder<InactivityReminderWorker>(1, TimeUnit.DAYS).build()


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
            Toast.makeText(requireContext(), "Objectives set!", Toast.LENGTH_SHORT).show()
        }

        numberPicker.setOnValueChangedListener { _, _, newVal ->
            val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit().putInt("inactivity_days", newVal).apply()
        }

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "inactivity_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}