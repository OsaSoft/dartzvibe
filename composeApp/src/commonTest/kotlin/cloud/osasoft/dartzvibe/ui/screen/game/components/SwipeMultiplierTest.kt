package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.ui.geometry.Offset
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.SwipeMultiplierMode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val CENTER = Offset(500f, 500f)
private const val RADIUS = 400f
private const val THRESHOLD = 30f

// Touch-down points on the wheel (screen y grows downward)
private val TOP_20 = Offset(500f, 200f)
private val BOTTOM_3 = Offset(500f, 800f)
private val RIGHT_6 = Offset(800f, 500f)
private val LEFT_11 = Offset(200f, 500f)
private val UPPER_RIGHT_4 = Offset(700f, 350f) // outward unit vector (0.8, -0.6)
private val LOWER_LEFT_16 = Offset(300f, 650f) // outward unit vector (-0.8, 0.6)

private val UP = Offset(0f, -60f)
private val DOWN = Offset(0f, 60f)
private val LEFT = Offset(-60f, 0f)
private val RIGHT = Offset(60f, 0f)

class SwipeMultiplierTest : FreeSpec({

    data class SwipeCase(
        val name: String,
        val start: Offset,
        val drag: Offset,
        val expectedSegment: Int,
        val expected: Multiplier?,
    ) {
        override fun toString(): String = "$name -> ${expected ?: "no swipe"}"
    }

    fun swipe(
        start: Offset,
        drag: Offset,
        mode: SwipeMultiplierMode,
        isBull: Boolean = false,
    ): Multiplier? = swipeMultiplier(
        start = start,
        current = start + drag,
        center = CENTER,
        thresholdPx = THRESHOLD,
        mode = mode,
        isBull = isBull,
    )

    fun checkCase(
        case: SwipeCase,
        mode: SwipeMultiplierMode,
    ) {
        // GIVEN a touch-down on the expected wedge
        getHitAtPosition(case.start, CENTER, RADIUS)?.segment shouldBe case.expectedSegment

        // WHEN the finger is dragged
        val result = swipe(case.start, case.drag, mode)

        // THEN the expected multiplier is selected
        result shouldBe case.expected
    }

    "Radial mode (like a real board)" - {
        "Should pick triple toward the center and double away from it, on every side of the board" - {
            withData(
                SwipeCase("20 at top, swipe down", TOP_20, DOWN, 20, Multiplier.TRIPLE),
                SwipeCase("20 at top, swipe up", TOP_20, UP, 20, Multiplier.DOUBLE),
                SwipeCase("3 at bottom, swipe up", BOTTOM_3, UP, 3, Multiplier.TRIPLE),
                SwipeCase("3 at bottom, swipe down", BOTTOM_3, DOWN, 3, Multiplier.DOUBLE),
                SwipeCase("6 on the right, swipe left", RIGHT_6, LEFT, 6, Multiplier.TRIPLE),
                SwipeCase("6 on the right, swipe right", RIGHT_6, RIGHT, 6, Multiplier.DOUBLE),
                SwipeCase("11 on the left, swipe right", LEFT_11, RIGHT, 11, Multiplier.TRIPLE),
                SwipeCase("11 on the left, swipe left", LEFT_11, LEFT, 11, Multiplier.DOUBLE),
                SwipeCase("4 upper right, swipe down-left", UPPER_RIGHT_4, Offset(-80f, 60f), 4, Multiplier.TRIPLE),
                SwipeCase("4 upper right, swipe up-right", UPPER_RIGHT_4, Offset(80f, -60f), 4, Multiplier.DOUBLE),
                SwipeCase("16 lower left, swipe up-right", LOWER_LEFT_16, Offset(80f, -60f), 16, Multiplier.TRIPLE),
                SwipeCase("16 lower left, swipe down-left", LOWER_LEFT_16, Offset(-80f, 60f), 16, Multiplier.DOUBLE),
            ) { case -> checkCase(case, SwipeMultiplierMode.RADIAL) }
        }

        "Should treat drags up to the threshold as a tap" - {
            withData(
                SwipeCase("20, no movement", TOP_20, Offset.Zero, 20, null),
                SwipeCase("20, inward exactly at threshold", TOP_20, Offset(0f, 30f), 20, null),
                SwipeCase("20, inward just past threshold", TOP_20, Offset(0f, 31f), 20, Multiplier.TRIPLE),
                SwipeCase("6, outward exactly at threshold", RIGHT_6, Offset(30f, 0f), 6, null),
                SwipeCase("6, outward just past threshold", RIGHT_6, Offset(31f, 0f), 6, Multiplier.DOUBLE),
            ) { case -> checkCase(case, SwipeMultiplierMode.RADIAL) }
        }

        "Should ignore drags that are mostly along the ring" - {
            withData(
                SwipeCase("20, purely sideways", TOP_20, RIGHT, 20, null),
                SwipeCase("20, mostly sideways with radial part past threshold", TOP_20, Offset(60f, 40f), 20, null),
                SwipeCase("20, exactly 45 degrees (radial ties tangential)", TOP_20, Offset(40f, 40f), 20, null),
                SwipeCase("20, mostly inward with a sideways part", TOP_20, Offset(30f, 50f), 20, Multiplier.TRIPLE),
                SwipeCase("6, purely vertical", RIGHT_6, DOWN, 6, null),
                SwipeCase("6, mostly outward with a vertical part", RIGHT_6, Offset(50f, -30f), 6, Multiplier.DOUBLE),
            ) { case -> checkCase(case, SwipeMultiplierMode.RADIAL) }
        }

        "Should keep triple when a long inward swipe passes over the center" {
            // GIVEN a touch-down on 20 at the top
            // WHEN the finger is dragged down past the bull to the far side of the board
            val result = swipe(TOP_20, Offset(0f, 650f), SwipeMultiplierMode.RADIAL)

            // THEN the drag still counts as toward the center
            result shouldBe Multiplier.TRIPLE
        }
    }

    "Vertical mode (up = double, down = triple)" - {
        "Should pick by vertical direction regardless of the wedge" - {
            withData(
                SwipeCase("20 at top, swipe down", TOP_20, DOWN, 20, Multiplier.TRIPLE),
                SwipeCase("20 at top, swipe up", TOP_20, UP, 20, Multiplier.DOUBLE),
                SwipeCase("3 at bottom, swipe up", BOTTOM_3, UP, 3, Multiplier.DOUBLE),
                SwipeCase("3 at bottom, swipe down", BOTTOM_3, DOWN, 3, Multiplier.TRIPLE),
                SwipeCase("6 on the right, swipe left", RIGHT_6, LEFT, 6, null),
                SwipeCase("6 on the right, swipe down", RIGHT_6, DOWN, 6, Multiplier.TRIPLE),
                SwipeCase("11 on the left, swipe up", LEFT_11, UP, 11, Multiplier.DOUBLE),
                SwipeCase("16 lower left, swipe up-right", LOWER_LEFT_16, Offset(80f, -60f), 16, Multiplier.DOUBLE),
                SwipeCase("4 upper right, swipe down-left", UPPER_RIGHT_4, Offset(-80f, 60f), 4, Multiplier.TRIPLE),
                SwipeCase("20, down exactly at threshold", TOP_20, Offset(0f, 30f), 20, null),
                SwipeCase("20, up just past threshold", TOP_20, Offset(0f, -31f), 20, Multiplier.DOUBLE),
                SwipeCase("20, no movement", TOP_20, Offset.Zero, 20, null),
            ) { case -> checkCase(case, SwipeMultiplierMode.VERTICAL) }
        }
    }

    "Bull" - {
        data class BullCase(
            val mode: SwipeMultiplierMode,
            val start: Offset,
            val drag: Offset,
            val expected: Multiplier?,
        ) {
            override fun toString(): String = "$mode, from $start by $drag -> ${expected ?: "no swipe"}"
        }

        "Should record a bullseye for any swipe and a single bull for a tap" - {
            withData(
                BullCase(SwipeMultiplierMode.RADIAL, CENTER, RIGHT, Multiplier.DOUBLE),
                BullCase(SwipeMultiplierMode.RADIAL, CENTER, UP, Multiplier.DOUBLE),
                BullCase(SwipeMultiplierMode.RADIAL, CENTER, DOWN, Multiplier.DOUBLE),
                BullCase(SwipeMultiplierMode.RADIAL, Offset(510f, 490f), DOWN, Multiplier.DOUBLE),
                BullCase(SwipeMultiplierMode.RADIAL, Offset(510f, 490f), Offset(-40f, 40f), Multiplier.DOUBLE),
                BullCase(SwipeMultiplierMode.RADIAL, CENTER, Offset(20f, 20f), null),
                BullCase(SwipeMultiplierMode.RADIAL, CENTER, Offset.Zero, null),
                BullCase(SwipeMultiplierMode.VERTICAL, CENTER, UP, Multiplier.DOUBLE),
                BullCase(SwipeMultiplierMode.VERTICAL, CENTER, DOWN, Multiplier.DOUBLE),
                BullCase(SwipeMultiplierMode.VERTICAL, CENTER, Offset.Zero, null),
            ) { case ->
                // GIVEN a touch-down on the bull
                val hit = getHitAtPosition(case.start, CENTER, RADIUS)
                hit?.isBull shouldBe true

                // WHEN the finger is dragged
                val result = swipe(case.start, case.drag, case.mode, isBull = true)

                // THEN any swipe is a bullseye (never a triple)
                result shouldBe case.expected
                result shouldNotBe Multiplier.TRIPLE
            }
        }

        "Should keep the original vertical-only bull rule in vertical mode" {
            // GIVEN a touch-down on the bull in vertical mode
            // WHEN the finger is dragged sideways only
            val result = swipe(CENTER, RIGHT, SwipeMultiplierMode.VERTICAL, isBull = true)

            // THEN it is not a swipe, exactly as before the setting existed
            result shouldBe null
        }
    }
})
