package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SpeedRepository
import com.example.location.LocationTracker
import com.example.model.GpsSignalStatus
import com.example.model.SpeedTelemetry
import com.example.model.SpeedUnit
import com.example.model.TestState
import com.example.service.SpeedTrackingService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpeedViewModel(application: Application) : AndroidViewModel(application) {

    val telemetry: StateFlow<SpeedTelemetry> = SpeedRepository.telemetry
    val selectedUnit: StateFlow<SpeedUnit> = SpeedRepository.selectedUnit
    val keepScreenOn: StateFlow<Boolean> = SpeedRepository.keepScreenOn

    private var inAppTracker: LocationTracker? = null

    init {
        inAppTracker = LocationTracker(
            context = getApplication(),
            onLocationUpdate = { location ->
                SpeedRepository.onLocationReceived(location)
            },
            onStatusUpdate = { status ->
                SpeedRepository.updateSignalStatus(status)
            }
        )
    }

    fun onPermissionsGranted() {
        inAppTracker?.start()
    }

    fun onPermissionsDenied() {
        SpeedRepository.updateSignalStatus(GpsSignalStatus.PermissionDenied)
    }

    fun checkGpsProvider() {
        val tracker = inAppTracker ?: return
        if (!tracker.isGpsEnabled()) {
            SpeedRepository.updateSignalStatus(GpsSignalStatus.GpsDisabled)
        } else if (telemetry.value.signalStatus is GpsSignalStatus.GpsDisabled) {
            SpeedRepository.updateSignalStatus(GpsSignalStatus.Searching)
            tracker.start()
        }
    }

    fun startTest(context: Context) {
        SpeedRepository.startTest()
        SpeedTrackingService.startTracking(context)
    }

    fun pauseTest(context: Context) {
        SpeedRepository.pauseTest()
        SpeedTrackingService.pauseTracking(context)
    }

    fun resumeTest(context: Context) {
        SpeedRepository.resumeTest()
        SpeedTrackingService.resumeTracking(context)
    }

    fun resetTest(context: Context) {
        SpeedRepository.resetTest()
        SpeedTrackingService.stopTracking(context)
    }

    fun setUnit(unit: SpeedUnit) {
        SpeedRepository.setUnit(unit)
    }

    fun toggleKeepScreenOn() {
        SpeedRepository.setKeepScreenOn(!keepScreenOn.value)
    }

    override fun onCleared() {
        super.onCleared()
        inAppTracker?.stop()
    }
}
