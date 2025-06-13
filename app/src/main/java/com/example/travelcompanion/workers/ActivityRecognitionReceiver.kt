package com.example.travelcompanion.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.travelcompanion.MainActivity
import com.example.travelcompanion.ui.settings.SettingsFragment.Companion.TRACK_BICYCLE
import com.example.travelcompanion.ui.settings.SettingsFragment.Companion.TRACK_CAR
import com.example.travelcompanion.ui.settings.SettingsFragment.Companion.TRACK_RUNNING
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "activity_recognition_channel"
        const val NOTIFICATION_ID = 459
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)!!
            // result.transitionEvents = list with updates about the activity transition
            // transitionEvent = last updated activity transition
            val transitionEvent = result.transitionEvents[result.transitionEvents.size - 1]

            val prefs = context.getSharedPreferences("goals", 0)   //TODO: change to name "settings"
            val trackCar = prefs.getBoolean(TRACK_CAR, false)
            val trackBicycle = prefs.getBoolean(TRACK_BICYCLE, false)
            val trackRunning = prefs.getBoolean(TRACK_RUNNING, false)

            when (transitionEvent.activityType) {
                DetectedActivity.IN_VEHICLE -> {
                    if (trackCar)
                        sendNotification(context, "Driving")
                }
                DetectedActivity.ON_BICYCLE -> {
                    if (trackBicycle)
                        sendNotification(context, "Cycling")
                }
                DetectedActivity.RUNNING -> {
                    if (trackRunning)
                        sendNotification(context, "Running")
                }
            }
        }
    }

    private fun sendNotification(context: Context, activity: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Activity Recognition",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val goToStartIntent = Intent(context, MainActivity::class.java)
            .putExtra("navigate_to_start", true)
        val pendingIntent = PendingIntent.getActivity(
            context,
            432,
            goToStartIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$activity detected")
            .setContentText("Start tracking your trip!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}