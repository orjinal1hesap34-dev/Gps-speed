package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
fun GpsStatusBanner(
    signalStatus: GpsSignalStatus,
    selectedUnit: SpeedUnit,
    keepScreenOn: Boolean,
    onUnitSelected: (SpeedUnit) -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row: App Identity + GPS Status + Screen Awake toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Signal status dot
                val statusDotColor = when (signalStatus) {
                    is GpsSignalStatus.Locked -> GreenSpeed
                    is GpsSignalStatus.Unreliable -> AmberRpm
                    is GpsSignalStatus.Searching -> CyanNeon
                    else -> RedAlert
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "MOTOSPEED GPS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = when (signalStatus) {
                            is GpsSignalStatus.Locked -> "GPS Kilitli (±${String.format(java.util.Locale.US, "%.1f", signalStatus.accuracyMeters)}m)"
                            is GpsSignalStatus.Unreliable -> "Doğruluk Düşük (±${signalStatus.accuracyMeters.toInt()}m)"
                            is GpsSignalStatus.Searching -> "Uydular Aranıyor…"
                            is GpsSignalStatus.GpsDisabled -> "GPS Donanımı Kapalı"
                            is GpsSignalStatus.PermissionDenied -> "Konum İzni Yok"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusDotColor
                    )
                }
            }

            // Screen On button toggle
            IconButton(
                onClick = onToggleKeepScreenOn,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (keepScreenOn) CyanNeon.copy(alpha = 0.15f) else CockpitSurface)
                    .border(1.dp, if (keepScreenOn) CyanNeon else CockpitBorder, RoundedCornerShape(12.dp))
                    .size(40.dp)
                    .testTag("keep_screen_on_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = "Ekranı Açık Tut",
                    tint = if (keepScreenOn) CyanNeon else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Unit Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CockpitSurface)
                .border(1.dp, CockpitBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SpeedUnit.entries.forEach { unit ->
                val isSelected = unit == selectedUnit
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanNeon else Color.Transparent)
                        .clickable { onUnitSelected(unit) }
                        .padding(vertical = 8.dp)
                        .testTag("unit_selector_${unit.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unit.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF001E2B) else TextSecondary
                    )
                }
            }
        }
    }
}
