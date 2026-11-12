package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.ui.theme.multiplierColors

private const val SWIPE_THRESHOLD_DP = 30f

/** Key the Bull button reports to [updateSwipePill] (the 1–20 keys report their number). */
private const val BULL_KEY = 25

/**
 * Opacity of the pressed tint on a key held without buttons, like the state layer an enabled Button shows when
 * pressed, a little stronger because there is no ripple to go with it.
 */
private const val PRESSED_TINT_ALPHA = 0.16f

/** Padding around the whole keypad. */
private val KeypadPadding = 8.dp

/** Gap between the keypad's rows. */
private val KeypadRowGap = 4.dp

/** Number of 1–20 key rows. */
private const val KEY_ROWS = 4

/** Padding inside a key, around its label. */
private val KeyContentPadding = 2.dp

private enum class KeypadSlot { TOP, KEYS, BOTTOM }

/**
 * The keypad's rows stacked [KeypadRowGap] apart, like a Column: [top], then the [KEY_ROWS] rows [keys] emits,
 * then [bottom]. The key rows are [KeyHeight] tall, unless [heightLeft] (the height available to the whole keypad,
 * padding included) is set and too short: then they share what the other rows leave (see [fitKeyHeight]), no lower
 * than [minKeyHeight]. The other rows are measured first, so the key height depends only on them and [heightLeft].
 * [keys] is told once whether the keys were shrunk, so their labels go on one line only then.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun KeypadColumn(
    heightLeft: Dp?,
    minKeyHeight: Dp,
    top: @Composable () -> Unit,
    keys: @Composable (oneLineLabels: Boolean) -> Unit,
    bottom: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val width = constraints.maxWidth
        val rowConstraints = Constraints(maxWidth = width)
        val gap = KeypadRowGap.roundToPx()
        val topPlaceables = subcompose(KeypadSlot.TOP, top).map { it.measure(rowConstraints) }
        val bottomPlaceables = subcompose(KeypadSlot.BOTTOM, bottom).map { it.measure(rowConstraints) }
        val nominalKeyHeight = KeyHeight.roundToPx()
        val keyHeight = fitKeyHeight(
            // In pixels, like the padding modifier, so rounding never leaves a pixel over
            heightLeftPx = heightLeft?.let {
                it.roundToPx() -
                    columnChromePx(
                        paddingPx = KeypadPadding.roundToPx(),
                        gapPx = gap,
                        rowHeightsPx = (topPlaceables + bottomPlaceables).map { row -> row.height },
                        flexibleRows = KEY_ROWS,
                    )
            },
            rows = KEY_ROWS,
            nominalPx = nominalKeyHeight,
            minPx = minKeyHeight.roundToPx(),
        )
        val keyConstraints = Constraints(maxWidth = width, minHeight = keyHeight, maxHeight = keyHeight)
        val keyPlaceables = subcompose(KeypadSlot.KEYS) { keys(keyHeight < nominalKeyHeight) }
            .map { it.measure(keyConstraints) }
        val placeables = topPlaceables + keyPlaceables + bottomPlaceables
        val gapsHeight = gap * (placeables.size - 1).coerceAtLeast(0)
        val height = constraints.constrainHeight(placeables.sumOf { it.height } + gapsHeight)

        layout(width, height) {
            var y = 0
            placeables.forEach { placeable ->
                placeable.place(x = 0, y = y)
                y += placeable.height + gap
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ScoreInputKeypad(
    selectedMultiplier: Multiplier,
    canThrow: Boolean,
    showMultiplierButtons: Boolean,
    onMultiplierChange: (Multiplier) -> Unit,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onMiss: () -> Unit,
    modifier: Modifier = Modifier,
    // Height available to the whole keypad; the 1–20 keys shrink to fit it (down to minKeyHeight). Null: default.
    maxHeight: Dp? = null,
) {
    var swipePill by remember { mutableStateOf<SwipePill?>(null) }
    // Only the key that set the pill clears it, so a second finger on another key can't hide it
    val swipeChangeFor = { key: Int ->
        { multiplier: Multiplier? ->
            swipePill =
                updateSwipePill(current = swipePill, key = key, multiplier = multiplier)
        }
    }
    // A shrunk key must still fit its one-line label at the user's font scale
    val labelLineHeight = MaterialTheme.typography.titleSmall.lineHeight
    val minKeyHeight = with(LocalDensity.current) {
        val labelLine = if (labelLineHeight.isSp) labelLineHeight.toDp() else 0.dp
        minKeyHeight(labelLine = labelLine, contentPadding = KeyContentPadding)
    }

    Box(modifier = modifier) {
        KeypadColumn(
            heightLeft = maxHeight,
            minKeyHeight = minKeyHeight,
            modifier = Modifier.padding(KeypadPadding),
            top = {
                // Multiplier selection row (optional)
                if (showMultiplierButtons) {
                    MultiplierButtonsRow(
                        selectedMultiplier = selectedMultiplier,
                        onMultiplierChange = onMultiplierChange,
                    )
                }
            },
            keys = { oneLineLabels ->
                // Score buttons grid: 1-20 in 4 rows of 5
                (1..20).chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        row.forEach { score ->
                            ScoreButton(
                                score = score,
                                selectedMultiplier = selectedMultiplier,
                                showMultiplierButtons = showMultiplierButtons,
                                enabled = canThrow,
                                oneLineLabel = oneLineLabels,
                                onScoreSelect = { segment, multiplier -> onScoreSelect(segment, multiplier) },
                                onSwipeChange = swipeChangeFor(score),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            },
            bottom = {
                // Bull row: Tap = 25 (single), Swipe = 50 (double)
                BullButton(
                    showMultiplierButtons = showMultiplierButtons,
                    selectedMultiplier = selectedMultiplier,
                    enabled = canThrow,
                    onScoreSelect = onScoreSelect,
                    onSwipeChange = swipeChangeFor(BULL_KEY),
                )

                // Miss button
                OutlinedButton(
                    onClick = onMiss,
                    enabled = canThrow,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Miss")
                }

                // Swipe hint (only when multiplier buttons are hidden)
                if (!showMultiplierButtons) {
                    Text(
                        text = "Tap = Single • Swipe ↑ = Double • Swipe ↓ = Triple",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            },
        )

        // Floating multiplier indicator overlay
        MultiplierIndicator(
            multiplier = swipePill?.multiplier,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun MultiplierButtonsRow(
    selectedMultiplier: Multiplier,
    onMultiplierChange: (Multiplier) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(Multiplier.SINGLE, Multiplier.DOUBLE, Multiplier.TRIPLE).forEach { multiplier ->
            MultiplierButton(
                multiplier = multiplier,
                isSelected = selectedMultiplier == multiplier,
                onClick = { onMultiplierChange(multiplier) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Live state of a key's swipe, written by [keySwipeInput] and read to draw the key. */
