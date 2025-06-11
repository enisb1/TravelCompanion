package com.example.travelcompanion.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class InactivityReminderWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val inactivityDays = prefs.getInt("inactivity_days", 3)
        val lastJourneyTime = prefs.getLong("last_journey_time", 0L)
        val now = System.currentTimeMillis()
        val daysSinceLast = (now - lastJourneyTime) / (1000 * 60 * 60 * 24)
        if (daysSinceLast >= inactivityDays) {
            val channelId = "inactivity_reminder_channel"
            val channelName = "Inactivity Reminder"
            val notificationId = 1

            // Create notification channel if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Inactivity Reminder")
                .setContentText("You haven't logged any trips in a while. Remember to add one!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, builder.build())
        }
        return Result.success()
    }
}