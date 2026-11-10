package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import cloud.osasoft.dartzvibe.ui.theme.multiplierColors

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
    /** Outline drawn around the highlighted wedge/bull */
    val highlightOutline: Color,
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
 * Black or white, whichever contrasts more with [fill]. 0.179 is the relative luminance at which
 * black and white text have equal contrast.
 */
fun textColorOn(fill: Color): Color = if (fill.luminance() > 0.179f) Color.Black else Color.White

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
        // Neutral "touched" highlight: blue, so it can't be mistaken for the red/amber multiplier accents
        highlight = Color(0xFF42A5F5),
        // Red family like MultiplierColors.double, but a light rose (Red A100): the brand red would be
        // invisible on the even (primary red) wedges. ~2.5:1 vs #C62828, ~3.4:1 vs dark green wedges
        highlightDouble = Color(0xFFFF8A80),
        highlightTriple = MaterialTheme.multiplierColors.triple,
        highlightOutline = colorScheme.onSurface,
    )
}
