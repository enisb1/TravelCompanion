package com.example.travelcompanion.ui.home.start

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.travelcompanion.MainActivity
import com.example.travelcompanion.R
import com.example.travelcompanion.db.locations.TripLocation
import com.example.travelcompanion.util.TRACKING_CHANNEL_ID
import com.example.travelcompanion.util.TRACKING_CHANNEL_NAME
import com.example.travelcompanion.util.TRACKING_NOTIFICATION_ID
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.util.Date


class TrackingService : Service() {

    companion object {
        // TODO: Incrementa distanza tra posizioni prima di consegnare
        const val MINIMUM_DISTANCE_BETWEEN_LOCATIONS = 1   // meters
        val ACTION_STOP = "ACTION_STOP"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var incrementTimerThread: Thread

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        val locationRequest = LocationRequest.Builder(5000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        var previousLocation: Location? = null
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val tripLocation = TripLocation(
                        id = 0,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = Date().time
                    )
                    if (previousLocation != null) { // check distance from previous location if possible
                        val distance = SphericalUtil.computeDistanceBetween(
                            LatLng(previousLocation!!.latitude, previousLocation!!.longitude),
                            LatLng(location.latitude, location.longitude))
                        Log.i("Tracking", distance.toString())
                        if (distance >= MINIMUM_DISTANCE_BETWEEN_LOCATIONS) {
                            TrackingRepository.addLocation(tripLocation)
                            TrackingRepository.incrementDistance(distance)
                        }

                    }
                    else
                        TrackingRepository.addLocation(tripLocation)
                    previousLocation = location
                }
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        startTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.i("Tracking", "stop")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = createNotification()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(TRACKING_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else
            startForeground(TRACKING_NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun startTimer() {
        incrementTimerThread = Thread {
            while (true) {
                try {
                    Thread.sleep(1000)
                    TrackingRepository.incrementTimerValue()
                } catch (e: InterruptedException) { // exception thrown by the interrupt() function
                    break
                }
            }
        }
        incrementTimerThread.start()
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TRACKING_CHANNEL_ID, TRACKING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // create pending intent to navigate to start when clicking the notification
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to_start", true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP // else it would be restarted even if on top of the stack
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, TRACKING_CHANNEL_ID)
            .setOngoing(true)
            .setContentTitle(getString(R.string.tracking_your_trip))
            .setContentText(getString(R.string.go_to_the_app_to_see_your_path))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)  // reduces delay in showing the notification
            .build()
    }

    // not a bound service
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
        // stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)
        // stop incrementing timer
        incrementTimerThread.interrupt()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}