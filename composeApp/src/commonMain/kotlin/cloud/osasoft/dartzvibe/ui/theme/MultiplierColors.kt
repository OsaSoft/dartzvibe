package cloud.osasoft.dartzvibe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Dedicated accent colors for color-coding dart multipliers (chips, indicators, buttons).
 *
 * Triple is the most valuable hit, so it gets the most striking accent (amber/gold with dark text);
 * double keeps the brand red. Kept separate from Material `tertiary` so other UI is unaffected.
 *
 * Contrast of text on chip (WCAG AA needs 4.5:1): red #C62828 / white ~5.6:1;
 * amber #FF8F00 / #1A1A1A ~7.6:1; amber #FFB300 / #121212 ~10:1.
 * Chip edge vs light surfaces is still low for the light amber (~2.3:1 on white, ~2.0:1 on #F0F0F0);
 * the dark text label carries the meaning there.
 */
@Immutable
data class MultiplierColors(
    val double: Color,
    val onDouble: Color,
    val triple: Color,
    val onTriple: Color,
)

internal val LightMultiplierColors = MultiplierColors(
    double = Color(0xFFC62828),
    onDouble = Color(0xFFFFFFFF),
    // Amber 800: deeper amber so the chip reads better against light surfaces
    triple = Color(0xFFFF8F00),
    onTriple = Color(0xFF1A1A1A),
)

internal val DarkMultiplierColors = MultiplierColors(
    double = Color(0xFFC62828),
    onDouble = Color(0xFFFFFFFF),
    // Amber 600: bright gold that pops on the dark background
    triple = Color(0xFFFFB300),
    onTriple = Color(0xFF121212),
)

val LocalMultiplierColors = staticCompositionLocalOf { LightMultiplierColors }

/**
 * Multiplier accent colors for the current theme (light/dark), provided by [DartzVibeTheme].
 */
val MaterialTheme.multiplierColors: MultiplierColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMultiplierColors.current
