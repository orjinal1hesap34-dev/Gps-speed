package com.example.ui.components

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
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedTelemetry
import com.example.model.SpeedUnit
import com.example.ui.theme.AmberRpm
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenSpeed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TelemetryCards(
    telemetry: SpeedTelemetry,
    unit: SpeedUnit,
    modifier: Modifier = Modifier
) {
    val minutes = telemetry.elapsedSeconds / 60
    val seconds = telemetry.elapsedSeconds % 60
    val hours = minutes / 60
    val timeFormatted = if (hours > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 2x2 Primary Telemetry Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryTile(
                title = "EN YÜKSEK HIZ",
                value = unit.formatSpeed(telemetry.maxSpeedMps),
                unitLabel = unit.label,
                icon = Icons.Default.Speed,
                accentColor = AmberRpm,
                tag = "max_speed_tile",
                modifier = Modifier.weight(1f)
            )

            TelemetryTile(
                title = "ORTALAMA HIZ",
                value = unit.formatSpeed(telemetry.avgSpeedMps),
                unitLabel = unit.label,
                icon = Icons.Default.AvTimer,
                accentColor = CyanNeon,
                tag = "avg_speed_tile",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryTile(
                title = "GEÇEN SÜRE",
                value = timeFormatted,
                unitLabel = if (hours > 0) "saat" else "dakika",
                icon = Icons.Default.Timer,
                accentColor = GreenSpeed,
                tag = "elapsed_time_tile",
                modifier = Modifier.weight(1f)
            )

            TelemetryTile(
                title = "KAT EDİLEN MESAFE",
                value = unit.formatDistance(telemetry.totalDistanceMeters),
                unitLabel = "",
                icon = Icons.Default.Navigation,
                accentColor = CyanNeon,
                tag = "distance_tile",
                modifier = Modifier.weight(1f)
            )
        }

        // Acceleration Benchmark Sprint Card (0-50, 0-100 km/h)
        AccelerationBenchmarkCard(
            zeroToFiftySec = telemetry.zeroToFiftyTimeSec,
            zeroToHundredSec = telemetry.zeroToHundredTimeSec
        )
    }
}

@Composable
fun TelemetryTile(
    title: String,
    value: String,
    unitLabel: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CockpitSurface)
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag(tag)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                if (unitLabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unitLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AccelerationBenchmarkCard(
    zeroToFiftySec: Double?,
    zeroToHundredSec: Double?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CockpitSurface)
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("acceleration_sprint_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TwoWheeler,
                    contentDescription = "Hızlanma Testi",
                    tint = AmberRpm,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "HIZLANMA ÖLÇÜMLERİ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "0\'dan kalkış testi",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SprintMetricBadge(
                    label = "0-50 km/s",
                    timeSec = zeroToFiftySec
                )
                SprintMetricBadge(
                    label = "0-100 km/s",
                    timeSec = zeroToHundredSec
                )
            }
        }
    }
}

@Composable
fun SprintMetricBadge(label: String, timeSec: Double?) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )
        Text(
            text = if (timeSec != null) String.format(java.util.Locale.US, "%.2f sn", timeSec) else "--",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (timeSec != null) GreenSpeed else TextSecondary
        )
    }
}
