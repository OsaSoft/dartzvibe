package cloud.osasoft.dartzvibe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import cloud.osasoft.dartzvibe.data.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    // Brand / primary (dartboard red)
    primary = Color(0xFFC62828),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),

    // Secondary (dartboard green outline / accents)
    secondary = Color(0xFF2E7D32),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE9CF),
    onSecondaryContainer = Color(0xFF002105),

    // Tertiary (neutral supporting accent; keep subtle)
    tertiary = Color(0xFF5C5F66),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1E2E8),
    onTertiaryContainer = Color(0xFF191B20),

    // Error (material-ish)
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Light background/surfaces (day mode)
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),

    // Subtle separators / cards
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF3A3A3A),
    outline = Color(0xFFDDDDDD),
    outlineVariant = Color(0xFFE8E8E8),

    // Inverse (used for things like snackbar in light theme)
    inverseSurface = Color(0xFF1E1E1E),
    inverseOnSurface = Color(0xFFF2F2F2),
    inversePrimary = Color(0xFFFF6B6B),
)

private val DarkColorScheme = darkColorScheme(
    // Brand / primary (dartboard red)
    primary = Color(0xFFC62828),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8E1C1C),
    onPrimaryContainer = Color(0xFFFFDAD6),

    // Secondary (dartboard green accents)
    secondary = Color(0xFF2E7D32),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFCFE9CF),

    // Tertiary (neutral supporting accent)
    tertiary = Color(0xFFB0B0B0),
    onTertiary = Color(0xFF121212),
    tertiaryContainer = Color(0xFF2A2A2A),
    onTertiaryContainer = Color(0xFFE0E0E0),

    // Error
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Modern Pub Night base
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),

    // Cards / separators
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2F2F2F),

    // Inverse (used for things like snackbar in dark theme)
    inverseSurface = Color(0xFFF2F2F2),
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = Color(0xFFEF5350),
)

@Suppress("ktlint:standard:function-naming")
@Composable
fun DartzVibeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}
