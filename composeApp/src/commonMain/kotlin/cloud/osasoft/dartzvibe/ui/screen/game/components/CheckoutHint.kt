package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.domain.game.CheckoutPath
import cloud.osasoft.dartzvibe.domain.game.toDisplayString

/** Minimum height of the checkout slot strip; it grows with the font scale, which is fixed for the session. */
private val MinCheckoutSlotHeight = 32.dp

/** Vertical room around the route's text line inside the strip. */
private val CheckoutSlotVerticalPadding = 6.dp

private const val ROUTE_SEPARATOR = " → "

/** Top suggestion and, if any, the alternative, formatted for the slot (e.g. "T20 → T19 → D12"). */
data class CheckoutSlotText(
    val primary: String,
    val secondary: String?,
)

/** The text for the checkout slot, or null when there is no checkout to suggest. */
fun checkoutSlotText(options: List<CheckoutPath>?): CheckoutSlotText? {
    val paths = options.orEmpty().map { path -> path.throws.joinToString(ROUTE_SEPARATOR) { it.toDisplayString() } }
    val primary = paths.firstOrNull() ?: return null
    return CheckoutSlotText(primary = primary, secondary = paths.getOrNull(1))
}

/** What TalkBack reads for the slot, e.g. "Checkout: T20, T20, Bull, or T20, T19, D20". */
fun checkoutSlotDescription(text: CheckoutSlotText): String {
    val primary = text.primary.replace(ROUTE_SEPARATOR, ", ")
    val secondary = text.secondary?.let { ", or " + it.replace(ROUTE_SEPARATOR, ", ") }.orEmpty()
    return "Checkout: $primary$secondary"
}

/**
 * Factor to shrink a text measured at [textWidth] so it fits [maxWidth]: 1 when it already fits, otherwise a bit
 * under the exact ratio, so rounding in the second measure can't clip the last character.
 */
fun fitTextScale(
    textWidth: Int,
    maxWidth: Int,
): Float = if (textWidth <= maxWidth || maxWidth <= 0) 1f else maxWidth.toFloat() / textWidth * 0.97f

/**
 * Fixed-height checkout slot between the turn display and the input (Classic, Parcheesi, Checkout Practice).
 * Its height is derived from the route's line height, so it holds at any font scale and stays the same with no,
 * one or two suggestions: the keypad or dartboard below never moves when a checkout appears, changes or
 * disappears. The top suggestion is shown in full, shrunk if it is wider than the strip; the alternative follows
 * it on the same line, smaller, and is left out when both don't fit. Without a checkout the slot is empty.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun CheckoutHint(
    checkoutOptions: List<CheckoutPath>?,
    modifier: Modifier = Modifier,
) {
    // Swapped instantly, no crossfade: the route must change in the same frame as the dart chip and score
    val text = checkoutSlotText(checkoutOptions)
    val lineHeight = MaterialTheme.typography.titleMedium.lineHeight
    val slotHeight = with(LocalDensity.current) {
        val line = if (lineHeight.isSp) lineHeight.toDp() else 0.dp
        maxOf(MinCheckoutSlotHeight, line + CheckoutSlotVerticalPadding)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(slotHeight),
        contentAlignment = Alignment.Center,
    ) {
        // Without a checkout the slot stays empty: the reserved space alone keeps the input below in place
        text?.let { slotText ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .clearAndSetSemantics {
                        contentDescription = checkoutSlotDescription(slotText)
                        liveRegion = LiveRegionMode.Polite
                    }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Checkout notation reads left to right, also in RTL locales
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    CheckoutSuggestions(slotText)
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun CheckoutSuggestions(text: CheckoutSlotText) {
    val onContainer = MaterialTheme.colorScheme.onTertiaryContainer
    val baseStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val maxWidthPx = constraints.maxWidth
        // A route wider than the strip (large font scale, narrow screen) is shrunk, never clipped: its end is the
        // finishing double
        val primaryStyle = remember(text.primary, baseStyle, maxWidthPx) {
            val width = textMeasurer
                .measure(text = text.primary, style = baseStyle, maxLines = 1, softWrap = false)
                .size.width
            val scale = fitTextScale(textWidth = width, maxWidth = maxWidthPx)
            if (scale < 1f) baseStyle.copy(fontSize = baseStyle.fontSize * scale) else baseStyle
        }

        PrimaryThenOptional(
            primary = {
                Text(
                    text = text.primary,
                    style = primaryStyle,
                    color = onContainer,
                    maxLines = 1,
                    softWrap = false,
                )
            },
            secondary = text.secondary?.let { secondary ->
                {
                    Row {
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "or",
                            style = MaterialTheme.typography.labelSmall,
                            color = onContainer.copy(alpha = 0.6f),
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = secondary,
                            style = MaterialTheme.typography.titleSmall,
                            color = onContainer.copy(alpha = 0.8f),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            },
        )
    }
}

/**
 * Centers [primary] and, when it fits on the same line, [secondary] after it. Never clips [secondary]: it is
 * dropped as a whole when it doesn't fit (narrow screens, large font scales).
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun PrimaryThenOptional(
    primary: @Composable () -> Unit,
    secondary: (@Composable () -> Unit)?,
) {
    Layout(
        content = {
            primary()
            secondary?.invoke()
        },
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val primaryPlaceable = measurables[0].measure(looseConstraints)
        val secondaryPlaceable = measurables
            .getOrNull(1)
            ?.measure(looseConstraints.copy(maxWidth = Constraints.Infinity))
            ?.takeIf { primaryPlaceable.width + it.width <= constraints.maxWidth }
        val contentWidth = primaryPlaceable.width + (secondaryPlaceable?.width ?: 0)
        val width = constraints.constrainWidth(contentWidth)
        val height = constraints.constrainHeight(
            maxOf(primaryPlaceable.height, secondaryPlaceable?.height ?: 0),
        )
        layout(width, height) {
            val startX = (width - contentWidth) / 2
            val primaryY = (height - primaryPlaceable.height) / 2
            primaryPlaceable.place(x = startX, y = primaryY)
            secondaryPlaceable?.let { placeable ->
                // Share the primary's baseline, so the smaller alternative sits on the same text line
                val primaryBaseline = primaryPlaceable[FirstBaseline]
                val secondaryBaseline = placeable[FirstBaseline]
                val secondaryY = if (primaryBaseline != AlignmentLine.Unspecified &&
                    secondaryBaseline != AlignmentLine.Unspecified
                ) {
                    primaryY + primaryBaseline - secondaryBaseline
                } else {
                    (height - placeable.height) / 2
                }
                placeable.place(x = startX + primaryPlaceable.width, y = secondaryY)
            }
        }
    }
}