private class KeySwipeState {
    var isPressed by mutableStateOf(false)

    /** Multiplier the key would enter if lifted now ([classifyKeySwipe]); SINGLE until the swipe passes the threshold. */
    var preview by mutableStateOf(Multiplier.SINGLE)
}

/** Pill callback value for a key's preview: null (no pill) for a single. */
private fun Multiplier.asPill(): Multiplier? = takeIf { it != Multiplier.SINGLE }

/**
 * Tap and swipe input for a grid key while the S/D/T buttons are hidden ([active]): one gesture per touch, classified
 * on lift by [classifyKeySwipe] from the vertical movement since touch-down. The live [swipe] preview uses the same
 * function and pixel threshold, so the label flips exactly where the throw would change. A cancelled gesture enters
 * nothing.
 *
 * Runs in the Initial pass: the key is a disabled Button, and its clickable still consumes the down and the up in the
 * Main pass before this outer modifier would see them (that made a plain tap never register).
 *
 * The handler is long-lived (restarting it would cancel a swipe in progress), so it reads the callbacks through
 * updated state ([onSwipeChange], [onRelease]); [active] is a key, so toggling it takes effect at once.
 */
private fun Modifier.keySwipeInput(
    active: Boolean,
    isBull: Boolean,
    swipe: KeySwipeState,
    onSwipeChange: State<(Multiplier?) -> Unit>,
    onRelease: State<(Multiplier) -> Unit>,
): Modifier = pointerInput(active, isBull) {
    // Inactive (buttons shown, or no throw allowed): leave every event to the Button
    if (!active) return@pointerInput

    val thresholdPx = SWIPE_THRESHOLD_DP * density
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        down.consume()
        val multiplierAt = { y: Float ->
            classifyKeySwipe(deltaY = y - down.position.y, thresholdPx = thresholdPx, isBull = isBull)
        }
        swipe.isPressed = true
        swipe.preview = Multiplier.SINGLE

        // finally: clear the preview even if the handler is cancelled mid-swipe (e.g. the gate key changes)
        try {
            var lastY = down.position.y
            var lifted = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                // The pointer vanished: cancelled
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                lastY = change.position.y
                if (!change.pressed) {
                    // A real lift reaches this pass unconsumed; a system cancel arrives as an already consumed up
                    lifted = !change.isConsumed
                    change.consume()
                    break
                }
                change.consume()
                val preview = multiplierAt(lastY)
                if (preview != swipe.preview) {
                    swipe.preview = preview
                    onSwipeChange.value(preview.asPill())
                }
            }
            if (lifted) {
                onRelease.value(multiplierAt(lastY))
            }
        } finally {
            swipe.isPressed = false
            swipe.preview = Multiplier.SINGLE
            onSwipeChange.value(null)
        }
    }
}

