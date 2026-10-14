package com.example.data

import android.location.Location
import com.example.location.GpsSpeedProcessor
import com.example.model.GpsSignalStatus
import com.example.model.SpeedTelemetry
import com.example.model.SpeedUnit
import com.example.model.TestState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object SpeedRepository {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _telemetry = MutableStateFlow(SpeedTelemetry())
    val telemetry: StateFlow<SpeedTelemetry> = _telemetry.asStateFlow()

    private val _selectedUnit = MutableStateFlow(SpeedUnit.KMH)
    val selectedUnit: StateFlow<SpeedUnit> = _selectedUnit.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(true)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val gpsProcessor = GpsSpeedProcessor()

    private var timerJob: Job? = null
    private var watchdogJob: Job? = null

    // Acceleration timing state (in ms)
    private var launchStartTimeMs: Long? = null
    private var zeroToFiftyDone = false
    private var zeroToHundredDone = false

    init {
        startGpsWatchdog()
    }

    fun setUnit(unit: SpeedUnit) {
        _selectedUnit.value = unit
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
    }

    fun updateSignalStatus(status: GpsSignalStatus) {
        _telemetry.update { current ->
            current.copy(signalStatus = status)
        }
    }

    fun onLocationReceived(location: Location) {
        val result = gpsProcessor.processLocation(location)
        val now = System.currentTimeMillis()

        _telemetry.update { current ->
            val isRunning = current.testState == TestState.RUNNING

            val newMax = if (isRunning && result.status is GpsSignalStatus.Locked) {
                maxOf(current.maxSpeedMps, result.speedMps)
            } else current.maxSpeedMps

            val newDistance = if (isRunning && result.status is GpsSignalStatus.Locked) {
                current.totalDistanceMeters + result.distanceIncrementMeters
            } else current.totalDistanceMeters

            // Acceleration run tracking (0-50, 0-100 km/h)
            handleAccelerationTracking(result.speedMps, isRunning, now)

            val avg = if (isRunning && current.elapsedSeconds > 0) {
                (newDistance / current.elapsedSeconds).toFloat()
            } else current.avgSpeedMps

            current.copy(
                currentSpeedMps = result.speedMps,
                maxSpeedMps = newMax,
                avgSpeedMps = avg,
                totalDistanceMeters = newDistance,
                signalStatus = result.status,
                lastFixTimestamp = now
            )
        }
    }

    private fun handleAccelerationTracking(speedMps: Float, isRunning: Boolean, nowMs: Long) {
        if (!isRunning) {
            launchStartTimeMs = null
            return
        }

        val speedKmh = speedMps * 3.6f

        // Launch detection: stationary or slow start
        if (speedKmh < 1.5f) {
            launchStartTimeMs = null
            zeroToFiftyDone = false
            zeroToHundredDone = false
        } else if (launchStartTimeMs == null && speedKmh >= 2.0f && speedKmh < 15.0f) {
            launchStartTimeMs = nowMs
            zeroToFiftyDone = false
            zeroToHundredDone = false
        }

        launchStartTimeMs?.let { start ->
            val elapsed = (nowMs - start) / 1000.0
            if (speedKmh >= 50.0f && !zeroToFiftyDone) {
                zeroToFiftyDone = true
                _telemetry.update { it.copy(zeroToFiftyTimeSec = elapsed) }
            }
            if (speedKmh >= 100.0f && !zeroToHundredDone) {
                zeroToHundredDone = true
                _telemetry.update { it.copy(zeroToHundredTimeSec = elapsed) }
            }
        }
    }

    fun startTest() {
        _telemetry.update {
            it.copy(
                testState = TestState.RUNNING,
                maxSpeedMps = it.currentSpeedMps,
                totalDistanceMeters = 0.0,
                elapsedSeconds = 0L,
                movingSeconds = 0L,
                avgSpeedMps = 0f,
                zeroToFiftyTimeSec = null,
                zeroToHundredTimeSec = null
            )
        }
        gpsProcessor.reset()
        launchStartTimeMs = null
        zeroToFiftyDone = false
        zeroToHundredDone = false
        startTimer()
    }

    fun pauseTest() {
        _telemetry.update { it.copy(testState = TestState.PAUSED) }
        stopTimer()
    }

    fun resumeTest() {
        _telemetry.update { it.copy(testState = TestState.RUNNING) }
        startTimer()
    }

    fun resetTest() {
        stopTimer()
        gpsProcessor.reset()
        launchStartTimeMs = null
        zeroToFiftyDone = false
        zeroToHundredDone = false
        _telemetry.update {
            it.copy(
                testState = TestState.IDLE,
                maxSpeedMps = 0f,
                avgSpeedMps = 0f,
                totalDistanceMeters = 0.0,
                elapsedSeconds = 0L,
                movingSeconds = 0L,
                zeroToFiftyTimeSec = null,
                zeroToHundredTimeSec = null
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                _telemetry.update { current ->
                    if (current.testState == TestState.RUNNING) {
                        val newElapsed = current.elapsedSeconds + 1
                        val newMoving = if (current.currentSpeedMps > 0.5f) current.movingSeconds + 1 else current.movingSeconds
                        val avg = if (newElapsed > 0) (current.totalDistanceMeters / newElapsed).toFloat() else 0f
                        current.copy(
                            elapsedSeconds = newElapsed,
                            movingSeconds = newMoving,
                            avgSpeedMps = avg
                        )
                    } else current
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun startGpsWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                delay(2000L)
                val current = _telemetry.value
                val now = System.currentTimeMillis()
                // If last fix was more than 4.5 seconds ago and we were locked, report searching signal
                if (current.signalStatus is GpsSignalStatus.Locked && (now - current.lastFixTimestamp > 4500L)) {
                    _telemetry.update {
                        it.copy(
                            signalStatus = GpsSignalStatus.Searching,
                            currentSpeedMps = 0f // Signal lost
                        )
                    }
                }
            }
        }
    }
}
