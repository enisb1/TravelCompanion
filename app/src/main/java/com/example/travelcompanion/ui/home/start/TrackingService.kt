package com.example.travelcompanion.ui.home.start

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.travelcompanion.R
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil


class TrackingService : Service() {

    companion object {
        const val MINIMUM_DISTANCE_BETWEEN_LOCATIONS = 50   // meters
        val NOTIFICATION_ID = 1
        val CHANNEL_ID = "location_tracking_service"
        val CHANNEL_NAME = "Location tracking channel"
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        val locationRequest = LocationRequest.Builder(5000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        var lastLocation: Location? = null
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    if (lastLocation != null) {
                        val distance = SphericalUtil.computeDistanceBetween(
                            LatLng(lastLocation!!.latitude, lastLocation!!.longitude),
                            LatLng(location.latitude, location.longitude))
                        Log.i("Tracking", distance.toString())
                        if (distance >= MINIMUM_DISTANCE_BETWEEN_LOCATIONS)
                            TrackingRepository.addLocation(location)
                    }
                    else
                        TrackingRepository.addLocation(location)
                    lastLocation = location
                }
            }
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else
            startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)   //TODO: it's dismissable on API 35, check with lower versions
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.tracking_service_notification_description))
            .setSmallIcon(R.drawable.ic_stop)   //TODO: replace with app's icon
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)  // reduces delay in showing the notification
            .build()
    }

    // not a bound service
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}