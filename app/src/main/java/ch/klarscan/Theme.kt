package ch.klarscan

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tintenblau für Auswahl, Leuchtstift-Gelb für die eine Hauptaktion (Scannen)
private val Light = lightColorScheme(
    primary = Color(0xFF1C3E66),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3D74A),
    onPrimaryContainer = Color(0xFF1D1A05),
    secondaryContainer = Color(0xFFDCE6F2),
    onSecondaryContainer = Color(0xFF16202B),
    background = Color(0xFFE9EDF1),
    onBackground = Color(0xFF16202B),
    surface = Color(0xFFE9EDF1),
    onSurface = Color(0xFF16202B),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFD2D9E1),
    onSurfaceVariant = Color(0xFF596574),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9CC0EA),
    onPrimary = Color(0xFF0F2540),
    primaryContainer = Color(0xFFE6CC43),
    onPrimaryContainer = Color(0xFF1D1A05),
    secondaryContainer = Color(0xFF263547),
    onSecondaryContainer = Color(0xFFE7ECF1),
    background = Color(0xFF0F141A),
    onBackground = Color(0xFFE7ECF1),
    surface = Color(0xFF0F141A),
    onSurface = Color(0xFFE7ECF1),
    surfaceContainerLow = Color(0xFF18202A),
    surfaceVariant = Color(0xFF2B3644),
    onSurfaceVariant = Color(0xFF97A3B1),
    error = Color(0xFFF08A80),
)

@Composable
fun KlarscanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
