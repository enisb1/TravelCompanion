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
import androidx.core.app.TaskStackBuilder
import com.example.travelcompanion.MainActivity
import com.example.travelcompanion.R
import com.example.travelcompanion.util.INACTIVITY_CHANNEL_ID
import com.example.travelcompanion.util.INACTIVITY_CHANNEL_NAME
import com.example.travelcompanion.util.INACTIVITY_NOTIFICATION_ID

class InactivityReminderWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val now = System.currentTimeMillis()
        val prefs = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val inactivityDays = prefs.getInt("inactivity_days", -1)
        val lastJourneyTime = prefs.getLong("last_journey_time", now)
        val daysSinceLast = (now - lastJourneyTime) / (1000 * 60 * 60 * 24)

        if (inactivityDays > 0 && daysSinceLast >= inactivityDays) {

            // Create notification channel if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    INACTIVITY_CHANNEL_ID,
                    INACTIVITY_CHANNEL_NAME,
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

            val builder = NotificationCompat.Builder(applicationContext, INACTIVITY_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Inactivity Reminder")
                .setContentText("You haven't logged any trips in a while. Remember to add one!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(INACTIVITY_NOTIFICATION_ID, builder.build())
        }
        return Result.success()
    }
}