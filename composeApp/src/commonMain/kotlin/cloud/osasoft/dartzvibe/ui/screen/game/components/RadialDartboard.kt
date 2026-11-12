package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.SwipeMultiplierMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val SWIPE_THRESHOLD_DP = 30f

/** Padding around the whole input (multiplier buttons, board, swipe hint). */
private val BoardPadding = 8.dp

/** Gap between the multiplier buttons or swipe hint and the board. */
private val BoardRowGap = 4.dp

/**
 * Radial dartboard wheel input component.
 *
 * A simplified wheel layout with 20 wedges in dartboard segment order and a bull in the center.
 * Multiplier is determined by S/D/T buttons or, when they are hidden, by swipe direction according to
 * [swipeMultiplierMode] (see [swipeMultiplier]).
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun RadialDartboard(
    selectedMultiplier: Multiplier,
    canThrow: Boolean,
    showMultiplierButtons: Boolean,
    swipeMultiplierMode: SwipeMultiplierMode,
    onMultiplierChange: (Multiplier) -> Unit,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onMiss: () -> Unit,
    modifier: Modifier = Modifier,
    // Height available to the whole input; the board shrinks to fit it (down to MinBoardSide). Null: full width.
    maxHeight: Dp? = null,
) {
    val colorScheme = wheelColorScheme()
    val textMeasurer = rememberTextMeasurer()

    // The pointerInput gesture handler below is long-lived: it is not restarted when these change
    // (restarting it would cancel a drag in progress), so it must read them through updated state.
    // Capturing the parameters directly made taps record the multiplier selected when the handler
    // started, e.g. T20 again after switching from Triple to Double.
    val currentSelectedMultiplier by rememberUpdatedState(selectedMultiplier)
    val currentOnScoreSelect by rememberUpdatedState(onScoreSelect)

    var highlightedSegmentIndex by remember { mutableStateOf<Int?>(null) }
    var highlightedBull by remember { mutableStateOf(false) }
    // Multiplier the current swipe would record (null = not a swipe yet, i.e. single)
    var swipePreview by remember { mutableStateOf<Multiplier?>(null) }

    BoardColumn(
        heightLeft = maxHeight,
        modifier = modifier.padding(BoardPadding),
        // Optional multiplier buttons
        top = if (showMultiplierButtons) {
            {
                MultiplierButtonsRow(
                    selectedMultiplier = selectedMultiplier,
                    onMultiplierChange = onMultiplierChange,
                )
            }
        } else {
            null
        },
        // Swipe hint (only when multiplier buttons are hidden). Laid out below the board, not over it,
        // so it can wrap at large font scales or on narrow screens without covering any wedge.
        bottom = if (!showMultiplierButtons) {
            {
                Text(
                    text = when (swipeMultiplierMode) {
                        SwipeMultiplierMode.VERTICAL -> "Tap = Single • Swipe ↑ = Double • Swipe ↓ = Triple"
                        SwipeMultiplierMode.RADIAL -> "Tap = Single • Swipe to center = Triple • Swipe out = Double"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            null
        },
    ) {
        // Dartboard wheel Canvas, sized by BoardColumn
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            var canvasCenter by remember { mutableStateOf(Offset.Zero) }
            var canvasRadius by remember { mutableStateOf(0f) }
            // Shrinks with the board so it never covers the wheel; full size on tall phones
            val missSize = missButtonSize(canvasSide = maxWidth.value, maxSize = MaxMissButtonSize.value).dp

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(canThrow, showMultiplierButtons, swipeMultiplierMode) {
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
                                highlightedSegmentIndex = hit.segmentIndex
                                highlightedBull = hit.isBull
                                swipePreview = null

                                // Same inputs for the live preview and the recorded throw, so they always agree
                                val swipeMultiplierAt = { position: Offset ->
                                    swipeMultiplier(
                                        start = down.position,
                                        current = position,
                                        center = canvasCenter,
                                        thresholdPx = swipeThresholdPx,
                                        mode = swipeMultiplierMode,
                                        isBull = hit.isBull,
                                    )
                                }

                                var lastPosition = down.position

                                // finally: clear the highlight even if the handler is cancelled mid-drag
                                // (e.g. pointerInput restarts), so it can't stay stuck on the board
                                try {
                                    // false = the gesture was cancelled rather than lifted: record nothing
                                    val completed = drag(down.id) { change ->
                                        change.consume()
                                        lastPosition = change.position
                                        swipePreview = swipeMultiplierAt(lastPosition)
                                    }

                                    if (completed) {
                                        // Lifted - determine multiplier and submit score (no swipe = single)
                                        val multiplier = swipeMultiplierAt(lastPosition) ?: Multiplier.SINGLE
                                        currentOnScoreSelect(hit.segment, multiplier)
                                    }
                                } finally {
                                    highlightedSegmentIndex = null
                                    highlightedBull = false
                                    swipePreview = null
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
                    swipePreview = swipePreview,
                )

                // Draw segment numbers on each wedge
                drawWedgeNumbers(
                    center = canvasCenter,
                    radius = canvasRadius,
                    textMeasurer = textMeasurer,
                    colorScheme = colorScheme,
                    highlightedSegmentIndex = highlightedSegmentIndex,
                    highlightedBull = highlightedBull,
                    swipePreview = swipePreview,
                )
            }

            // Floating multiplier indicator overlay (bull swipes already resolve to DOUBLE)
            MultiplierIndicator(
                multiplier = swipePreview,
                modifier = Modifier.align(Alignment.Center),
            )

            // Round Miss button in top-left corner
            OutlinedButton(
                onClick = onMiss,
                enabled = canThrow,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(missSize),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("Miss", style = MaterialTheme.typography.labelSmall)
            }
        }
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

/**
 * A column of the optional [top] row, the square [board] and the optional [bottom] row, 4 dp apart. The board is as
 * wide as the column, unless [heightLeft] (the height available to the whole input, padding included) is set and
 * too short: then it shrinks to fit (see [fitBoardSide]) and is centred. The other rows are measured first, so the
 * board's size depends only on them and [heightLeft].
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun BoardColumn(
    heightLeft: Dp?,
    top: (@Composable () -> Unit)?,
    bottom: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    board: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(top ?: {}, board, bottom ?: {}),
        modifier = modifier,
    ) { (topMeasurables, boardMeasurables, bottomMeasurables), constraints ->
        val width = constraints.maxWidth
        val rowConstraints = Constraints(maxWidth = width)
        val topPlaceable = topMeasurables.firstOrNull()?.measure(rowConstraints)
        val bottomPlaceable = bottomMeasurables.firstOrNull()?.measure(rowConstraints)
        val gap = BoardRowGap.roundToPx()
        val rows = listOfNotNull(topPlaceable, bottomPlaceable)
        val rowsHeight = rows.sumOf { it.height } + gap * rows.size
        val side = fitBoardSide(
            widthPx = width,
            // In pixels, like the padding modifier, so rounding never leaves a pixel over
            heightLeftPx = heightLeft?.let {
                it.roundToPx() -
                    columnChromePx(
                        paddingPx = BoardPadding.roundToPx(),
                        gapPx = gap,
                        rowHeightsPx = rows.map { row -> row.height },
                        flexibleRows = 1,
                    )
            },
            minSidePx = MinBoardSide.roundToPx(),
        )
        val boardPlaceable = boardMeasurables.first().measure(Constraints.fixed(side, side))
        val height = constraints.constrainHeight(rowsHeight + side)

        layout(width, height) {
            var y = 0
            topPlaceable?.let {
                it.place(x = 0, y = y)
                y += it.height + gap
            }
            boardPlaceable.place(x = (width - side) / 2, y = y)
            y += side + gap
            bottomPlaceable?.place(x = 0, y = y)
        }
    }
}

private fun DrawScope.drawWheel(
    center: Offset,
    radius: Float,
    colorScheme: WheelColorScheme,
    highlightedSegmentIndex: Int?,
    highlightedBull: Boolean,
    swipePreview: Multiplier?,
) {
    val bullRadius = radius * BULL_RADIUS_RATIO

    // Draw wedges
    DARTBOARD_SEGMENT_ORDER.forEachIndexed { index, _ ->
        val (startAngle, endAngle) = getSegmentAngles(index)
        val sweepAngle = endAngle - startAngle
        val canvasStartAngle = toCanvasAngle(startAngle)

        // Draw wedge (full pie slice)
        drawArc(
            color = wedgeFill(index, highlightedSegmentIndex, swipePreview, colorScheme),
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
        color = bullFill(highlightedBull, swipePreview, colorScheme),
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

/** [desired] unless the wheel is too small for it (see [boardLabelSizePx]); exactly [desired] on a full-size board. */
private fun DrawScope.boardLabelSize(
    desired: TextUnit,
    radius: Float,
    maxToRadius: Float,
): TextUnit {
    val desiredPx = desired.toPx()
    val sizePx = boardLabelSizePx(desiredPx = desiredPx, radiusPx = radius, maxToRadius = maxToRadius)
    return if (sizePx < desiredPx) sizePx.toSp() else desired
}

