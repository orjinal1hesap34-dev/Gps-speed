package com.example

import android.location.Location
import com.example.location.GpsSpeedProcessor
import com.example.model.GpsSignalStatus
import com.example.model.SpeedUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

    @Test
    fun testSpeedUnitConversions() {
        val speedMps = 27.7778f // exactly 100 km/h

        val kmh = SpeedUnit.KMH.formatSpeed(speedMps)
        assertEquals("100", kmh)

        val mph = SpeedUnit.MPH.formatSpeed(speedMps)
        assertEquals("62", mph)

        val mps = SpeedUnit.MPS.formatSpeed(speedMps)
        assertEquals("27.8", mps)
    }

    @Test
    fun testGpsSpeedProcessorRejectsLowAccuracy() {
        val processor = GpsSpeedProcessor(maxAcceptableAccuracyMeters = 25f)
        val poorLocation = Location("gps").apply {
            latitude = 41.0082
            longitude = 28.9784
            accuracy = 45f // Unreliable accuracy
            time = System.currentTimeMillis()
            speed = 25f
        }

        val result = processor.processLocation(poorLocation)
        assertTrue(result.status is GpsSignalStatus.Unreliable)
    }

    @Test
    fun testGpsSpeedProcessorUsesHardwareGnssSpeed() {
        val processor = GpsSpeedProcessor(maxAcceptableAccuracyMeters = 25f)
        val gnssLocation = Location("gps").apply {
            latitude = 41.0082
            longitude = 28.9784
            accuracy = 3.5f // Excellent lock
            time = System.currentTimeMillis()
            speed = 22.22f // ~80 km/h Doppler GNSS speed
        }

        val result = processor.processLocation(gnssLocation)
        assertTrue(result.status is GpsSignalStatus.Locked)
        assertTrue(result.isHardwareGnss)
        // Smoothed speed on initial fix should be near raw speed
        assertTrue(result.speedMps > 20f)
    }

    @Test
    fun testGpsSpeedProcessorStationaryThreshold() {
        val processor = GpsSpeedProcessor()
        val stationaryLocation = Location("gps").apply {
            latitude = 41.0082
            longitude = 28.9784
            accuracy = 4f
            time = System.currentTimeMillis()
            speed = 0.2f // < 0.45 m/s (~0.7 km/h) stationary drift
        }

        val result = processor.processLocation(stationaryLocation)
        assertEquals(0f, result.speedMps, 0.001f)
    }
}
