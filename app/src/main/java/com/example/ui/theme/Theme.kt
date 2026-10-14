package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = CyanNeon,
  onPrimary = Color(0xFF00222B),
  primaryContainer = CockpitSurfaceElevated,
  onPrimaryContainer = CyanNeon,
  secondary = GreenSpeed,
  onSecondary = Color(0xFF003916),
  tertiary = AmberRpm,
  onTertiary = Color(0xFF3E1C00),
  background = CockpitBackground,
  onBackground = TextPrimary,
  surface = CockpitSurface,
  onSurface = TextPrimary,
  surfaceVariant = CockpitSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = CockpitBorder,
  error = RedAlert,
  onError = Color.White
)

private val LightColorScheme = DarkColorScheme // Cockpit speedometer prioritizes dark high-contrast mode for outdoor readability

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
