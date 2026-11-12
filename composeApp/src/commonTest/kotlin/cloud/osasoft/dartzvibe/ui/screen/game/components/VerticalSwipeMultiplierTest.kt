package cloud.osasoft.dartzvibe.ui.screen.game.components

import cloud.osasoft.dartzvibe.data.model.Multiplier
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

private const val THRESHOLD_PX = 78.75f // 30 dp at density 2.625 (Pixel 9)

class VerticalSwipeMultiplierTest : FreeSpec({

    data class Case(
        val name: String,
        val deltaY: Float,
        val isBull: Boolean,
        val expected: Multiplier,
    ) {
        override fun toString(): String = "$name (${if (isBull) "bull" else "1-20"}) -> $expected"
    }

    "Should classify a grid key gesture by its final vertical movement" - {
        withData(
            // 1–20: a tap or a wobble within the threshold is a single
            Case(name = "tap without movement", deltaY = 0f, isBull = false, expected = Multiplier.SINGLE),
            Case(name = "small wobble up", deltaY = -20f, isBull = false, expected = Multiplier.SINGLE),
            Case(name = "small wobble down", deltaY = 20f, isBull = false, expected = Multiplier.SINGLE),
            Case(
                name = "exactly the threshold up",
                deltaY = -THRESHOLD_PX,
                isBull = false,
                expected = Multiplier.SINGLE,
            ),
            Case(
                name = "exactly the threshold down",
                deltaY = THRESHOLD_PX,
                isBull = false,
                expected = Multiplier.SINGLE,
            ),
            // Beyond the threshold: up = double, down = triple
            Case(
                name = "just past the threshold up",
                deltaY = -THRESHOLD_PX - 1f,
                isBull = false,
                expected = Multiplier.DOUBLE,
            ),
            Case(
                name = "just past the threshold down",
                deltaY = THRESHOLD_PX + 1f,
                isBull = false,
                expected = Multiplier.TRIPLE,
            ),
            Case(name = "long swipe up", deltaY = -300f, isBull = false, expected = Multiplier.DOUBLE),
            Case(name = "long swipe down", deltaY = 300f, isBull = false, expected = Multiplier.TRIPLE),
            // Bull: single on a tap or a wobble, double on any swipe, never triple
            Case(name = "tap without movement", deltaY = 0f, isBull = true, expected = Multiplier.SINGLE),
            Case(name = "small wobble up", deltaY = -20f, isBull = true, expected = Multiplier.SINGLE),
            Case(name = "small wobble down", deltaY = 20f, isBull = true, expected = Multiplier.SINGLE),
            Case(
                name = "exactly the threshold up",
                deltaY = -THRESHOLD_PX,
                isBull = true,
                expected = Multiplier.SINGLE,
            ),
            Case(
                name = "exactly the threshold down",
                deltaY = THRESHOLD_PX,
                isBull = true,
                expected = Multiplier.SINGLE,
            ),
            Case(
                name = "just past the threshold up",
                deltaY = -THRESHOLD_PX - 1f,
                isBull = true,
                expected = Multiplier.DOUBLE,
            ),
            Case(
                name = "just past the threshold down",
                deltaY = THRESHOLD_PX + 1f,
                isBull = true,
                expected = Multiplier.DOUBLE,
            ),
            Case(name = "long swipe down", deltaY = 300f, isBull = true, expected = Multiplier.DOUBLE),
        ) { case ->
            // GIVEN a touch-down on a key and a lift case.deltaY px lower (negative = higher)
            // WHEN the key classifies the gesture with its pixel threshold (also what its live label shows)
            val multiplier = classifyKeySwipe(deltaY = case.deltaY, thresholdPx = THRESHOLD_PX, isBull = case.isBull)

            // THEN the key enters the expected multiplier
            multiplier shouldBe case.expected
        }
    }

    "Should keep the swipe pill owned by the key that set it" - {
        data class PillCase(
            val name: String,
            val current: SwipePill?,
            val key: Int,
            val multiplier: Multiplier?,
            val expected: SwipePill?,
        ) {
            override fun toString(): String = name
        }

        val double20 = SwipePill(key = 20, multiplier = Multiplier.DOUBLE)
        withData(
            PillCase(
                name = "Should show the pill when a key previews a multiplier",
                current = null,
                key = 20,
                multiplier = Multiplier.DOUBLE,
                expected = double20,
            ),
            PillCase(
                name = "Should update the pill when its key changes the preview",
                current = double20,
                key = 20,
                multiplier = Multiplier.TRIPLE,
                expected = SwipePill(key = 20, multiplier = Multiplier.TRIPLE),
            ),
            PillCase(
                name = "Should hide the pill when its own key clears it",
                current = double20,
                key = 20,
                multiplier = null,
                expected = null,
            ),
            PillCase(
                name = "Should keep the pill when another key (a second finger) clears",
                current = double20,
                key = 5,
                multiplier = null,
                expected = double20,
            ),
            PillCase(
                name = "Should hand the pill to another key that previews a multiplier",
                current = double20,
                key = 25,
                multiplier = Multiplier.DOUBLE,
                expected = SwipePill(key = 25, multiplier = Multiplier.DOUBLE),
            ),
        ) { case ->
            // GIVEN the current pill
            // WHEN a key reports its preview
            val pill = updateSwipePill(current = case.current, key = case.key, multiplier = case.multiplier)

            // THEN the pill belongs to the right key
            pill shouldBe case.expected
        }
    }
})
