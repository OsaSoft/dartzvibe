package cloud.osasoft.dartzvibe.ui.screen.visiondebug.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.domain.detection.DetectedDart
import cloud.osasoft.dartzvibe.domain.detection.DetectionResult

private val BOARD_OUTLINE_COLOR = Color.Green
private val RING_GUIDE_COLOR = Color.Green.copy(alpha = 0.3f)
private val DART_MARKER_COLOR = Color.Red
private val DART_LABEL_COLOR = Color.White
private val DART_LABEL_BG_COLOR = Color.Black.copy(alpha = 0.6f)

private const val DOUBLE_BULL_RATIO = 0.032f
private const val SINGLE_BULL_RATIO = 0.079f
private const val TRIPLE_INNER_RATIO = 0.485f
private const val TRIPLE_OUTER_RATIO = 0.534f
private const val DOUBLE_INNER_RATIO = 0.838f

@Suppress("ktlint:standard:function-naming")
@Composable
fun DetectionOverlay(
    result: DetectionResult?,
    showBoardOverlay: Boolean,
    showRingGuides: Boolean,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        if (result == null || !result.boardDetected) return@Canvas

        val centerX = (result.boardCenterX ?: 0.5f) * size.width
        val centerY = (result.boardCenterY ?: 0.5f) * size.height
        val boardRadius = (result.boardRadius ?: 0.35f) * size.width
        val center = Offset(centerX, centerY)

        if (showBoardOverlay) {
            drawBoardOutline(center, boardRadius)
        }

        if (showRingGuides) {
            drawRingGuides(center, boardRadius)
        }

        result.darts.forEach { dart ->
            drawDartMarker(dart, center, boardRadius, textMeasurer)
        }
    }
}

private fun DrawScope.drawBoardOutline(center: Offset, radius: Float) {
    drawCircle(
        color = BOARD_OUTLINE_COLOR,
        radius = radius,
        center = center,
        style = Stroke(width = 2f),
    )
}

private fun DrawScope.drawRingGuides(center: Offset, radius: Float) {
    listOf(
        DOUBLE_BULL_RATIO,
        SINGLE_BULL_RATIO,
        TRIPLE_INNER_RATIO,
        TRIPLE_OUTER_RATIO,
        DOUBLE_INNER_RATIO,
    ).forEach { ratio ->
        drawCircle(
            color = RING_GUIDE_COLOR,
            radius = radius * ratio,
            center = center,
            style = Stroke(width = 1f),
        )
    }
}

private fun DrawScope.drawDartMarker(
    dart: DetectedDart,
    boardCenter: Offset,
    boardRadius: Float,
    textMeasurer: TextMeasurer,
) {
    val dartX = boardCenter.x + dart.normalizedX * boardRadius
    val dartY = boardCenter.y + dart.normalizedY * boardRadius
    val dartCenter = Offset(dartX, dartY)

    drawCircle(
        color = DART_MARKER_COLOR,
        radius = 8f,
        center = dartCenter,
    )
    drawCircle(
        color = Color.White,
        radius = 8f,
        center = dartCenter,
        style = Stroke(width = 2f),
    )

    val label = formatDartLabel(dart)
    val textLayout = textMeasurer.measure(
        text = label,
        style = TextStyle(
            color = DART_LABEL_COLOR,
            fontSize = 12.sp,
        ),
    )

    val labelOffset = Offset(dartX + 12f, dartY - textLayout.size.height / 2f)

    drawRect(
        color = DART_LABEL_BG_COLOR,
        topLeft = Offset(labelOffset.x - 2f, labelOffset.y - 2f),
        size = androidx.compose.ui.geometry.Size(
            textLayout.size.width + 4f,
            textLayout.size.height + 4f,
        ),
    )

    drawText(
        textLayoutResult = textLayout,
        topLeft = labelOffset,
    )
}

private fun formatDartLabel(dart: DetectedDart): String {
    if (dart.segment == null || dart.multiplier == null) return "MISS"

    return when {
        dart.segment == 25 && dart.multiplier == Multiplier.DOUBLE -> "DB"

        dart.segment == 25 -> "SB"

        else -> {
            val prefix = when (dart.multiplier) {
                Multiplier.SINGLE -> "S"
                Multiplier.DOUBLE -> "D"
                Multiplier.TRIPLE -> "T"
            }
            "$prefix${dart.segment}"
        }
    }
}
