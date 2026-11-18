package com.increment.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFFA1390A), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC9), onPrimaryContainer = Color(0xFF371000),
    secondary = Color(0xFF785747), secondaryContainer = Color(0xFFFFE2D3),
    onSecondaryContainer = Color(0xFF2E1509),
    tertiary = Color(0xFF625F32), tertiaryContainer = Color(0xFFEAE5AB),
    onTertiaryContainer = Color(0xFF1E1C00),
    background = Color(0xFFFFF8F4), surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF241A15), onBackground = Color(0xFF241A15),
    onSurfaceVariant = Color(0xFF55433B), outline = Color(0xFF88746A),
    outlineVariant = Color(0xFFDFC1B3), surfaceContainer = Color(0xFFF6EAE3),
    surfaceContainerLow = Color(0xFFFFF1E9), surfaceContainerHigh = Color(0xFFF1E1D8),
    surfaceContainerHighest = Color(0xFFECDBD2), surfaceContainerLowest = Color.White,
    inverseSurface = Color(0xFF3B2E28), inverseOnSurface = Color(0xFFFFEDE5),
    inversePrimary = Color(0xFFFFB693)
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB693), onPrimary = Color(0xFF591C00),
    primaryContainer = Color(0xFF7F2B00), onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFFE8BEAA), secondaryContainer = Color(0xFF5E4032),
    onSecondaryContainer = Color(0xFFFFE2D3),
    tertiary = Color(0xFFCDC88E), tertiaryContainer = Color(0xFF4A481D),
    onTertiaryContainer = Color(0xFFEAE5AB),
    background = Color(0xFF1A120E), surface = Color(0xFF1A120E),
    onSurface = Color(0xFFF3DFD5), onBackground = Color(0xFFF3DFD5),
    onSurfaceVariant = Color(0xFFE0C5B8), outline = Color(0xFFA48D81),
    outlineVariant = Color(0xFF55433B), surfaceContainer = Color(0xFF2C211C),
    surfaceContainerLow = Color(0xFF241A15), surfaceContainerHigh = Color(0xFF372923),
    surfaceContainerHighest = Color(0xFF41332C), surfaceContainerLowest = Color(0xFF140D0A),
    inverseSurface = Color(0xFFF3DFD5), inverseOnSurface = Color(0xFF3B2E28),
    inversePrimary = Color(0xFFA1390A)
)

@Composable
fun IncrementTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
