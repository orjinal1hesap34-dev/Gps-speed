package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.example.model.GpsSignalStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationTracker(
    private val context: Context,
    private val onLocationUpdate: (Location) -> Unit,
    private val onStatusUpdate: (GpsSignalStatus) -> Unit
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private var isTracking = false

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            onLocationUpdate(location)
        }
    }

    private val directGpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocationUpdate(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) {
                onStatusUpdate(GpsSignalStatus.Searching)
            }
        }

        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) {
                onStatusUpdate(GpsSignalStatus.GpsDisabled)
            }
        }
    }

    fun isGpsEnabled(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isTracking) return

        if (!isGpsEnabled()) {
            onStatusUpdate(GpsSignalStatus.GpsDisabled)
            return
        }

        onStatusUpdate(GpsSignalStatus.Searching)
        isTracking = true

        try {
            // High accuracy fused location request (500ms interval for agile vehicle speed updates)
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                .setMinUpdateIntervalMillis(250L)
                .setMinUpdateDistanceMeters(0f)
                .setMaxUpdateDelayMillis(1000L)
                .build()

            fusedClient.requestLocationUpdates(
                locationRequest,
                fusedCallback,
                Looper.getMainLooper()
            )

            // Direct GPS provider listener fallback to ensure pure GNSS updates
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500L,
                0f,
                directGpsListener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationTracker", "Permission missing for location tracking", e)
            onStatusUpdate(GpsSignalStatus.PermissionDenied)
            isTracking = false
        } catch (e: Exception) {
            Log.e("LocationTracker", "Error starting location tracking", e)
        }
    }

    fun stop() {
        if (!isTracking) return
        isTracking = false
        try {
            fusedClient.removeLocationUpdates(fusedCallback)
            locationManager?.removeUpdates(directGpsListener)
        } catch (e: Exception) {
            Log.e("LocationTracker", "Error stopping location tracking", e)
        }
    }
}
