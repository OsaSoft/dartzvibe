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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.domain.game.CheckoutPath
import cloud.osasoft.dartzvibe.domain.game.KnockoutRoute
import cloud.osasoft.dartzvibe.domain.game.toDisplayString
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Minimum height of the checkout slot strip; it grows with the font scale, which is fixed for the session. */
private val MinCheckoutSlotHeight = 32.dp

/** Vertical room around the route's text line inside the strip. */
private val CheckoutSlotVerticalPadding = 6.dp

private const val ROUTE_SEPARATOR = " → "

private const val KNOCKOUT_PREFIX = "KO"

/** Smallest shrink of a knockout line that still keeps its alternative; below it the alternative is dropped. */
private const val MIN_KNOCKOUT_PAIR_SCALE = 0.8f

/**
 * Top suggestion and, if any, the alternative, formatted for the slot (e.g. "T20 → T19 → D12"). For a knockout
 * ([isKnockout]) each suggestion is "name: darts" and the slot draws the "KO" marker in front of it.
 */
data class CheckoutSlotText(
    val primary: String,
    val secondary: String?,
    val isKnockout: Boolean = false,
)

/** The text for the checkout slot, or null when there is no checkout to suggest. */
fun checkoutSlotText(options: List<CheckoutPath>?): CheckoutSlotText? {
    val paths = options.orEmpty().map { path -> path.throws.joinToString(ROUTE_SEPARATOR) { it.toDisplayString() } }
    val primary = paths.firstOrNull() ?: return null
    return CheckoutSlotText(primary = primary, secondary = paths.getOrNull(1))
}

/**
 * The text for a Parcheesi knockout suggestion (e.g. "Bob: T7", or "Carol: T20 → T7" with "Dave: 25" as the
 * alternative), or null when no opponent can be knocked out. [playerName] names the opponent.
 */
@OptIn(ExperimentalUuidApi::class)
fun knockoutSlotText(
    routes: List<KnockoutRoute>,
    playerName: (Uuid) -> String,
): CheckoutSlotText? {
    val suggestions = routes.map { route ->
        "${playerName(route.opponentId)}: " + route.throws.joinToString(ROUTE_SEPARATOR) { it.toKnockoutString() }
    }
    val primary = suggestions.firstOrNull() ?: return null
    return CheckoutSlotText(primary = primary, secondary = suggestions.getOrNull(1), isKnockout = true)
}

/** Knockouts aim at a total, not a finishing double, so the outer bull reads as plain "25". */
private fun Throw.toKnockoutString(): String =
    if (segment == 25 && multiplier == Multiplier.SINGLE) "25" else toDisplayString()

/**
 * What TalkBack reads for the slot, e.g. "Checkout: T20, T20, Bull, or T20, T19, D20", or for a knockout
 * "Knockout Bob: T20, T7, or knockout Dave: 25".
 */
fun checkoutSlotDescription(text: CheckoutSlotText): String {
    val primary = text.primary.replace(ROUTE_SEPARATOR, ", ")
    if (text.isKnockout) {
        val secondary = text.secondary?.let { ", or knockout " + it.replace(ROUTE_SEPARATOR, ", ") }.orEmpty()
        return "Knockout $primary$secondary"
    }
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
 * it on the same line, smaller, and is left out when both don't fit. Without a suggestion the slot is empty.
 * [text] is a checkout route ([checkoutSlotText]) or, in Parcheesi, a knockout ([knockoutSlotText]).
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun CheckoutHint(
    text: CheckoutSlotText?,
    modifier: Modifier = Modifier,
) {
    // Swapped instantly, no crossfade: the route must change in the same frame as the dart chip and score
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
                    if (slotText.isKnockout) KnockoutSuggestions(slotText) else CheckoutSuggestions(slotText)
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
 * A knockout line: a "KO" badge, then "name: darts", then optionally "or KO name: darts". Names make it wider
 * than a checkout route, so when both suggestions don't fit, the whole line shrinks a little
 * ([MIN_KNOCKOUT_PAIR_SCALE]) before the alternative is dropped; the main suggestion alone shrinks as far as needed.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun KnockoutSuggestions(text: CheckoutSlotText) {
    val onContainer = MaterialTheme.colorScheme.onTertiaryContainer
    val baseStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    val badgeBaseStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
    val orBaseStyle = MaterialTheme.typography.labelSmall
    val secondaryBaseStyle = MaterialTheme.typography.titleSmall
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val maxWidthPx = constraints.maxWidth
        val scale = remember(text, baseStyle, maxWidthPx, density) {
            fun width(
                value: String,
                style: TextStyle,
            ) = textMeasurer.measure(text = value, style = style, maxLines = 1, softWrap = false).size.width
            val badgePx = with(density) { (KnockoutBadgePadding * 2 + KnockoutBadgeGap).roundToPx() }
            val primaryWidth = badgePx + width(KNOCKOUT_PREFIX, badgeBaseStyle) + width(text.primary, baseStyle)
            val secondaryWidth = text.secondary?.let { secondary ->
                with(density) { (14.dp + 6.dp).roundToPx() } + width("or", orBaseStyle) +
                    width("$KNOCKOUT_PREFIX $secondary", secondaryBaseStyle)
            } ?: 0
            val pairScale = fitTextScale(textWidth = primaryWidth + secondaryWidth, maxWidth = maxWidthPx)
            if (text.secondary != null && pairScale >= MIN_KNOCKOUT_PAIR_SCALE) {
                pairScale
            } else {
                fitTextScale(textWidth = primaryWidth, maxWidth = maxWidthPx)
            }
        }

        fun TextStyle.scaled(): TextStyle = if (scale < 1f) copy(fontSize = fontSize * scale) else this

        PrimaryThenOptional(
            primary = {
                Row {
                    KnockoutBadge(style = badgeBaseStyle.scaled(), modifier = Modifier.alignByBaseline())
                    Spacer(modifier = Modifier.width(KnockoutBadgeGap))
                    Text(
                        text = text.primary,
                        style = baseStyle.scaled(),
                        color = onContainer,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            },
            secondary = text.secondary?.let { secondary ->
                {
                    Row {
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "or",
                            style = orBaseStyle.scaled(),
                            color = onContainer.copy(alpha = 0.6f),
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$KNOCKOUT_PREFIX $secondary",
                            style = secondaryBaseStyle.scaled(),
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

/** Room between the "KO" badge's text and its edge, and between the badge and the name. */
private val KnockoutBadgePadding = 5.dp
private val KnockoutBadgeGap = 6.dp

/**
 * The "KO" marker: brand red with white text in both themes, so a knockout never reads as a checkout route. Its
 * label is smaller than the route's line, so the badge stays inside the slot's fixed height.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun KnockoutBadge(
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        text = KNOCKOUT_PREFIX,
        style = style,
        color = MaterialTheme.colorScheme.onPrimary,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = KnockoutBadgePadding),
    )
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
