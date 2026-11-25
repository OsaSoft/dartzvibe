package cloud.osasoft.dartzvibe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Text colour for a positive result, the counterpart of `colorScheme.error` for a negative one (Checkout Practice
 * PASS next to the red FAIL in Game Details).
 *
 * The dartboard green `colorScheme.secondary` (#2E7D32) falls just short of 4.5:1 on the light theme's
 * `surfaceVariant` and is only ~3:1 on the dark theme's greys, so light theme uses a deeper green (#1B5E20) and
 * dark theme a lighter one (#81C784). `ResultColorsTest` checks 4.5:1 on each surface it can sit on.
 */
@Immutable
data class ResultColors(
    val success: Color,
)

internal val LightResultColors = ResultColors(success = Color(0xFF1B5E20))

internal val DarkResultColors = ResultColors(success = Color(0xFF81C784))

val LocalResultColors = staticCompositionLocalOf { LightResultColors }

/** Result colours for the current theme (light/dark), provided by [DartzVibeTheme]. */
val MaterialTheme.resultColors: ResultColors
    @Composable
    @ReadOnlyComposable
    get() = LocalResultColors.current
