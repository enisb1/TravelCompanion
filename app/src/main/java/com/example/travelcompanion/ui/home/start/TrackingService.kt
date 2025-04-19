package com.example.travelcompanion.ui.home.start

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil

class TrackingService : Service() {

    companion object {
        // metres between location
        val MINIMUM_DISTANCE_BETWEEN_LOCATIONS = 50
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        var lastLocation: Location? = null

        val locationRequest = LocationRequest.Builder(5000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
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

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}