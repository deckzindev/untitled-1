package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = AccentGreenBright,
  onPrimary = DarkSurface,
  primaryContainer = AccentGreen,
  onPrimaryContainer = TextPrimary,
  secondary = AccentBlueBright,
  onSecondary = DarkSurface,
  secondaryContainer = DarkSurfaceVariant,
  onSecondaryContainer = AccentBlueBright,
  tertiary = AccentPurple,
  background = DarkSurface,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = DarkBorder,
  error = AccentRed,
  onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
  primary = AccentBlue,
  onPrimary = LightSurface,
  primaryContainer = LightSurfaceVariant,
  onPrimaryContainer = LightTextPrimary,
  secondary = AccentGreen,
  onSecondary = LightSurface,
  secondaryContainer = LightSurfaceVariant,
  onSecondaryContainer = LightTextPrimary,
  tertiary = AccentPurple,
  background = LightBackground,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightTextSecondary,
  outline = LightBorder,
  error = AccentRed,
  onError = LightSurface
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For developer tools, dark theme is strongly preferred, but dynamic/light is supported
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> DarkColorScheme // Default to sleek dark dev mode for high contrast
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
