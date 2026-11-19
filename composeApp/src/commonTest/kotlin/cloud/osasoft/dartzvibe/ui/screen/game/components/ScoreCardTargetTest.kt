package cloud.osasoft.dartzvibe.ui.screen.game.components

import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class ScoreCardTargetTest : FreeSpec({

    data class Case(
        val gameMode: GameMode,
        val gameType: GameType,
        val rouletteTargetScore: Int?,
        val expected: String?,
    ) {
        override fun toString(): String = "$gameMode $gameType target=$rouletteTargetScore -> ${expected ?: "none"}"
    }

    "scoreCardTargetSuffix" - {

        "Should show the configured target for Roulette Score" - {
            withData(
                Case(GameMode.ROULETTE, GameType.ROULETTE_SCORE, 100, " / 100"),
                Case(GameMode.ROULETTE, GameType.ROULETTE_SCORE, 250, " / 250"),
            ) { case ->
                // GIVEN a Roulette Score game with a target
                // WHEN building the score card suffix
                val suffix = scoreCardTargetSuffix(case.gameMode, case.gameType, case.rouletteTargetScore)

                // THEN it shows the configured target
                suffix shouldBe case.expected
            }
        }

        "Should show no suffix for Roulette Rounds or a missing Roulette target" - {
            withData(
                Case(GameMode.ROULETTE, GameType.ROULETTE_ROUNDS, null, null),
                Case(GameMode.ROULETTE, GameType.ROULETTE_ROUNDS, 100, null),
                Case(GameMode.ROULETTE, GameType.ROULETTE_SCORE, null, null),
                Case(GameMode.ROULETTE, GameType.ROULETTE_SCORE, 0, null),
            ) { case ->
                // GIVEN a Roulette game without a score to reach (Rounds, or an old save with no target)
                // WHEN building the score card suffix
                val suffix = scoreCardTargetSuffix(case.gameMode, case.gameType, case.rouletteTargetScore)

                // THEN the card shows just the score, never "/ 0"
                suffix shouldBe case.expected
            }
        }

        "Should show the starting score as the Parcheesi target" - {
            withData(
                Case(GameMode.PARCHEESI, GameType.CLASSIC_301, null, " / 301"),
                Case(GameMode.PARCHEESI, GameType.CLASSIC_501, null, " / 501"),
            ) { case ->
                // GIVEN a Parcheesi game
                // WHEN building the score card suffix
                val suffix = scoreCardTargetSuffix(case.gameMode, case.gameType, case.rouletteTargetScore)

                // THEN it shows the 301/501 to count up to
                suffix shouldBe case.expected
            }
        }

        "Should show no suffix for Classic, Cricket and Checkout Practice" - {
            withData(
                Case(GameMode.CLASSIC, GameType.CLASSIC_501, null, null),
                Case(GameMode.CLASSIC, GameType.CLASSIC_301, null, null),
                Case(GameMode.CRICKET, GameType.CRICKET_REGULAR, null, null),
                Case(GameMode.CRICKET, GameType.CRICKET_RANDOM, null, null),
                Case(GameMode.CHECKOUT_PRACTICE, GameType.CHECKOUT_EASY, null, null),
                Case(GameMode.CHECKOUT_PRACTICE, GameType.CHECKOUT_FULL, null, null),
            ) { case ->
                // GIVEN a mode without a score to count up to
                // WHEN building the score card suffix
                val suffix = scoreCardTargetSuffix(case.gameMode, case.gameType, case.rouletteTargetScore)

                // THEN the card shows just the score
                suffix shouldBe case.expected
            }
        }
    }
})
