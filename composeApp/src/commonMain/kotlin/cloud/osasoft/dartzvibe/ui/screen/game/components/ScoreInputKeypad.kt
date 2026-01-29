package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Multiplier
import kotlin.math.abs

private const val SWIPE_THRESHOLD_DP = 30f

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
) {
    var activeSwipeMultiplier by remember { mutableStateOf<Multiplier?>(null) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Multiplier selection row (optional)
            if (showMultiplierButtons) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MultiplierButton(
                        text = "Single",
                        shortText = "S",
                        isSelected = selectedMultiplier == Multiplier.SINGLE,
                        onClick = { onMultiplierChange(Multiplier.SINGLE) },
                        modifier = Modifier.weight(1f),
                    )
                    MultiplierButton(
                        text = "Double",
                        shortText = "D",
                        isSelected = selectedMultiplier == Multiplier.DOUBLE,
                        onClick = { onMultiplierChange(Multiplier.DOUBLE) },
                        modifier = Modifier.weight(1f),
                    )
                    MultiplierButton(
                        text = "Triple",
                        shortText = "T",
                        isSelected = selectedMultiplier == Multiplier.TRIPLE,
                        onClick = { onMultiplierChange(Multiplier.TRIPLE) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Score buttons grid: 1-20 in 4 rows of 5
            val scoreRows = listOf(
                listOf(1, 2, 3, 4, 5),
                listOf(6, 7, 8, 9, 10),
                listOf(11, 12, 13, 14, 15),
                listOf(16, 17, 18, 19, 20),
            )

            scoreRows.forEach { row ->
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
                            onScoreSelect = { segment, multiplier -> onScoreSelect(segment, multiplier) },
                            onSwipeChange = { activeSwipeMultiplier = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

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
        }

        // Floating multiplier indicator overlay
        MultiplierIndicator(
            multiplier = activeSwipeMultiplier,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun MultiplierButton(
    text: String,
    shortText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            Text(text, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
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
    val containerColor = when {
        !isPressed -> MaterialTheme.colorScheme.secondaryContainer
        displayMultiplier == Multiplier.DOUBLE -> MaterialTheme.colorScheme.primary
        displayMultiplier == Multiplier.TRIPLE -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        !isPressed -> MaterialTheme.colorScheme.onSecondaryContainer
        displayMultiplier == Multiplier.DOUBLE -> MaterialTheme.colorScheme.onPrimary
        displayMultiplier == Multiplier.TRIPLE -> MaterialTheme.colorScheme.onTertiary
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
        modifier = gestureModifier.height(40.dp),
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
                text = "$prefix$score",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (displayMultiplier != Multiplier.SINGLE) {
                Text(
                    text = "=$displayScore",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                )
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
    val containerColor = when {
        !isPressed -> MaterialTheme.colorScheme.secondaryContainer
        displayMultiplier == Multiplier.DOUBLE -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        !isPressed -> MaterialTheme.colorScheme.onSecondaryContainer
        displayMultiplier == Multiplier.DOUBLE -> MaterialTheme.colorScheme.onPrimary
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
