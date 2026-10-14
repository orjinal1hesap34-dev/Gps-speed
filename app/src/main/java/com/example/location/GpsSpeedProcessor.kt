package com.example.location

import android.location.Location
import com.example.model.GpsSignalStatus

data class ProcessedLocationResult(
    val speedMps: Float,
    val distanceIncrementMeters: Double,
    val status: GpsSignalStatus,
    val isHardwareGnss: Boolean,
    val accuracyMeters: Float
)

class GpsSpeedProcessor(
    private val maxAcceptableAccuracyMeters: Float = 25.0f,
    private val alphaSmoothingFactor: Float = 0.85f,
    private val stationaryThresholdMps: Float = 0.45f // ~1.6 km/h, eliminates stopped GPS jitter
) {
    private var lastValidLocation: Location? = null
    private var smoothedSpeedMps: Float = 0f
    private var lastValidTimestamp: Long = 0L

    fun reset() {
        lastValidLocation = null
        smoothedSpeedMps = 0f
        lastValidTimestamp = 0L
    }

    fun processLocation(location: Location): ProcessedLocationResult {
        val accuracy = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE

        // 1. Accuracy Filter: Reject low-accuracy / multipath reflections
        if (accuracy > maxAcceptableAccuracyMeters) {
            return ProcessedLocationResult(
                speedMps = 0f,
                distanceIncrementMeters = 0.0,
                status = GpsSignalStatus.Unreliable(accuracy),
                isHardwareGnss = false,
                accuracyMeters = accuracy
            )
        }

        val currentTime = location.time
        val prevLocation = lastValidLocation

        // 2. Determine raw speed in meters per second
        var rawSpeedMps: Float
        var isGnssHardware = false

        if (location.hasSpeed() && location.speed >= 0f) {
            // Priority 1: Hardware GNSS Doppler speed directly from GPS chipset
            rawSpeedMps = location.speed
            isGnssHardware = true
        } else if (prevLocation != null) {
            // Priority 2: Geodesic distance / elapsed time derivative
            val timeDiffSec = (currentTime - prevLocation.time) / 1000.0
            if (timeDiffSec in 0.1..10.0) {
                val distanceMeters = prevLocation.distanceTo(location)
                rawSpeedMps = (distanceMeters / timeDiffSec).toFloat()
            } else {
                rawSpeedMps = 0f
            }
        } else {
            rawSpeedMps = 0f
        }

        // 3. Stationary Deadband: Prevent drift when at a standstill
        if (rawSpeedMps < stationaryThresholdMps) {
            rawSpeedMps = 0f
        }

        // 4. Outlier Spike Filter: Reject physically impossible acceleration (> 22 m/s² ~ 80 km/h jump in 1s)
        if (prevLocation != null && lastValidTimestamp > 0L) {
            val dt = (currentTime - lastValidTimestamp) / 1000.0
            if (dt in 0.2..5.0) {
                val acceleration = kotlin.math.abs(rawSpeedMps - smoothedSpeedMps) / dt
                if (acceleration > 22.0) {
                    // Unrealistic speed jump (e.g. GPS tunnel exit multipath) - clamp to maximum feasible change
                    val maxDelta = (22.0 * dt).toFloat()
                    rawSpeedMps = if (rawSpeedMps > smoothedSpeedMps) {
                        smoothedSpeedMps + maxDelta
                    } else {
                        (smoothedSpeedMps - maxDelta).coerceAtLeast(0f)
                    }
                }
            }
        }

        // 5. Exponential Smoothing Filter (alpha = 0.85): Immediate real-world response without jitter
        smoothedSpeedMps = if (smoothedSpeedMps == 0f && rawSpeedMps > 0f) {
            rawSpeedMps
        } else {
            alphaSmoothingFactor * rawSpeedMps + (1f - alphaSmoothingFactor) * smoothedSpeedMps
        }

        if (smoothedSpeedMps < 0.15f) {
            smoothedSpeedMps = 0f
        }

        // 6. Calculate distance increment
        val distanceIncrement = if (prevLocation != null && smoothedSpeedMps > 0f) {
            val dist = prevLocation.distanceTo(location).toDouble()
            // Ignore distance if accuracy radius is significantly larger than movement
            if (dist > 0.5) dist else 0.0
        } else {
            0.0
        }

        lastValidLocation = location
        lastValidTimestamp = currentTime

        return ProcessedLocationResult(
            speedMps = smoothedSpeedMps,
            distanceIncrementMeters = distanceIncrement,
            status = GpsSignalStatus.Locked(accuracy, isGnssHardware),
            isHardwareGnss = isGnssHardware,
            accuracyMeters = accuracy
        )
    }
}
