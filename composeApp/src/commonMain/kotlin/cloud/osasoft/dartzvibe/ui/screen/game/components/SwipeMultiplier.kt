package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.ui.geometry.Offset
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.SwipeMultiplierMode
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Works out the multiplier a radial-dartboard swipe selects. Used both for the live drag feedback
 * and for the recorded throw, so the two always agree.
 *
 * @param start where the finger touched down
 * @param current where the finger is now (or where it lifted)
 * @param center the center of the board
 * @param thresholdPx minimum movement (strictly greater) that counts as a swipe
 * @param mode how the swipe direction maps to a multiplier
 * @param isBull true when [start] is on the bull
 * @return [Multiplier.DOUBLE] or [Multiplier.TRIPLE], or null when the gesture is not a swipe (a tap = single)
 *
 * Rules:
 * - [SwipeMultiplierMode.VERTICAL]: vertical movement beyond the threshold; up = DOUBLE, down = TRIPLE.
 * - [SwipeMultiplierMode.RADIAL]: the drag is projected onto the unit vector from [center] to [start].
 *   Toward the center (negative) = TRIPLE, away from it (positive) = DOUBLE. The radial component must
 *   pass the threshold *and* be strictly larger than the tangential component, i.e. the drag must point
 *   within 45° of the radial line; mostly sideways drags (along the ring) count as no swipe.
 * - Bull: there is no triple bull, so any swipe is DOUBLE (bullseye). In vertical mode that keeps the
 *   original rule (vertical movement beyond the threshold); in radial mode the direction from the
 *   center is meaningless, so any movement beyond the threshold counts.
 */
fun swipeMultiplier(
    start: Offset,
    current: Offset,
    center: Offset,
    thresholdPx: Float,
    mode: SwipeMultiplierMode,
    isBull: Boolean,
): Multiplier? {
    val drag = current - start
    return when (mode) {
        SwipeMultiplierMode.VERTICAL -> verticalSwipeMultiplier(drag, thresholdPx, isBull)
        SwipeMultiplierMode.RADIAL -> radialSwipeMultiplier(drag, start - center, thresholdPx, isBull)
    }
}

private fun verticalSwipeMultiplier(
    drag: Offset,
    thresholdPx: Float,
    isBull: Boolean,
): Multiplier? = when {
    abs(drag.y) <= thresholdPx -> null

    isBull -> Multiplier.DOUBLE

    // Screen y grows downward: negative = up
    drag.y < 0f -> Multiplier.DOUBLE

    else -> Multiplier.TRIPLE
}

private fun radialSwipeMultiplier(
    drag: Offset,
    outward: Offset,
    thresholdPx: Float,
    isBull: Boolean,
): Multiplier? {
    if (isBull) {
        return if (drag.getDistance() > thresholdPx) Multiplier.DOUBLE else null
    }
    val outwardLength = sqrt(outward.x * outward.x + outward.y * outward.y)
    if (outwardLength == 0f) return null

    val unitX = outward.x / outwardLength
    val unitY = outward.y / outwardLength
    val radial = drag.x * unitX + drag.y * unitY
    val tangential = drag.x * unitY - drag.y * unitX

    return when {
        abs(radial) <= thresholdPx -> null
        abs(radial) <= abs(tangential) -> null
        radial < 0f -> Multiplier.TRIPLE
        else -> Multiplier.DOUBLE
    }
}
