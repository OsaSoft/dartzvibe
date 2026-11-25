package cloud.osasoft.dartzvibe.ui.screen.game.components

import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class MultiplierIndicatorPlacementTest : FreeSpec({

    "pillTopAboveKeys" - {

        data class TopCase(
            val name: String,
            val pillHeightPx: Int,
            val keysTopPx: Int,
            val gapPx: Int,
            val expected: Int,
        ) {
            override fun toString(): String = name
        }

        withData(
            TopCase("Should end the gap above the first key row", 120, 21, 5, -104),
            TopCase("Should sit right on the keys without a gap", 120, 21, 0, -99),
            TopCase("Should rise further for a taller pill (large font)", 160, 21, 5, -144),
        ) { case ->
            // GIVEN the pill's height, where the key rows start and the gap
            // WHEN placing the pill above the keys
            val top = pillTopAboveKeys(case.pillHeightPx, case.keysTopPx, case.gapPx)

            // THEN its bottom edge is the gap above the first key row, so it covers no key
            top shouldBe case.expected
        }
    }
})
