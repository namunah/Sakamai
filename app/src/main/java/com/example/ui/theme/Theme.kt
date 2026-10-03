package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryBlueDark,
    secondary = SecondaryTealDark,
    tertiary = PrimaryBlue,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onPrimary = Color.Black,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
    tertiary = PrimaryBlueDark,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onPrimary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
  )

val IncognitoColorScheme =
  darkColorScheme(
    primary = IncognitoAccent,
    secondary = PrimaryBlueDark,
    tertiary = IncognitoAccent,
    background = IncognitoBackground,
    surface = IncognitoSurface,
    surfaceVariant = Color(0xFF261D3A),
    outline = Color(0xFF3F305E),
    onPrimary = Color.White,
    onBackground = Color(0xFFEDE9FE),
    onSurface = Color(0xFFEDE9FE)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  isIncognito: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    isIncognito -> IncognitoColorScheme
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
