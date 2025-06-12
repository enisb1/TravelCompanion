package com.example.travelcompanion.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import com.example.travelcompanion.ui.settings.SettingsFragment.Companion.TRACK_BICYCLE
import com.example.travelcompanion.ui.settings.SettingsFragment.Companion.TRACK_CAR
import com.example.travelcompanion.ui.settings.SettingsFragment.Companion.TRACK_RUNNING
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity


class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            val activity = result!!.mostProbableActivity

            val type = activity.type
            val confidence = activity.confidence

            val prefs = context.getSharedPreferences("goals", 0)   //TODO: change to name "settings"
            val trackCar = prefs.getBoolean(TRACK_CAR, false)
            val trackBicycle = prefs.getBoolean(TRACK_BICYCLE, false)
            val trackRunning = prefs.getBoolean(TRACK_RUNNING, false)

            Log.i("receiver", type.toString())
            Log.i("receiver", trackCar.toString())
            Log.i("receiver", trackBicycle.toString())
            Log.i("receiver", trackRunning.toString())

            if (confidence > 70) {
                if (type == DetectedActivity.IN_VEHICLE && trackCar) {
                    // Launch desired action, activity, or service here
                    Log.i("Activity", "User started driving")
                }
                else if (type == DetectedActivity.RUNNING && trackBicycle) {
                    // Launch desired action, activity, or service here
                    Log.i("Activity", "User started running")
                }
                else if (type == DetectedActivity.ON_BICYCLE && trackRunning) {
                    // Launch desired action, activity, or service here
                    Log.i("Activity", "User started cycling")
                }
            }
        }
    }
}