/**
 * Accessibility for a grid key while the S/D/T buttons are hidden ([active]). The key is a disabled Button then, so
 * TalkBack would read it as disabled and offer nothing; instead it is a Button whose click enters a single, with
 * custom actions for the double and (not on the [isBull] key) the triple. Inactive: the Button's own semantics.
 */
private fun Modifier.keyAccessibility(
    active: Boolean,
    label: String,
    isBull: Boolean,
    onEnter: (Multiplier) -> Unit,
): Modifier = if (active) {
    clearAndSetSemantics {
        contentDescription = label
        role = Role.Button
        onClick(label = "Single") {
            onEnter(Multiplier.SINGLE)
            true
        }
        customActions = listOfNotNull(
            CustomAccessibilityAction(label = "Double") {
                onEnter(Multiplier.DOUBLE)
                true
            },
            if (isBull) {
                null
            } else {
                CustomAccessibilityAction(label = "Triple") {
                    onEnter(Multiplier.TRIPLE)
                    true
                }
            },
        )
    }
} else {
    this
}

/** Container of a key held down for a single without buttons: the key's colour with a pressed tint over it. */
@Composable
private fun pressedKeyContainer(): Color = MaterialTheme.colorScheme.onSecondaryContainer
    .copy(alpha = PRESSED_TINT_ALPHA)
    .compositeOver(MaterialTheme.colorScheme.secondaryContainer)

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ScoreButton(
    score: Int,
    selectedMultiplier: Multiplier,
    showMultiplierButtons: Boolean,
    enabled: Boolean,
    // Label and points on one line: only when the keys were shrunk to fit a short screen
    oneLineLabel: Boolean,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onSwipeChange: (Multiplier?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipe = remember { KeySwipeState() }
    val isPressed = swipe.isPressed

    // The multiplier to display: the selected one with buttons, else the one the current swipe would record
    val displayMultiplier = if (showMultiplierButtons) selectedMultiplier else swipe.preview

    val displayScore = score * displayMultiplier.value
    val prefix = when (displayMultiplier) {
        Multiplier.SINGLE -> ""
        Multiplier.DOUBLE -> "D"
        Multiplier.TRIPLE -> "T"
    }

    // Determine button colors based on drag state
    val multiplierColors = MaterialTheme.multiplierColors
    val containerColor = when {
        !isPressed -> MaterialTheme.colorScheme.secondaryContainer

        displayMultiplier == Multiplier.DOUBLE -> multiplierColors.double

        displayMultiplier == Multiplier.TRIPLE -> multiplierColors.triple

        // Held for a single (only without buttons): pressed tint, so a tap shows it registered
        else -> pressedKeyContainer()
    }
    val contentColor = when {
        !isPressed -> MaterialTheme.colorScheme.onSecondaryContainer
        displayMultiplier == Multiplier.DOUBLE -> multiplierColors.onDouble
        displayMultiplier == Multiplier.TRIPLE -> multiplierColors.onTriple
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val swipeInputActive = !showMultiplierButtons && enabled
    val currentOnSwipeChange = rememberUpdatedState(onSwipeChange)
    val currentOnRelease = rememberUpdatedState { multiplier: Multiplier -> onScoreSelect(score, multiplier) }
    val gestureModifier = modifier
        .keyAccessibility(
            active = swipeInputActive,
            label = "$score",
            isBull = false,
            onEnter = { multiplier -> onScoreSelect(score, multiplier) },
        )
        .keySwipeInput(
            active = swipeInputActive,
            isBull = false,
            swipe = swipe,
            onSwipeChange = currentOnSwipeChange,
            onRelease = currentOnRelease,
        )

    FilledTonalButton(
        onClick = { onScoreSelect(score, selectedMultiplier) },
        enabled = enabled && showMultiplierButtons,
        modifier = gestureModifier.fillMaxHeight(),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(KeyContentPadding),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
    ) {
        val showPoints = displayMultiplier != Multiplier.SINGLE
        if (oneLineLabel) {
            // Keys shrunk to fit a short screen: label and points share one line, so neither is clipped
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$prefix$score",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
                if (showPoints) {
                    Text(
                        text = "=$displayScore",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        } else {
            // Default key height (as on main): the points go under the label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$prefix$score",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (showPoints) {
                    Text(
                        text = "=$displayScore",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun BullButton(
    showMultiplierButtons: Boolean,
    selectedMultiplier: Multiplier,
    enabled: Boolean,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onSwipeChange: (Multiplier?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipe = remember { KeySwipeState() }
    val isPressed = swipe.isPressed

    // Bull only has single (25) and double (50), no triple: buttons clamp Triple to Double, and a swipe either way
    // is Double (see classifyKeySwipe)
    val displayMultiplier = if (showMultiplierButtons) {
        if (selectedMultiplier == Multiplier.TRIPLE) Multiplier.DOUBLE else selectedMultiplier
    } else {
        swipe.preview
    }

    val displayScore = 25 * displayMultiplier.value
    val displayText = if (displayMultiplier == Multiplier.DOUBLE) "BULL" else "25"
    val subtitleText = if (displayMultiplier == Multiplier.DOUBLE) "50" else "Outer"

    // Determine button colors based on drag state
    val multiplierColors = MaterialTheme.multiplierColors
    val containerColor = when {
        !isPressed -> MaterialTheme.colorScheme.secondaryContainer

        displayMultiplier == Multiplier.DOUBLE -> multiplierColors.double

        // Held for a single (only without buttons): pressed tint, so a tap shows it registered
        else -> pressedKeyContainer()
    }
    val contentColor = when {
        !isPressed -> MaterialTheme.colorScheme.onSecondaryContainer
        displayMultiplier == Multiplier.DOUBLE -> multiplierColors.onDouble
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val swipeInputActive = !showMultiplierButtons && enabled
    val currentOnSwipeChange = rememberUpdatedState(onSwipeChange)
    val currentOnRelease = rememberUpdatedState { multiplier: Multiplier -> onScoreSelect(25, multiplier) }
    val gestureModifier = modifier
        .keyAccessibility(
            active = swipeInputActive,
            label = "25, Outer bull",
            isBull = true,
            onEnter = { multiplier -> onScoreSelect(25, multiplier) },
        )
        .keySwipeInput(
            active = swipeInputActive,
            isBull = true,
            swipe = swipe,
            onSwipeChange = currentOnSwipeChange,
            onRelease = currentOnRelease,
        )

    FilledTonalButton(
        onClick = {
            // Clamp to SINGLE or DOUBLE only for bull
            val multiplier = if (selectedMultiplier == Multiplier.TRIPLE) Multiplier.DOUBLE else selectedMultiplier
            onScoreSelect(25, multiplier)
        },
        enabled = enabled && showMultiplierButtons,
        modifier = gestureModifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(2.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
