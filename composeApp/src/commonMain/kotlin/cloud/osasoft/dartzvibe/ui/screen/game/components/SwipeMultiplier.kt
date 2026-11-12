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
        SwipeMultiplierMode.VERTICAL -> verticalSwipeMultiplier(
            deltaY = drag.y,
            thresholdPx = thresholdPx,
            isBull = isBull,
        )

        SwipeMultiplierMode.RADIAL -> radialSwipeMultiplier(drag, start - center, thresholdPx, isBull)
    }
}

/**
 * The vertical swipe rule, shared by the radial board's [SwipeMultiplierMode.VERTICAL] and the grid keypad, for both
 * the live preview and the recorded throw.
 *
 * @param deltaY vertical movement from the touch-down point, in px (screen y grows downward: negative = up)
 * @param thresholdPx minimum movement (strictly greater) that counts as a swipe
 * @param isBull true for the bull, which has no triple: any swipe beyond the threshold is DOUBLE
 * @return [Multiplier.DOUBLE] (up) or [Multiplier.TRIPLE] (down), or null when the gesture is not a swipe (a tap or a
 *   small wobble = single)
 */
fun verticalSwipeMultiplier(
    deltaY: Float,
    thresholdPx: Float,
    isBull: Boolean,
): Multiplier? = when {
    abs(deltaY) <= thresholdPx -> null
    isBull -> Multiplier.DOUBLE
    deltaY < 0f -> Multiplier.DOUBLE
    else -> Multiplier.TRIPLE
}

/**
 * What a grid keypad key enters for a gesture that lifted [deltaY] px below where it touched down (negative = above):
 * [verticalSwipeMultiplier], or SINGLE for a tap or a wobble within [thresholdPx]. The key's live label uses it too.
 */
internal fun classifyKeySwipe(
    deltaY: Float,
    thresholdPx: Float,
    isBull: Boolean,
): Multiplier =
    verticalSwipeMultiplier(deltaY = deltaY, thresholdPx = thresholdPx, isBull = isBull) ?: Multiplier.SINGLE

/** The grid keypad's swipe pill: the [multiplier] previewed by the key ([key]: its number, 25 for Bull) that set it. */
internal data class SwipePill(
    val key: Int,
    val multiplier: Multiplier,
)

/**
 * The pill after [key] reports its preview ([multiplier]; null = none). A key takes the pill over when it shows a
 * multiplier, but clears it only if it set it, so a tap on another key (a second finger) can't hide a swipe in progress.
 */
internal fun updateSwipePill(
    current: SwipePill?,
    key: Int,
    multiplier: Multiplier?,
): SwipePill? = when {
    multiplier != null -> SwipePill(key = key, multiplier = multiplier)
    current?.key == key -> null
    else -> current
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
