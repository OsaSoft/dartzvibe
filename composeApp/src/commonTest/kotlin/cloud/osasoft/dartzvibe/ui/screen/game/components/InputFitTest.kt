package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

class InputFitTest : FreeSpec({

    "fitBoardSide" - {

        data class SideCase(
            val name: String,
            val widthPx: Int,
            val heightLeftPx: Int?,
            val minSidePx: Int,
            val expected: Int,
        ) {
            override fun toString(): String = name
        }

        withData(
            SideCase("Should keep the full width without a height limit", 1000, null, 578, 1000),
            SideCase("Should keep the full width when it fits the height", 1000, 1200, 578, 1000),
            SideCase("Should keep the full width when it fits exactly", 1000, 1000, 578, 1000),
            SideCase("Should shrink to the height left when the width doesn't fit", 1000, 700, 578, 700),
            SideCase("Should stop at the minimum and let the screen scroll", 1000, 400, 578, 578),
            SideCase("Should stop at the minimum when nothing is left", 1000, -50, 578, 578),
            SideCase("Should never be wider than the width, even below the minimum", 500, 300, 578, 500),
        ) { case ->
            // GIVEN the width, the height left for the board and the minimum side
            // WHEN fitting the board
            val side = fitBoardSide(case.widthPx, case.heightLeftPx, case.minSidePx)

            // THEN it is the expected side
            side shouldBe case.expected
        }
    }

    "fitKeyHeight" - {

        data class KeyCase(
            val name: String,
            val heightLeftPx: Int?,
            val expected: Int,
        ) {
            override fun toString(): String = name
        }

        // 4 rows, 105 px default (40 dp at 2.625), 74 px minimum (28 dp)
        withData(
            KeyCase("Should keep the default height without a height limit", null, 105),
            KeyCase("Should keep the default height when the keys fit", 500, 105),
            KeyCase("Should share the height left evenly, rounded down", 403, 100),
            KeyCase("Should stop at the minimum and let the screen scroll", 200, 74),
        ) { case ->
            // GIVEN the height left for 4 key rows
            // WHEN fitting the keys
            val height = fitKeyHeight(heightLeftPx = case.heightLeftPx, rows = 4, nominalPx = 105, minPx = 74)

            // THEN each row gets the expected height, never more than what's left above the minimum
            height shouldBe case.expected
        }
    }

    "missButtonSize" - {

        "Should keep the full size on a full-width board" {
            // GIVEN a Pixel 9 board canvas: a 411 dp screen gives a 395 dp box, minus 4 dp padding on each side
            // WHEN sizing the Miss button
            // THEN it is the full 65 dp
            missButtonSize(canvasSide = 387f, maxSize = 65f) shouldBe 65f
        }

        withData(
            nameFn = { "Should stay clear of the wheel on a $it dp canvas" },
            212f,
            260f,
            320f,
            387f,
        ) { canvasSide ->
            // GIVEN a board canvas; the wheel's radius is 0.85 of half the canvas, the button in the top-left corner
            val half = canvasSide / 2
            val wheelRadius = half * 0.85f

            // WHEN sizing the Miss button
            val size = missButtonSize(canvasSide = canvasSide, maxSize = 65f)

            // THEN the button's edge nearest the centre is outside the wheel
            val buttonCentreDistance = (half - size / 2) * sqrt(2f)
            (buttonCentreDistance - size / 2 >= wheelRadius) shouldBe true
        }

        "Should still be a 48 dp touch target at the minimum board side" {
            // GIVEN the minimum board box (220 dp) minus 4 dp padding on each side
            // WHEN sizing the Miss button
            val size = missButtonSize(canvasSide = MinBoardSide.value - 8f, maxSize = 65f)

            // THEN it is at least 48 dp
            size.toInt() shouldBeGreaterThanOrEqual 48
            size shouldBe (48.76f plusOrMinus 0.01f)
        }
    }

    "minKeyHeight" - {

        withData(
            nameFn = { (line, expected) -> "Should need $expected dp for a $line dp label line" },
            20f to 28f, // default font scale: the 28 dp floor
            24f to 28f,
            26f to 30f, // 1.3x: line plus 2 dp padding above and below
            32f to 36f,
        ) { (line, expected) ->
            // GIVEN the one-line label's line height at the user's font scale
            // WHEN computing the smallest key
            // THEN it fits the line and the key's padding, never below 28 dp
            minKeyHeight(labelLine = line.dp, contentPadding = 2.dp) shouldBe expected.dp
        }
    }

    "Pixel accounting" - {

        data class Screen(
            val density: Float,
            val heightLeftDp: Int,
            val rowHeightsDp: List<Float>,
        ) {
            override fun toString(): String = "${heightLeftDp}dp left at ${density}x with rows $rowHeightsDp"
        }

        // Densities from mdpi to xxxhdpi, including the 420 and 450 dpi ones whose rounding differs; heights from a
        // short phone's leftover to a tall one's; rows as with and without S/D/T buttons or a swipe hint
        val screens = listOf(1f, 2f, 2.625f, 2.8125f, 3f, 3.5f, 4f).flatMap { density ->
            listOf(240, 289, 301, 333, 378, 431, 500).flatMap { heightLeft ->
                listOf(listOf(40f), listOf(16f), listOf(40f, 44f, 40f), listOf(44f, 40f, 20f)).map { rows ->
                    Screen(density = density, heightLeftDp = heightLeft, rowHeightsDp = rows)
                }
            }
        }

        fun Float.px(density: Float): Int = (this * density).roundToInt()

        withData(nameFn = { "Should fit the board into $it" }, screens) { screen ->
            // GIVEN the height left for the radial input, its padding, gaps and fixed rows in device pixels
            val d = screen.density
            val heightLeftPx = screen.heightLeftDp.toFloat().px(d)
            val paddingPx = 8f.px(d)
            val gapPx = 4f.px(d)
            val rowsPx = screen.rowHeightsDp.map { it.px(d) }
            val chromePx = columnChromePx(paddingPx = paddingPx, gapPx = gapPx, rowHeightsPx = rowsPx, flexibleRows = 1)

            // WHEN fitting a board on a 411 dp wide screen
            val minSidePx = 220f.px(d)
            val side = fitBoardSide(widthPx = 395f.px(d), heightLeftPx = heightLeftPx - chromePx, minSidePx = minSidePx)

            // THEN padding, rows, gaps and board never take more than the height left, unless the board is at its
            // floor (where the screen scrolls by design)
            if (side > minSidePx) {
                (paddingPx * 2 + rowsPx.sum() + gapPx * rowsPx.size + side) shouldBeLessThanOrEqual heightLeftPx
            }
        }

        withData(nameFn = { "Should fit the keys into $it" }, screens) { screen ->
            // GIVEN the height left for the grid keypad, its padding, gaps and fixed rows in device pixels
            val d = screen.density
            val heightLeftPx = screen.heightLeftDp.toFloat().px(d)
            val paddingPx = 8f.px(d)
            val gapPx = 4f.px(d)
            val rowsPx = screen.rowHeightsDp.map { it.px(d) }
            val chromePx = columnChromePx(paddingPx = paddingPx, gapPx = gapPx, rowHeightsPx = rowsPx, flexibleRows = 4)

            // WHEN fitting the four key rows
            val minPx = 28f.px(d)
            val key =
                fitKeyHeight(heightLeftPx = heightLeftPx - chromePx, rows = 4, nominalPx = 40f.px(d), minPx = minPx)

            // THEN everything stacked, with a gap between every two rows, fits the height left unless the keys are
            // at their floor
            val stacked = paddingPx * 2 + rowsPx.sum() + key * 4 + gapPx * (rowsPx.size + 4 - 1)
            if (key > minPx) {
                stacked shouldBeLessThanOrEqual heightLeftPx
            }
        }
    }

    "boardLabelSizePx" - {

        withData(
            nameFn = { (radius, expected) -> "Should draw wedge numbers at $expected px on a $radius px wheel" },
            432f to 42f, // Pixel 9 full size (164.5 dp radius at 2.625x): 16 sp unchanged
            236f to 42f, // 90 dp radius, the 220 dp floor: still 16 sp at 1.0x
            200f to 36f, // smaller: capped at 0.18 of the radius
        ) { (radius, expected) ->
            // GIVEN 16 sp at 2.625x (42 px) and a wheel radius
            // WHEN sizing the wedge numbers
            val size = boardLabelSizePx(desiredPx = 42f, radiusPx = radius, maxToRadius = WEDGE_LABEL_MAX_TO_RADIUS)

            // THEN a full-size board keeps 16 sp and a small one gets numbers that fit their wedge
            size shouldBe (expected plusOrMinus 0.5f)
        }

        "Should shrink large-font wedge numbers at the board floor so they don't touch" {
            // GIVEN 16 sp at 1.3x font scale and 2.625x density (54.6 px) on the floor board (90 dp = 236 px radius)
            val radius = 236f

            // WHEN sizing the wedge numbers
            val size = boardLabelSizePx(desiredPx = 54.6f, radiusPx = radius, maxToRadius = WEDGE_LABEL_MAX_TO_RADIUS)

            // THEN two digits (about 1.15 em) fit the wedge's arc where the numbers are drawn (0.7875 of the radius)
            val arc = 2 * PI.toFloat() * radius * 0.7875f / 20
            (size * 1.15f < arc) shouldBe true
        }
    }
})
