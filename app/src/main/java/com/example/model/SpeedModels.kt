package com.example.model

/**
 * Supported units for vehicle and motorcycle speed measurement.
 */
enum class SpeedUnit(
    val label: String,
    val conversionFactor: Float,
    val distanceUnit: String,
    val distanceFactor: Float
) {
    KMH(label = "km/sa", conversionFactor = 3.6f, distanceUnit = "km", distanceFactor = 0.001f),
    MPH(label = "mph", conversionFactor = 2.23694f, distanceUnit = "mil", distanceFactor = 0.000621371f),
    MPS(label = "m/sn", conversionFactor = 1.0f, distanceUnit = "m", distanceFactor = 1.0f);

    fun formatSpeed(speedMps: Float): String {
        val converted = speedMps * conversionFactor
        return when (this) {
            KMH, MPH -> String.format(java.util.Locale.US, "%.0f", converted)
            MPS -> String.format(java.util.Locale.US, "%.1f", converted)
        }
    }

    fun formatSpeedDecimal(speedMps: Float): String {
        val converted = speedMps * conversionFactor
        return String.format(java.util.Locale.US, "%.1f", converted)
    }

    fun formatDistance(distanceMeters: Double): String {
        return when (this) {
            KMH -> {
                val km = distanceMeters / 1000.0
                if (km < 1.0) {
                    "${distanceMeters.toInt()} m"
                } else {
                    String.format(java.util.Locale.US, "%.2f km", km)
                }
            }
            MPH -> {
                val miles = distanceMeters * 0.000621371
                String.format(java.util.Locale.US, "%.2f mil", miles)
            }
            MPS -> {
                "${distanceMeters.toInt()} m"
            }
        }
    }
}

/**
 * GPS signal reception and accuracy status.
 * Ensures the app never shows fake speeds or zero when signal is invalid.
 */
sealed interface GpsSignalStatus {
    data object PermissionDenied : GpsSignalStatus
    data object GpsDisabled : GpsSignalStatus
    data object Searching : GpsSignalStatus
    data class Unreliable(val accuracyMeters: Float) : GpsSignalStatus
    data class Locked(val accuracyMeters: Float, val isGnssHardware: Boolean) : GpsSignalStatus
}

/**
 * State of the speed test execution.
 */
enum class TestState {
    IDLE,
    RUNNING,
    PAUSED
}

/**
 * Real-time telemetry snapshot of the test run.
 */
data class SpeedTelemetry(
    val currentSpeedMps: Float = 0f,
    val maxSpeedMps: Float = 0f,
    val avgSpeedMps: Float = 0f,
    val totalDistanceMeters: Double = 0.0,
    val elapsedSeconds: Long = 0L,
    val movingSeconds: Long = 0L,
    val signalStatus: GpsSignalStatus = GpsSignalStatus.Searching,
    val testState: TestState = TestState.IDLE,
    val zeroToFiftyTimeSec: Double? = null,
    val zeroToHundredTimeSec: Double? = null,
    val lastFixTimestamp: Long = 0L
)