private fun DrawScope.drawWedgeNumbers(
    center: Offset,
    radius: Float,
    textMeasurer: TextMeasurer,
    colorScheme: WheelColorScheme,
    highlightedSegmentIndex: Int?,
    highlightedBull: Boolean,
    swipePreview: Multiplier?,
) {
    val bullRadius = radius * BULL_RADIUS_RATIO
    // Position numbers towards the edge (like a real dartboard)
    val numberRadius = bullRadius + (radius - bullRadius) * 0.75f
    // 16 sp on a full-size board; smaller on a board shrunk to fit a short screen, so the numbers never touch
    val wedgeLabelSize = boardLabelSize(desired = 16.sp, radius = radius, maxToRadius = WEDGE_LABEL_MAX_TO_RADIUS)

    DARTBOARD_SEGMENT_ORDER.forEachIndexed { index, segment ->
        val (startAngle, endAngle) = getSegmentAngles(index)
        val midAngle = (startAngle + endAngle) / 2
        val angleRad = (midAngle - 90) * PI / 180

        val x = center.x + numberRadius * cos(angleRad).toFloat()
        val y = center.y + numberRadius * sin(angleRad).toFloat()

        // Highlighted wedges pick black/white from the actual fill; others keep the wedge text color
        val textColor = if (index == highlightedSegmentIndex) {
            textColorOn(wedgeFill(index, highlightedSegmentIndex, swipePreview, colorScheme))
        } else {
            getWedgeTextColor(index, colorScheme)
        }
        val textStyle = TextStyle(
            color = textColor,
            fontSize = wedgeLabelSize,
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
        color = textColorOn(bullFill(highlightedBull, swipePreview, colorScheme)),
        fontSize = boardLabelSize(desired = 12.sp, radius = radius, maxToRadius = BULL_LABEL_MAX_TO_RADIUS),
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
    swipePreview: Multiplier?,
    colorScheme: WheelColorScheme,
): Color = if (index == highlightedSegmentIndex) {
    getHighlightColor(swipePreview, colorScheme)
} else {
    getWedgeColor(index, colorScheme)
}

/**
 * Bull has no triple: any swipe records a DOUBLE (50), so it always lights up in the double color.
 */
private fun bullFill(
    highlightedBull: Boolean,
    swipePreview: Multiplier?,
    colorScheme: WheelColorScheme,
): Color = when {
    !highlightedBull -> colorScheme.bull
    swipePreview != null -> colorScheme.highlightDouble
    else -> colorScheme.highlight
}

private fun getHighlightColor(
    swipePreview: Multiplier?,
    colorScheme: WheelColorScheme,
): Color = when (swipePreview) {
    Multiplier.DOUBLE -> colorScheme.highlightDouble
    Multiplier.TRIPLE -> colorScheme.highlightTriple
    Multiplier.SINGLE, null -> colorScheme.highlight
}
