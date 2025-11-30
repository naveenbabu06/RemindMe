package com.example.remindme.ui.theme

import android.app.Activity
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

// Purplish schemes to match the splash gradient (6A5AE0 -> 9575CD)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB39DFF),
    onPrimary = Color(0xFF201047),
    secondary = Color(0xFF9575CD),
    onSecondary = Color(0xFF1F102F),
    tertiary = Pink80,

    background = Color(0xFF070814),
    onBackground = Color(0xFFE7E0FF),

    surface = Color(0xFF121221),
    onSurface = Color(0xFFE7E0FF),

    surfaceVariant = Color(0xFF2D2948),
    onSurfaceVariant = Color(0xFFCEC2FF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6A5AE0),   // same as splash top
    onPrimary = Color.White,
    secondary = Color(0xFF9575CD), // same as splash bottom
    onSecondary = Color.White,
    tertiary = Pink40,

    background = Color(0xFFF5F1FF),
    onBackground = Color(0xFF1C1733),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1733),

    surfaceVariant = Color(0xFFE3DFFF),
    onSurfaceVariant = Color(0xFF43396B)
)

@Composable
fun RemindMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep false so Material You doesn't override your purple palette
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
        content = content
    )
}
