package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.ui.theme.multiplierColors
import kotlin.math.abs

private const val SWIPE_THRESHOLD_DP = 30f

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
    var activeSwipeMultiplier by remember { mutableStateOf<Multiplier?>(null) }
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
                                onSwipeChange = { activeSwipeMultiplier = it },
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
                    onSwipeChange = { activeSwipeMultiplier = it },
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
            multiplier = activeSwipeMultiplier,
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
    var dragStartY by remember { mutableStateOf<Float?>(null) }
    var currentDragY by remember { mutableStateOf<Float?>(null) }
    var isPressed by remember { mutableStateOf(false) }

    // Determine the current multiplier based on drag state (for display)
    val displayMultiplier = if (showMultiplierButtons || dragStartY == null || currentDragY == null) {
        if (showMultiplierButtons) selectedMultiplier else Multiplier.SINGLE
    } else {
        val deltaY = currentDragY!! - dragStartY!!
        when {
            deltaY < -SWIPE_THRESHOLD_DP -> Multiplier.DOUBLE
            deltaY > SWIPE_THRESHOLD_DP -> Multiplier.TRIPLE
            else -> Multiplier.SINGLE
        }
    }

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
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        !isPressed -> MaterialTheme.colorScheme.onSecondaryContainer
        displayMultiplier == Multiplier.DOUBLE -> multiplierColors.onDouble
        displayMultiplier == Multiplier.TRIPLE -> multiplierColors.onTriple
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val gestureModifier = if (!showMultiplierButtons && enabled) {
        modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    onScoreSelect(score, Multiplier.SINGLE)
                }
            }
            .pointerInput(Unit) {
                val swipeThresholdPx = SWIPE_THRESHOLD_DP * density
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartY = offset.y
                        currentDragY = offset.y
                        isPressed = true
                        onSwipeChange(null)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentDragY = change.position.y
                        val startY = dragStartY ?: return@detectDragGestures
                        val deltaY = change.position.y - startY
                        val multiplier = when {
                            deltaY < -swipeThresholdPx -> Multiplier.DOUBLE
                            deltaY > swipeThresholdPx -> Multiplier.TRIPLE
                            else -> null
                        }
                        onSwipeChange(multiplier)
                    },
                    onDragEnd = {
                        val startY = dragStartY
                        val endY = currentDragY
                        if (startY != null && endY != null) {
                            val deltaY = endY - startY
                            val multiplier = when {
                                deltaY < -swipeThresholdPx -> Multiplier.DOUBLE
                                deltaY > swipeThresholdPx -> Multiplier.TRIPLE
                                else -> Multiplier.SINGLE
                            }
                            onScoreSelect(score, multiplier)
                        }
                        dragStartY = null
                        currentDragY = null
                        isPressed = false
                        onSwipeChange(null)
                    },
                    onDragCancel = {
                        dragStartY = null
                        currentDragY = null
                        isPressed = false
                        onSwipeChange(null)
                    },
                )
            }
    } else {
        modifier
    }

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
    var dragStartY by remember { mutableStateOf<Float?>(null) }
    var currentDragY by remember { mutableStateOf<Float?>(null) }
    var isPressed by remember { mutableStateOf(false) }

    // Bull only has single (25) and double (50), no triple
    // Determine the current multiplier based on drag state (for display)
    val displayMultiplier = if (showMultiplierButtons || dragStartY == null || currentDragY == null) {
        if (showMultiplierButtons) {
            // Clamp to SINGLE or DOUBLE only for bull
            if (selectedMultiplier == Multiplier.TRIPLE) Multiplier.DOUBLE else selectedMultiplier
        } else {
            Multiplier.SINGLE
        }
    } else {
        val deltaY = currentDragY!! - dragStartY!!
        // Both up and down swipe result in DOUBLE for bull
        if (abs(deltaY) > SWIPE_THRESHOLD_DP) {
            Multiplier.DOUBLE
        } else {
            Multiplier.SINGLE
        }
    }

    val displayScore = 25 * displayMultiplier.value
    val displayText = if (displayMultiplier == Multiplier.DOUBLE) "BULL" else "25"
    val subtitleText = if (displayMultiplier == Multiplier.DOUBLE) "50" else "Outer"

    // Determine button colors based on drag state
    val multiplierColors = MaterialTheme.multiplierColors
    val containerColor = when {
        !isPressed -> MaterialTheme.colorScheme.secondaryContainer
        displayMultiplier == Multiplier.DOUBLE -> multiplierColors.double
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        !isPressed -> MaterialTheme.colorScheme.onSecondaryContainer
        displayMultiplier == Multiplier.DOUBLE -> multiplierColors.onDouble
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val gestureModifier = if (!showMultiplierButtons && enabled) {
        modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    onScoreSelect(25, Multiplier.SINGLE)
                }
            }
            .pointerInput(Unit) {
                val swipeThresholdPx = SWIPE_THRESHOLD_DP * density
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartY = offset.y
                        currentDragY = offset.y
                        isPressed = true
                        onSwipeChange(null)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentDragY = change.position.y
                        val startY = dragStartY ?: return@detectDragGestures
                        val deltaY = change.position.y - startY
                        // Both up and down show DOUBLE indicator for bull
                        val multiplier = if (abs(deltaY) > swipeThresholdPx) {
                            Multiplier.DOUBLE
                        } else {
                            null
                        }
                        onSwipeChange(multiplier)
                    },
                    onDragEnd = {
                        val startY = dragStartY
                        val endY = currentDragY
                        if (startY != null && endY != null) {
                            val deltaY = endY - startY
                            // Both up and down result in DOUBLE for bull
                            val multiplier = if (abs(deltaY) > swipeThresholdPx) {
                                Multiplier.DOUBLE
                            } else {
                                Multiplier.SINGLE
                            }
                            onScoreSelect(25, multiplier)
                        }
                        dragStartY = null
                        currentDragY = null
                        isPressed = false
                        onSwipeChange(null)
                    },
                    onDragCancel = {
                        dragStartY = null
                        currentDragY = null
                        isPressed = false
                        onSwipeChange(null)
                    },
                )
            }
    } else {
        modifier
    }

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
