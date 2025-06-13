package com.example.travelcompanion.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.travelcompanion.MainActivity
import com.example.travelcompanion.ui.settings.SettingsFragment.Companion.TRACK_BICYCLE
import com.example.travelcompanion.ui.settings.SettingsFragment.Companion.TRACK_CAR
import com.example.travelcompanion.ui.settings.SettingsFragment.Companion.TRACK_RUNNING
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ActivityRecognitionReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "activity_recognition_channel"
        const val NOTIFICATION_ID = 459
    }

    override fun onReceive(context: Context, intent: Intent) {

        Log.i("myreceiver", intent.toString())
        //sendNotification(context, intent.action.toString())

        if (!TextUtils.equals("TRANSITIONS_RECEIVER_ACTION", intent.getAction())) {

            Log.i("myreceiver", "received intent with wrong action: " +
                    intent.action
            );
            return;
        }

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)!!
            Log.i("myreceiver", result.transitionEvents.toString())
            val transitionEvent = result.transitionEvents[result.transitionEvents.size - 1]

            val prefs = context.getSharedPreferences("goals", 0)   //TODO: change to name "settings"
            val trackCar = prefs.getBoolean(TRACK_CAR, false)
            val trackBicycle = prefs.getBoolean(TRACK_BICYCLE, false)
            val trackRunning = prefs.getBoolean(TRACK_RUNNING, false)

            for (event in result.transitionEvents) {
                val info = "Transition: " + event.activityType +
                        " (" + event.transitionType + ")" + "   " +
                        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

                Log.i("myreceiver", info)
            }

            when (transitionEvent.activityType) {
                DetectedActivity.IN_VEHICLE -> {
                    if (trackCar)
                        Log.i("myreceiver", "User started driving")
                    sendNotification(context, "Driving")
                }
                DetectedActivity.ON_BICYCLE -> {
                    if (trackBicycle)
                        Log.i("myreceiver", "User started cycling")
                    sendNotification(context, "Cycling")
                }
                DetectedActivity.RUNNING -> {
                    if (trackRunning)
                        Log.i("myreceiver", "User started running")
                    sendNotification(context, "Running")
                }
                DetectedActivity.WALKING -> {
                    if (trackRunning)
                        Log.i("myreceiver", "User started running")
                    sendNotification(context, "Walking")
                }
                DetectedActivity.STILL -> {
                    if (trackRunning)
                        Log.i("myreceiver", "User started running")
                    sendNotification(context, "Still")
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