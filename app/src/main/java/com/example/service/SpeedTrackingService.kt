package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SpeedRepository
import com.example.location.LocationTracker
import com.example.model.GpsSignalStatus
import com.example.model.TestState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpeedTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var locationTracker: LocationTracker? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationJob: Job? = null

    companion object {
        const val CHANNEL_ID = "motospeed_tracking_channel"
        const val NOTIFICATION_ID = 4040

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        fun startTracking(context: Context) {
            val intent = Intent(context, SpeedTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseTracking(context: Context) {
            val intent = Intent(context, SpeedTrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeTracking(context: Context) {
            val intent = Intent(context, SpeedTrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, SpeedTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MotoSpeed:GpsTrackingWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }

        locationTracker = LocationTracker(
            context = applicationContext,
            onLocationUpdate = { location ->
                SpeedRepository.onLocationReceived(location)
            },
            onStatusUpdate = { status ->
                SpeedRepository.updateSignalStatus(status)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                wakeLock?.acquire(3 * 60 * 60 * 1000L) // Safety 3h max
                locationTracker?.start()
                observeTelemetryForNotification()
            }
            ACTION_PAUSE -> {
                SpeedRepository.pauseTest()
                updateNotification()
            }
            ACTION_RESUME -> {
                SpeedRepository.resumeTest()
                updateNotification()
            }
            ACTION_STOP -> {
                stopTrackingService()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeTelemetryForNotification() {
        notificationJob?.cancel()
        notificationJob = serviceScope.launch {
            SpeedRepository.telemetry.collect {
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val telemetry = SpeedRepository.telemetry.value
        val unit = SpeedRepository.selectedUnit.value

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SpeedTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speedText = when (telemetry.signalStatus) {
            is GpsSignalStatus.Locked -> "${unit.formatSpeed(telemetry.currentSpeedMps)} ${unit.label}"
            is GpsSignalStatus.Unreliable -> getString(R.string.speed_unreliable)
            else -> getString(R.string.speed_waiting_gps)
        }

        val minutes = telemetry.elapsedSeconds / 60
        val seconds = telemetry.elapsedSeconds % 60
        val timeFormatted = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

        val content = "Maks: ${unit.formatSpeed(telemetry.maxSpeedMps)} ${unit.label} • Süre: $timeFormatted • Mesafe: ${unit.formatDistance(telemetry.totalDistanceMeters)}"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MotoSpeed GPS: $speedText")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_delete, "Testi Bitir", pendingStop)

        // Add Pause/Resume action depending on state
        if (telemetry.testState == TestState.RUNNING) {
            val pauseIntent = Intent(this, SpeedTrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pendingPause = PendingIntent.getService(
                this,
                2,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Duraklat", pendingPause)
        } else if (telemetry.testState == TestState.PAUSED) {
            val resumeIntent = Intent(this, SpeedTrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            val pendingResume = PendingIntent.getService(
                this,
                3,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Devam Et", pendingResume)
        }

        return builder.build()
    }

    private fun stopTrackingService() {
        notificationJob?.cancel()
        locationTracker?.stop()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tracking_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Motosiklet ve araç hız testinde arka plan GPS takibini sürdürür"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTrackingService()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
