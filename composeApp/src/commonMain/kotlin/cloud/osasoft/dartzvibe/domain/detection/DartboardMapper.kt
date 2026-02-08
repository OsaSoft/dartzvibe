package cloud.osasoft.dartzvibe.domain.detection

import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.ui.screen.game.components.DARTBOARD_SEGMENT_ORDER
import cloud.osasoft.dartzvibe.ui.screen.game.components.getSegmentIndexAtAngle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Ring radius ratios as fractions of the overall board radius.
 * Based on standard dartboard proportions from dart-sense.
 */
private const val DOUBLE_BULL_MAX = 0.032f
private const val SINGLE_BULL_MAX = 0.079f
private const val INNER_SINGLE_MAX = 0.485f
private const val TRIPLE_MAX = 0.534f
private const val OUTER_SINGLE_MAX = 0.838f
private const val DOUBLE_MAX = 1.0f

/**
 * Maps a normalized board position to a dartboard segment and multiplier.
 *
 * Coordinates are relative to the board center, with range -1..1 in both axes.
 * The board outer edge is at radius 1.0.
 *
 * @param normalizedX X position relative to board center (-1..1)
 * @param normalizedY Y position relative to board center (-1..1)
 * @return Pair of (segment number, multiplier), or null if the dart missed the board
 */
fun mapPosition(normalizedX: Float, normalizedY: Float): Pair<Int, Multiplier>? {
    val radius = sqrt(normalizedX * normalizedX + normalizedY * normalizedY)

    if (radius > DOUBLE_MAX) return null

    if (radius <= DOUBLE_BULL_MAX) return Pair(25, Multiplier.DOUBLE)
    if (radius <= SINGLE_BULL_MAX) return Pair(25, Multiplier.SINGLE)

    val angleDegrees = positionToAngle(normalizedX, normalizedY)
    val segmentIndex = getSegmentIndexAtAngle(angleDegrees)
    val segment = DARTBOARD_SEGMENT_ORDER[segmentIndex]

    val multiplier = when {
        radius <= INNER_SINGLE_MAX -> Multiplier.SINGLE
        radius <= TRIPLE_MAX -> Multiplier.TRIPLE
        radius <= OUTER_SINGLE_MAX -> Multiplier.SINGLE
        else -> Multiplier.DOUBLE
    }

    return Pair(segment, multiplier)
}

/**
 * Converts normalized (x, y) to an angle in degrees.
 * 0 degrees is at the top (12 o'clock), increasing clockwise.
 */
private fun positionToAngle(x: Float, y: Float): Float {
    val angleRadians = atan2(x.toDouble(), (-y).toDouble())
    val angleDegrees = (angleRadians * 180.0 / PI).toFloat()
    return (angleDegrees + 360f) % 360f
}
