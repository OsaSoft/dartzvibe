package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class PlayerNameTextTest : FreeSpec({

    "playerNameScale" - {

        "Should keep the full size when the name fits" {
            // GIVEN a name narrower than its room
            // WHEN computing its scale
            val scale = playerNameScale(textWidth = 80, maxWidth = 100, fontSize = 16.sp)

            // THEN it isn't shrunk
            scale shouldBe 1f
        }

        "Should shrink a slightly too wide name until it fits" {
            // GIVEN a name 25% wider than its room, far from the floor
            // WHEN computing its scale
            val scale = playerNameScale(textWidth = 125, maxWidth = 100, fontSize = 16.sp)

            // THEN it shrinks to fit, like the card's other lines
            scale shouldBe fitTextScale(textWidth = 125, maxWidth = 100)
        }

        "Should stop at the minimum font size, leaving the rest to the ellipsis" {
            // GIVEN a name three times wider than its room at 16 sp
            // WHEN computing its scale
            val scale = playerNameScale(textWidth = 300, maxWidth = 100, fontSize = 16.sp)

            // THEN it stops at 10 sp instead of shrinking to an unreadable size
            scale shouldBe ((MinPlayerNameFontSize.value / 16f) plusOrMinus 0.0001f)
        }

        "Should never shrink a font already at or below the minimum" {
            // GIVEN a too wide name whose font is already at the minimum size or smaller
            // WHEN computing its scale
            val atMin = playerNameScale(textWidth = 300, maxWidth = 100, fontSize = MinPlayerNameFontSize)
            val belowMin = playerNameScale(textWidth = 300, maxWidth = 100, fontSize = 8.sp)

            // THEN it keeps its size and is ellipsized instead
            atMin shouldBe 1f
            belowMin shouldBe 1f
        }

        "Should handle a missing width or font size" {
            // GIVEN no width yet (zero) or an unspecified font size
            // WHEN computing the scale
            val noWidth = playerNameScale(textWidth = 300, maxWidth = 0, fontSize = 16.sp)
            val noFontSize = playerNameScale(textWidth = 300, maxWidth = 100, fontSize = TextUnit.Unspecified)

            // THEN a zero width keeps the full size, and with no font size to floor it just shrinks to fit
            noWidth shouldBe 1f
            noFontSize shouldBe fitTextScale(textWidth = 300, maxWidth = 100)
        }
    }
})
