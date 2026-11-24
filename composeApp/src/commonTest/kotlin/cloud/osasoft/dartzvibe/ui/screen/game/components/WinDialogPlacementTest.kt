package cloud.osasoft.dartzvibe.ui.screen.game.components

import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class WinDialogPlacementTest : FreeSpec({

    "winDialogMaxHeight" - {

        data class MaxCase(
            val name: String,
            val spaceBelowPx: Int,
            val minSpaceBelowPx: Int,
            val viewportPx: Int,
            val expected: Int,
        ) {
            override fun toString(): String = name
        }

        withData(
            MaxCase("Should take the space below the header when it's enough", 900, 500, 2000, 900),
            MaxCase("Should take the space below when it's exactly the minimum", 500, 500, 2000, 500),
            MaxCase("Should take the whole viewport when the space below is shorter", 300, 500, 2000, 2000),
            MaxCase("Should take the whole viewport when the header fills the screen", -100, 500, 2000, 2000),
            MaxCase("Should never be taller than the viewport", 3000, 500, 2000, 2000),
            MaxCase("Should be 0 in an empty viewport", 300, 500, -20, 0),
        ) { case ->
            // GIVEN the space below the header, the minimum and the viewport
            // WHEN limiting the card's height
            val maxHeight = winDialogMaxHeight(case.spaceBelowPx, case.minSpaceBelowPx, case.viewportPx)

            // THEN it's the space below, or the whole viewport when that's too short
            maxHeight shouldBe case.expected
        }
    }

    "winDialogTop" - {

        data class TopCase(
            val name: String,
            val anchorPx: Int,
            val cardPx: Int,
            val expected: Int,
        ) {
            override fun toString(): String = name
        }

        // The card may sit between y = 100 and y = 2000
        withData(
            TopCase("Should sit right below the header when it fits there", 1200, 500, 1200),
            TopCase("Should sit right below the header when it fits exactly", 1500, 500, 1500),
            TopCase("Should rise over the header just enough to fit", 1700, 500, 1500),
            TopCase("Should stop at the top when it's taller than the space", 1700, 2500, 100),
        ) { case ->
            // GIVEN the header's bottom and the card's height
            // WHEN placing the card
            val top = winDialogTop(anchorPx = case.anchorPx, cardPx = case.cardPx, topPx = 100, bottomPx = 2000)

            // THEN it's below the header unless its bottom would leave the screen
            top shouldBe case.expected
        }
    }
})
