package cloud.osasoft.dartzvibe.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Brand red for score highlights and labels in the game and stats screens (Cricket PTS and closed marks, turn summary,
 * current player's score, results and leaderboard values, section headers). The brand red `colorScheme.primary`
 * (#C62828) stays the fill colour (buttons, badges, white text on red) and the colour of buttons, borders and rings,
 * but as these labels it is too dark on the dark theme's greys (~2.2–3.3:1), so dark theme draws them in a lighter
 * red (#FF8080).
 *
 * Light theme keeps #C62828 for [text]; only [textOnContainer], for text on the faint red tint behind Cricket "PTS"
 * and on default cards (`surfaceContainerHighest`), where #C62828 falls to ~4.2:1, is darker (#9A1A1A).
 *
 * [highlight] is the red of what frames that text: the current player's card border and, as a faint tint, the
 * Cricket PTS strip. It matches [text] in each theme, so in dark theme the border, strip and score read as one red
 * (light theme keeps the brand red, as before).
 *
 * Contrast (WCAG AA needs 4.5:1), checked per surface in `BrandColorsTest`: dark #FF8080 is 5.1:1 on default cards
 * to 7.7:1 on the background, and dark [textOnContainer] #FF9090 5.6:1 on the PTS strip (its 10% tint of
 * [highlight]); light #C62828 is 4.6:1 on dialogs to 5.6:1 on white, #9A1A1A 6.2:1 on the PTS strip.
 *
 * [controlLabel] is the label and icon colour of outlined controls (Undo, Miss, unselected S/D/T, the Home menu
 * buttons), via [outlinedControlButtonColors]. Light theme keeps the brand red (5.6:1 on white); dark theme uses a
 * neutral light grey (#E0E0E0, ~11:1 on the turn card) instead, since a lighter red there turned these neutral
 * controls pink. Red stays the fill and highlight colour.
 */
@Immutable
data class BrandColors(
    val text: Color,
    val textOnContainer: Color,
    val highlight: Color,
    val controlLabel: Color,
)

internal val LightBrandColors = BrandColors(
    text = Color(0xFFC62828),
    textOnContainer = Color(0xFF9A1A1A),
    highlight = Color(0xFFC62828),
    controlLabel = Color(0xFFC62828),
)

internal val DarkBrandColors = BrandColors(
    text = Color(0xFFFF8080),
    // A touch lighter than text: the small bold "PTS" label on its red strip needs the margin
    textOnContainer = Color(0xFFFF9090),
    highlight = Color(0xFFFF8080),
    controlLabel = Color(0xFFE0E0E0),
)

val LocalBrandColors = staticCompositionLocalOf { LightBrandColors }

/** Brand colours for the current theme (light/dark), provided by [DartzVibeTheme]. */
val MaterialTheme.brandColors: BrandColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current

/**
 * Colours for a dialog's text buttons (Continue, Yes/No, OK, Undo, Delete): the label in [BrandColors.text], since
 * Material's default `primary` label is ~2.5:1 on the dark dialog surface. Only for buttons inside dialogs; other
 * buttons keep Material's defaults.
 */
@Composable
fun dialogTextButtonColors(): ButtonColors = ButtonDefaults.textButtonColors(
    contentColor = MaterialTheme.brandColors.text,
)

/**
 * Colours for outlined controls on game and Home screens (Undo, Miss, unselected S/D/T, Home menu buttons): the label
 * and icon in [BrandColors.controlLabel], since Material's default `primary` label is ~2.5–3.3:1 on the dark theme's
 * greys. Disabled colours stay Material's defaults.
 */
@Composable
fun outlinedControlButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
    contentColor = MaterialTheme.brandColors.controlLabel,
)

/** WCAG contrast ratio between two opaque colours, from 1 (same luminance) to 21 (black on white). */
fun contrastRatio(
    first: Color,
    second: Color,
): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
