package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.osasoft.dartzvibe.data.model.Multiplier
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private data class GestureState(
    val startPosition: Offset? = null,
    val currentPosition: Offset? = null,
    val activeHit: DartboardHit? = null,
)

private const val SWIPE_THRESHOLD_DP = 30f

private enum class SwipeDirection { UP, DOWN }

/**
 * Radial dartboard wheel input component.
 *
 * A simplified wheel layout with 20 wedges in dartboard segment order and a bull in the center.
 * Multiplier is determined by gestures (swipe up = double, swipe down = triple) or S/D/T buttons.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun RadialDartboard(
    selectedMultiplier: Multiplier,
    canThrow: Boolean,
    showMultiplierButtons: Boolean,
    onMultiplierChange: (Multiplier) -> Unit,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onMiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = wheelColorScheme()
    val textMeasurer = rememberTextMeasurer()

    // The pointerInput gesture handler below is long-lived: it is not restarted when these change
    // (restarting it would cancel a drag in progress), so it must read them through updated state.
    // Capturing the parameters directly made taps record the multiplier selected when the handler
    // started, e.g. T20 again after switching from Triple to Double.
    val currentSelectedMultiplier by rememberUpdatedState(selectedMultiplier)
    val currentOnScoreSelect by rememberUpdatedState(onScoreSelect)

    var gestureState by remember { mutableStateOf(GestureState()) }
    var highlightedSegmentIndex by remember { mutableStateOf<Int?>(null) }
    var highlightedBull by remember { mutableStateOf(false) }
    var swipeIndicator by remember { mutableStateOf<SwipeDirection?>(null) }

    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Optional multiplier buttons
        if (showMultiplierButtons) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MultiplierButton(
                    multiplier = Multiplier.SINGLE,
                    isSelected = selectedMultiplier == Multiplier.SINGLE,
                    onClick = { onMultiplierChange(Multiplier.SINGLE) },
                    modifier = Modifier.weight(1f),
                )
                MultiplierButton(
                    multiplier = Multiplier.DOUBLE,
                    isSelected = selectedMultiplier == Multiplier.DOUBLE,
                    onClick = { onMultiplierChange(Multiplier.DOUBLE) },
                    modifier = Modifier.weight(1f),
                )
                MultiplierButton(
                    multiplier = Multiplier.TRIPLE,
                    isSelected = selectedMultiplier == Multiplier.TRIPLE,
                    onClick = { onMultiplierChange(Multiplier.TRIPLE) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Dartboard wheel Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            var canvasCenter by remember { mutableStateOf(Offset.Zero) }
            var canvasRadius by remember { mutableStateOf(0f) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(canThrow, showMultiplierButtons) {
                        if (!canThrow) return@pointerInput

                        val swipeThresholdPx = SWIPE_THRESHOLD_DP * density

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val hit = getHitAtPosition(down.position, canvasCenter, canvasRadius)

                            // If outside dartboard, don't consume - let scroll handle it
                            if (hit == null) {
                                return@awaitEachGesture
                            }

                            // Inside dartboard - consume and handle
                            down.consume()

                            if (showMultiplierButtons) {
                                // Tap mode: wait for up and register score
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    up.consume()
                                    currentOnScoreSelect(hit.segment, currentSelectedMultiplier)
                                }
                            } else {
                                // Swipe mode: track drag for multiplier detection
                                gestureState = GestureState(
                                    startPosition = down.position,
                                    currentPosition = down.position,
                                    activeHit = hit,
                                )
                                highlightedSegmentIndex = hit.segmentIndex
                                highlightedBull = hit.isBull
                                swipeIndicator = null

                                var lastPosition = down.position

                                // finally: clear the highlight even if the handler is cancelled mid-drag
                                // (e.g. pointerInput restarts), so it can't stay stuck on the board
                                try {
                                    // false = the gesture was cancelled rather than lifted: record nothing
                                    val completed = drag(down.id) { change ->
                                        change.consume()
                                        lastPosition = change.position
                                        gestureState = gestureState.copy(currentPosition = lastPosition)

                                        val startY = gestureState.startPosition?.y ?: return@drag
                                        val deltaY = lastPosition.y - startY
                                        swipeIndicator = when {
                                            deltaY < -swipeThresholdPx -> SwipeDirection.UP
                                            deltaY > swipeThresholdPx -> SwipeDirection.DOWN
                                            else -> null
                                        }
                                    }

                                    if (completed) {
                                        // Lifted - determine multiplier and submit score
                                        val startY = gestureState.startPosition?.y ?: 0f
                                        val deltaY = lastPosition.y - startY

                                        val multiplier = if (hit.isBull) {
                                            // Bull has no triple - both up and down are double
                                            if (abs(deltaY) > swipeThresholdPx) {
                                                Multiplier.DOUBLE
                                            } else {
                                                Multiplier.SINGLE
                                            }
                                        } else {
                                            when {
                                                deltaY < -swipeThresholdPx -> Multiplier.DOUBLE
                                                deltaY > swipeThresholdPx -> Multiplier.TRIPLE
                                                else -> Multiplier.SINGLE
                                            }
                                        }
                                        currentOnScoreSelect(hit.segment, multiplier)
                                    }
                                } finally {
                                    gestureState = GestureState()
                                    highlightedSegmentIndex = null
                                    highlightedBull = false
                                    swipeIndicator = null
                                }
                            }
                        }
                    },
            ) {
                canvasCenter = Offset(size.width / 2, size.height / 2)
                canvasRadius = min(size.width, size.height) / 2 * 0.85f

                // Draw the wheel
                drawWheel(
                    center = canvasCenter,
                    radius = canvasRadius,
                    colorScheme = colorScheme,
                    highlightedSegmentIndex = highlightedSegmentIndex,
                    highlightedBull = highlightedBull,
                    swipeIndicator = swipeIndicator,
                )

                // Draw segment numbers on each wedge
                drawWedgeNumbers(
                    center = canvasCenter,
                    radius = canvasRadius,
                    textMeasurer = textMeasurer,
                    colorScheme = colorScheme,
                    highlightedSegmentIndex = highlightedSegmentIndex,
                    highlightedBull = highlightedBull,
                    swipeIndicator = swipeIndicator,
                )
            }

            // Floating multiplier indicator overlay
            // Bull has no triple - both directions show DOUBLE
            val indicatorMultiplier = when {
                swipeIndicator == null -> null
                highlightedBull -> Multiplier.DOUBLE
                swipeIndicator == SwipeDirection.UP -> Multiplier.DOUBLE
                swipeIndicator == SwipeDirection.DOWN -> Multiplier.TRIPLE
                else -> null
            }
            MultiplierIndicator(
                multiplier = indicatorMultiplier,
                modifier = Modifier.align(Alignment.Center),
            )

            // Round Miss button in top-left corner
            OutlinedButton(
                onClick = onMiss,
                enabled = canThrow,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(65.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("Miss", style = MaterialTheme.typography.labelSmall)
            }

            // Swipe hint (only when multiplier buttons are hidden)
            if (!showMultiplierButtons) {
                Text(
                    text = "Tap = Single • Swipe ↑ = Double • Swipe ↓ = Triple",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

private fun DrawScope.drawWheel(
    center: Offset,
    radius: Float,
    colorScheme: WheelColorScheme,
    highlightedSegmentIndex: Int?,
    highlightedBull: Boolean,
    swipeIndicator: SwipeDirection?,
) {
    val bullRadius = radius * BULL_RADIUS_RATIO

    // Draw wedges
    DARTBOARD_SEGMENT_ORDER.forEachIndexed { index, _ ->
        val (startAngle, endAngle) = getSegmentAngles(index)
        val sweepAngle = endAngle - startAngle
        val canvasStartAngle = toCanvasAngle(startAngle)

        // Draw wedge (full pie slice)
        drawArc(
            color = wedgeFill(index, highlightedSegmentIndex, swipeIndicator, colorScheme),
            startAngle = canvasStartAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
        )
    }

    // Draw divider lines between wedges
    val wireStroke = Stroke(width = 2.dp.toPx())
    (0 until 20).forEach { index ->
        val (startAngle, _) = getSegmentAngles(index)
        val angleRad = (startAngle - 90) * PI / 180
        val innerX = center.x + bullRadius * cos(angleRad).toFloat()
        val innerY = center.y + bullRadius * sin(angleRad).toFloat()
        val outerX = center.x + radius * cos(angleRad).toFloat()
        val outerY = center.y + radius * sin(angleRad).toFloat()
        drawLine(
            color = colorScheme.divider,
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = 2.dp.toPx(),
        )
    }

    // Draw bull (center circle)
    drawCircle(
        color = bullFill(highlightedBull, swipeIndicator, colorScheme),
        radius = bullRadius,
        center = center,
    )

    // Draw bull outline
    drawCircle(
        color = colorScheme.divider,
        radius = bullRadius,
        center = center,
        style = wireStroke,
    )

    // Draw outer circle
    drawCircle(
        color = colorScheme.divider,
        radius = radius,
        center = center,
        style = wireStroke,
    )

    // Outline the highlighted wedge/bull last (on top of the wires) so it stands out even when
    // the highlight fill is close in lightness to the wedge colors (e.g. light-theme green wedges)
    val highlightStroke = Stroke(width = 3.dp.toPx())
    highlightedSegmentIndex?.let { index ->
        val (startAngle, endAngle) = getSegmentAngles(index)
        drawArc(
            color = colorScheme.highlightOutline,
            startAngle = toCanvasAngle(startAngle),
            sweepAngle = endAngle - startAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = highlightStroke,
        )
    }
    if (highlightedBull) {
        drawCircle(
            color = colorScheme.highlightOutline,
            radius = bullRadius,
            center = center,
            style = highlightStroke,
        )
    }
}

private fun DrawScope.drawWedgeNumbers(
    center: Offset,
    radius: Float,
    textMeasurer: TextMeasurer,
    colorScheme: WheelColorScheme,
    highlightedSegmentIndex: Int?,
    highlightedBull: Boolean,
    swipeIndicator: SwipeDirection?,
) {
    val bullRadius = radius * BULL_RADIUS_RATIO
    // Position numbers towards the edge (like a real dartboard)
    val numberRadius = bullRadius + (radius - bullRadius) * 0.75f

    DARTBOARD_SEGMENT_ORDER.forEachIndexed { index, segment ->
        val (startAngle, endAngle) = getSegmentAngles(index)
        val midAngle = (startAngle + endAngle) / 2
        val angleRad = (midAngle - 90) * PI / 180

        val x = center.x + numberRadius * cos(angleRad).toFloat()
        val y = center.y + numberRadius * sin(angleRad).toFloat()

        // Highlighted wedges pick black/white from the actual fill; others keep the wedge text color
        val textColor = if (index == highlightedSegmentIndex) {
            textColorOn(wedgeFill(index, highlightedSegmentIndex, swipeIndicator, colorScheme))
        } else {
            getWedgeTextColor(index, colorScheme)
        }
        val textStyle = TextStyle(
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        val textLayoutResult = textMeasurer.measure(
            text = segment.toString(),
            style = textStyle,
        )

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x - textLayoutResult.size.width / 2,
                y - textLayoutResult.size.height / 2,
            ),
        )
    }

    // Draw "BULL" text in center
    val bullTextStyle = TextStyle(
        color = textColorOn(bullFill(highlightedBull, swipeIndicator, colorScheme)),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
    val bullTextResult = textMeasurer.measure(text = "BULL", style = bullTextStyle)
    drawText(
        textLayoutResult = bullTextResult,
        topLeft = Offset(
            center.x - bullTextResult.size.width / 2,
            center.y - bullTextResult.size.height / 2,
        ),
    )
}

private fun wedgeFill(
    index: Int,
    highlightedSegmentIndex: Int?,
    swipeIndicator: SwipeDirection?,
    colorScheme: WheelColorScheme,
): Color = if (index == highlightedSegmentIndex) {
    getHighlightColor(swipeIndicator, colorScheme)
} else {
    getWedgeColor(index, colorScheme)
}

/**
 * Bull has no triple: any swipe records a DOUBLE (50), so it always lights up in the double color.
 */
private fun bullFill(
    highlightedBull: Boolean,
    swipeIndicator: SwipeDirection?,
    colorScheme: WheelColorScheme,
): Color = when {
    !highlightedBull -> colorScheme.bull
    swipeIndicator != null -> colorScheme.highlightDouble
    else -> colorScheme.highlight
}

private fun getHighlightColor(
    swipeIndicator: SwipeDirection?,
    colorScheme: WheelColorScheme,
): Color = when (swipeIndicator) {
    SwipeDirection.UP -> colorScheme.highlightDouble
    SwipeDirection.DOWN -> colorScheme.highlightTriple
    null -> colorScheme.highlight
}
