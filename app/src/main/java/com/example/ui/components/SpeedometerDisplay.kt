package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsSignalStatus
import com.example.model.SpeedUnit
import com.example.ui.theme.AmberRpm
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenSpeed
import com.example.ui.theme.RedAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SpeedometerDisplay(
    currentSpeedMps: Float,
    signalStatus: GpsSignalStatus,
    selectedUnit: SpeedUnit,
    modifier: Modifier = Modifier
) {
    val speedValue = currentSpeedMps * selectedUnit.conversionFactor
    // Max reference for the progress bar (e.g. 200 km/h, 120 mph, 50 m/s)
    val maxScale = when (selectedUnit) {
        SpeedUnit.KMH -> 220f
        SpeedUnit.MPH -> 140f
        SpeedUnit.MPS -> 60f
    }
    val progress = (speedValue / maxScale).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "speed_bar"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(CockpitSurfaceElevated, CockpitSurface)
                )
            )
            .border(1.5.dp, CockpitBorder, RoundedCornerShape(24.dp))
            .padding(vertical = 28.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (signalStatus) {
                is GpsSignalStatus.Locked -> {
                    // Top Speed Arc Gauge Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(CockpitBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(CyanNeon, GreenSpeed, AmberRpm)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Massive High-Visibility Speed Number
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.testTag("speed_value_display")
                    ) {
                        Text(
                            text = selectedUnit.formatSpeed(currentSpeedMps),
                            fontSize = 98.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = CyanNeon,
                            lineHeight = 98.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = selectedUnit.label,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier
                                .padding(bottom = 14.dp)
                                .testTag("speed_unit_label")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live GNSS Doppler Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CockpitSurface)
                            .border(1.dp, GreenSpeed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "GPS Kilitli",
                            tint = GreenSpeed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (signalStatus.isGnssHardware) "GNSS Donanım Hızı" else "GPS Konum Türevi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GreenSpeed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "±${String.format(java.util.Locale.US, "%.1f", signalStatus.accuracyMeters)}m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                is GpsSignalStatus.Unreliable -> {
                    // Low accuracy warning: strict requirement - do not display fake speed
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .testTag("unreliable_gps_banner")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Ölçüm Güvenilir Değil",
                            tint = AmberRpm,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Ölçüm Güvenilir Değil",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberRpm,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "GPS hassasiyeti düşük (±${signalStatus.accuracyMeters.toInt()}m).\nDoğru hız için açık gökyüzü bekleniyor.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is GpsSignalStatus.Searching -> {
                    // No GPS fix yet: strict requirement - do not show 0 or fake speed
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .testTag("waiting_gps_banner")
                    ) {
                        CircularProgressIndicator(
                            color = CyanNeon,
                            modifier = Modifier.size(46.dp),
                            strokeWidth = 3.5.dp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "GPS Sinyali Bekleniyor…",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Uydulara bağlanılıyor. Lütfen açık bir alanda bekleyin.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is GpsSignalStatus.GpsDisabled -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsNotFixed,
                            contentDescription = "GPS Kapalı",
                            tint = RedAlert,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "GPS Servisi Kapalı",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedAlert,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gerçek hız ölçümü için cihazınızın GPS konumunu açmalısınız.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is GpsSignalStatus.PermissionDenied -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "İzin Gerekli",
                            tint = AmberRpm,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Konum İzni Gerekli",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberRpm,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hız ölçümü için hassas konum izni vermelisiniz.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
