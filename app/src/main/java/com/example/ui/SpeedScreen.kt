package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.GpsSignalStatus
import com.example.model.TestState
import com.example.ui.components.GpsPermissionCard
import com.example.ui.components.GpsStatusBanner
import com.example.ui.components.SpeedometerDisplay
import com.example.ui.components.TelemetryCards
import com.example.ui.components.TestControls
import com.example.ui.theme.CockpitBackground

@Composable
fun SpeedScreen(
    viewModel: SpeedViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val selectedUnit by viewModel.selectedUnit.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()

    // Handle Keep Screen On window flag
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Permission launcher
    val permissionsToRequest = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineLocationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.onPermissionsGranted()
        } else {
            viewModel.onPermissionsDenied()
        }
    }

    // Initial permission and GPS check
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine) {
            viewModel.onPermissionsGranted()
            viewModel.checkGpsProvider()
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CockpitBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header: App Branding + Satellite status + Unit Selection
                GpsStatusBanner(
                    signalStatus = telemetry.signalStatus,
                    selectedUnit = selectedUnit,
                    keepScreenOn = keepScreenOn,
                    onUnitSelected = { unit -> viewModel.setUnit(unit) },
                    onToggleKeepScreenOn = { viewModel.toggleKeepScreenOn() }
                )

                // Permissions / GPS Hardware Prompt (visible only when needed)
                GpsPermissionCard(
                    signalStatus = telemetry.signalStatus,
                    onRequestPermission = {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                )

                // Hero Speedometer Display
                SpeedometerDisplay(
                    currentSpeedMps = telemetry.currentSpeedMps,
                    signalStatus = telemetry.signalStatus,
                    selectedUnit = selectedUnit
                )

                // Primary Telemetry Metrics (Max, Avg, Time, Distance, 0-100)
                TelemetryCards(
                    telemetry = telemetry,
                    unit = selectedUnit
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Test Controls (Start / Pause / Resume / Reset)
                TestControls(
                    testState = telemetry.testState,
                    onStart = { viewModel.startTest(context) },
                    onPause = { viewModel.pauseTest(context) },
                    onResume = { viewModel.resumeTest(context) },
                    onReset = { viewModel.resetTest(context) }
                )
            }
        }
    }
}
