package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Color scheme for the simplified dartboard wheel.
 */
data class WheelColorScheme(
    /** Color for even-indexed wedges */
    val wedgePrimary: Color,
    /** Color for odd-indexed wedges */
    val wedgeSecondary: Color,
    /** Color for the bull (center) */
    val bull: Color,
    /** Color for segment divider lines */
    val divider: Color,
    /** Color for segment numbers */
    val numberColor: Color,
    /** Highlight color when a segment is touched */
    val highlight: Color,
    /** Highlight color for swipe-up (double) */
    val highlightDouble: Color,
    /** Highlight color for swipe-down (triple) */
    val highlightTriple: Color,
)

/**
 * Gets the color for a wedge based on its index.
 */
fun getWedgeColor(segmentIndex: Int, colorScheme: WheelColorScheme): Color =
    if (segmentIndex % 2 == 0) colorScheme.wedgePrimary else colorScheme.wedgeSecondary

/**
 * Gets the text color for a wedge (contrasting with background).
 */
fun getWedgeTextColor(segmentIndex: Int, colorScheme: WheelColorScheme): Color =
    if (segmentIndex % 2 == 0) Color.White else Color.Black

/**
 * Provides the wheel color scheme based on the current Material theme.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun wheelColorScheme(): WheelColorScheme {
    val colorScheme = MaterialTheme.colorScheme
    return WheelColorScheme(
        wedgePrimary = colorScheme.primary,
        wedgeSecondary = colorScheme.secondaryContainer,
        bull = colorScheme.tertiary,
        divider = colorScheme.outline,
        numberColor = colorScheme.onSurface,
        highlight = Color(0xFFFFEB3B),
        highlightDouble = Color(0xFF2196F3),
        highlightTriple = Color(0xFFFF5722),
    )
}
