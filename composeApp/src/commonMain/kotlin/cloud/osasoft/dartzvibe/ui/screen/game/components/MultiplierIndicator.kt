package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.ui.theme.MultiplierColors
import cloud.osasoft.dartzvibe.ui.theme.multiplierColors

/**
 * Floating indicator that displays the current multiplier during swipe gestures.
 *
 * Shows "DOUBLE" with the red double accent or "TRIPLE" with the amber triple accent
 * (see [MultiplierColors]), for whichever multiplier the current swipe will record.
 * Hidden when multiplier is null or SINGLE.
 *
 * Both are the same opaque pill with a surface-coloured outline and a shadow, so the red DOUBLE keeps its edge over
 * the red dartboard wedges instead of melting into them (issue #33). [compact] is the smaller size the grid and
 * Cricket pads show above their keys ([aboveKeys]); the radial board shows the full size in its centre.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun MultiplierIndicator(
    multiplier: Multiplier?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    // The last multiplier shown: the pill keeps it for the frame in which it hides, or it draws a label-less disc
    val lastShown = remember { LastShownMultiplier() }
    val isVisible = when (multiplier) {
        Multiplier.DOUBLE, Multiplier.TRIPLE -> {
            lastShown.value = multiplier
            true
        }

        Multiplier.SINGLE, null -> false
    }
    val shown = lastShown.value

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        // Gone the moment the finger lifts: a shrinking label-less blob over the bull read as a Bull hit
        exit = ExitTransition.None,
        modifier = modifier,
    ) {
        val multiplierColors = MaterialTheme.multiplierColors
        val backgroundColor = when (shown) {
            Multiplier.TRIPLE -> multiplierColors.triple
            Multiplier.DOUBLE, Multiplier.SINGLE -> multiplierColors.double
        }
        val textColor = when (shown) {
            Multiplier.TRIPLE -> multiplierColors.onTriple
            Multiplier.DOUBLE, Multiplier.SINGLE -> multiplierColors.onDouble
        }
        val text = when (shown) {
            Multiplier.TRIPLE -> "TRIPLE"
            Multiplier.DOUBLE, Multiplier.SINGLE -> "DOUBLE"
        }

        // A plain Box, not a Material Surface: a Surface blocks touches, and the pill lies over the bull and Undo
        Box(
            modifier = Modifier
                .shadow(elevation = 6.dp, shape = PillShape)
                .border(width = PillOutline, color = MaterialTheme.colorScheme.surface, shape = PillShape)
                .background(color = backgroundColor, shape = PillShape)
                .padding(
                    horizontal = if (compact) 20.dp else 28.dp,
                    vertical = if (compact) 6.dp else 10.dp,
                ),
        ) {
            Text(
                text = text,
                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
            )
        }
    }
}

/** Plain holder (not state): written and read in the same composition, so a change needs no extra recomposition. */
private class LastShownMultiplier {
    var value: Multiplier = Multiplier.DOUBLE
}

private val PillShape = RoundedCornerShape(percent = 50)
private val PillOutline = 3.dp

/**
 * Draws the pill just above the top of its parent's key rows ([keysTop] below the parent's top edge), clear of every
 * key, where the hand swiping up from below doesn't cover it. While a swipe shows it, it overlaps what sits above the
 * keys: the checkout slot on the grid keypad (the Undo button at large font sizes), the Undo button in Cricket. It takes
 * no height and no touches, so the layout never moves and Undo still works.
 *
 * [keysTop] is only the key rows' top while the keypad's top slot (the S/D/T row) is empty, which holds whenever a swipe
 * can show the pill: swipe input only exists with the S/D/T buttons off.
 */
internal fun Modifier.aboveKeys(keysTop: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, 0) {
        placeable.place(
            x = 0,
            y = pillTopAboveKeys(
                pillHeightPx = placeable.height,
                keysTopPx = keysTop.roundToPx(),
                gapPx = PillGap.roundToPx(),
            ),
        )
    }
}

private val PillGap = 4.dp

/** Top of a pill [pillHeightPx] tall whose bottom sits [gapPx] above the first key row ([keysTopPx]). */
internal fun pillTopAboveKeys(
    pillHeightPx: Int,
    keysTopPx: Int,
    gapPx: Int,
): Int = keysTopPx - gapPx - pillHeightPx
