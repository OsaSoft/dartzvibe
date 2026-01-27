package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Dartboard segment order, clockwise starting from the top (12 o'clock position).
 * This is the standard dartboard layout used worldwide.
 */
val DARTBOARD_SEGMENT_ORDER: List<Int> = listOf(
    20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5,
)

/** Bull radius as a fraction of the total wheel radius */
const val BULL_RADIUS_RATIO: Float = 0.15f

/**
 * Result of a hit test on the dartboard wheel.
 *
 * @property segment The segment number (1-20 for wedges, 25 for bull)
 * @property segmentIndex The index in [DARTBOARD_SEGMENT_ORDER] (0-19), null for bull
 * @property isBull True if the hit was on the bull (center)
 */
data class DartboardHit(
    val segment: Int,
    val segmentIndex: Int?,
    val isBull: Boolean,
)

/**
 * Gets the segment index (0-19) at a given angle.
 * Angle 0 is at the top (12 o'clock), increasing clockwise.
 *
 * @param angleDegrees Angle in degrees (0-360, with 0 at top)
 * @return The segment index in [DARTBOARD_SEGMENT_ORDER]
 */
fun getSegmentIndexAtAngle(angleDegrees: Float): Int {
    val degreesPerSegment = 360f / 20f
    val adjustedAngle = (angleDegrees + degreesPerSegment / 2 + 360f) % 360f
    return (adjustedAngle / degreesPerSegment).toInt() % 20
}

/**
 * Hit-tests a position on the dartboard wheel.
 *
 * @param position The touch position
 * @param center The center of the wheel
 * @param radius The radius of the wheel
 * @return The [DartboardHit] result, or null if outside the wheel
 */
fun getHitAtPosition(
    position: Offset,
    center: Offset,
    radius: Float,
): DartboardHit? {
    val dx = position.x - center.x
    val dy = position.y - center.y
    val distance = sqrt(dx * dx + dy * dy)
    val normalizedRadius = distance / radius

    // Outside the wheel
    if (normalizedRadius > 1f) {
        return null
    }

    // Bull hit (center)
    if (normalizedRadius <= BULL_RADIUS_RATIO) {
        return DartboardHit(
            segment = 25,
            segmentIndex = null,
            isBull = true,
        )
    }

    // Calculate angle (0 at top, clockwise)
    val angleRadians = atan2(dx, -dy)
    val angleDegrees = (angleRadians * 180 / PI).toFloat()
    val normalizedAngle = (angleDegrees + 360f) % 360f

    val segmentIndex = getSegmentIndexAtAngle(normalizedAngle)
    val segment = DARTBOARD_SEGMENT_ORDER[segmentIndex]

    return DartboardHit(
        segment = segment,
        segmentIndex = segmentIndex,
        isBull = false,
    )
}

/**
 * Gets the start and end angles for a segment in degrees.
 * Angles are measured from the top (12 o'clock), clockwise.
 *
 * @param segmentIndex The segment index (0-19) in [DARTBOARD_SEGMENT_ORDER]
 * @return Pair of (startAngle, endAngle) in degrees
 */
fun getSegmentAngles(segmentIndex: Int): Pair<Float, Float> {
    val degreesPerSegment = 360f / 20f
    val startAngle = segmentIndex * degreesPerSegment - degreesPerSegment / 2
    val endAngle = startAngle + degreesPerSegment
    return Pair(startAngle, endAngle)
}

/**
 * Converts an angle from "top-centered clockwise" to Canvas.drawArc format.
 * Canvas uses 0 at 3 o'clock (right), increasing clockwise.
 */
fun toCanvasAngle(topCenteredAngle: Float): Float = topCenteredAngle - 90f
