package com.example.yiheng.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = WarmPrimary80,
    secondary = WarmSecondary80,
    tertiary = WarmTertiary80,
    background = WarmBackgroundDark,
    surface = WarmSurfaceDark,
    onPrimary = Color(0xFF3A1A10),
    onSecondary = Color(0xFF2B1A07),
    onTertiary = Color(0xFF3A1114),
    onBackground = Color(0xFFFFEDE3),
    onSurface = Color(0xFFFFEDE3)
)

private val LightColorScheme = lightColorScheme(
    primary = WarmPrimary40,
    secondary = WarmSecondary40,
    tertiary = WarmTertiary40,
    background = WarmBackgroundLight,
    surface = WarmSurfaceLight,
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF2A1600),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF231A16),
    onSurface = Color(0xFF231A16)
)

@Composable
fun YihengTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // Set to false to always use the warmer palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
