package cloud.osasoft.dartzvibe.ui.screen.game.components

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ScoreboardViewTest : FreeSpec({

    val alice = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val bob = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val carol = Uuid.parse("00000000-0000-0000-0000-000000000003")
    val dave = Uuid.parse("00000000-0000-0000-0000-000000000004")

    "scoreboardStandingsOrder" - {
        "Should list the other players in throwing order, starting after the thrower" {
            // GIVEN four players with Carol throwing
            // WHEN the standings are ordered
            val order = scoreboardStandingsOrder(listOf(alice, bob, carol, dave), currentPlayerId = carol)

            // THEN Dave comes next, then Alice and Bob
            order shouldBe listOf(dave, alice, bob)
        }

        "Should list nobody in a solo game" {
            // GIVEN one player, throwing
            // WHEN the standings are ordered
            // THEN the list is empty
            scoreboardStandingsOrder(listOf(alice), currentPlayerId = alice) shouldBe emptyList()
        }

        "Should list everyone when there is no current player" {
            // GIVEN no current player (still loading)
            // WHEN the standings are ordered
            // THEN everyone is listed in throwing order
            scoreboardStandingsOrder(listOf(alice, bob), currentPlayerId = null) shouldBe listOf(alice, bob)
        }
    }

    "scoreboardScale" - {
        "Should be 1 on the 360 x 720 dp design screen" {
            // GIVEN the design size
            // WHEN the scale is computed
            // THEN nothing scales
            scoreboardScale(widthDp = 360f, heightDp = 720f) shouldBe (1f plusOrMinus 0.001f)
        }

        "Should follow the tighter side on a short, wide screen" {
            // GIVEN a 412 x 640 dp screen
            // WHEN the scale is computed
            // THEN the height sets it, so the standings still fit
            scoreboardScale(widthDp = 412f, heightDp = 640f) shouldBe (640f / 720f plusOrMinus 0.001f)
        }

        "Should shrink to the height in landscape" {
            // GIVEN a phone turned sideways, 640 x 330 dp
            // WHEN the scale is computed
            // THEN the height sets it, so the whole layout still fits
            scoreboardScale(widthDp = 640f, heightDp = 330f) shouldBe (330f / 720f plusOrMinus 0.001f)
        }

        "Should stop at its limits on tiny and huge screens" {
            // GIVEN a tiny and a tablet-sized screen
            // WHEN the scale is computed
            // THEN it's clamped to 0.4 and 1.4
            scoreboardScale(widthDp = 100f, heightDp = 200f) shouldBe (0.4f plusOrMinus 0.001f)
            scoreboardScale(widthDp = 800f, heightDp = 1280f) shouldBe (1.4f plusOrMinus 0.001f)
        }
    }
})
