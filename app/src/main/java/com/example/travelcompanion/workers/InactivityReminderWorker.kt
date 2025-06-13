package com.example.travelcompanion.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.TaskStackBuilder
import com.example.travelcompanion.MainActivity

class InactivityReminderWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val now = System.currentTimeMillis()
        val prefs = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val inactivityDays = prefs.getInt("inactivity_days", -1)
        val lastJourneyTime = prefs.getLong("last_journey_time", now)
        val daysSinceLast = (now - lastJourneyTime) / (1000 * 60 * 60 * 24)
        Log.d("InactivityReminderWorker", "Days since last journey: $daysSinceLast, Inactivity threshold: $inactivityDays, Last journey time: $lastJourneyTime")
        if (inactivityDays > 0 && daysSinceLast >= inactivityDays) {
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

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                putExtra("navigate_to_start", true)
            }
            val pendingIntent = TaskStackBuilder.create(applicationContext).run {
                addNextIntentWithParentStack(intent)
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }

            val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Inactivity Reminder")
                .setContentText("You haven't logged any trips in a while. Remember to add one!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, builder.build())
        }
        return Result.success()
    }
}