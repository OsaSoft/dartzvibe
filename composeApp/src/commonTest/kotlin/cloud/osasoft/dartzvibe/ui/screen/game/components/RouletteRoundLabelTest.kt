package cloud.osasoft.dartzvibe.ui.screen.game.components

import cloud.osasoft.dartzvibe.data.model.GameType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class RouletteRoundLabelTest : FreeSpec({

    data class Case(
        val gameType: GameType?,
        val totalRounds: Int?,
        val targetScore: Int?,
        val expected: String,
    ) {
        override fun toString(): String = "$gameType rounds=$totalRounds target=$targetScore -> $expected"
    }

    "rouletteRoundLabel" - {

        "Should show the configured target for Roulette Score" - {
            withData(
                Case(GameType.ROULETTE_SCORE, null, 30, "Round 2 - Target: 30 pts"),
                Case(GameType.ROULETTE_SCORE, null, 100, "Round 2 - Target: 100 pts"),
            ) { case ->
                // GIVEN a Roulette Score game with a target
                // WHEN building the round line
                val label = rouletteRoundLabel(case.gameType, roundNumber = 2, case.totalRounds, case.targetScore)

                // THEN it shows the round and the target
                label shouldBe case.expected
            }
        }

        "Should hide the target when an old save has none" - {
            withData(
                Case(GameType.ROULETTE_SCORE, null, null, "Round 2"),
                Case(GameType.ROULETTE_SCORE, null, 0, "Round 2"),
            ) { case ->
                // GIVEN a Roulette Score game saved without a target
                // WHEN building the round line
                val label = rouletteRoundLabel(case.gameType, roundNumber = 2, case.totalRounds, case.targetScore)

                // THEN it shows just the round, never "Target: 0 pts"
                label shouldBe case.expected
            }
        }

        "Should show the round count for Roulette Rounds" {
            // GIVEN a Roulette Rounds game of 10 rounds
            // WHEN building the round line
            val label =
                rouletteRoundLabel(GameType.ROULETTE_ROUNDS, roundNumber = 3, totalRounds = 10, targetScore = null)

            // THEN it shows the round out of the total
            label shouldBe "Round 3 of 10"
        }

        "Should hide the round count when an old save has none" - {
            withData(
                Case(GameType.ROULETTE_ROUNDS, null, null, "Round 3"),
                Case(GameType.ROULETTE_ROUNDS, 0, null, "Round 3"),
            ) { case ->
                // GIVEN a Roulette Rounds game saved without a round count
                // WHEN building the round line
                val label = rouletteRoundLabel(case.gameType, roundNumber = 3, case.totalRounds, case.targetScore)

                // THEN it shows just the round, never "of 0"
                label shouldBe case.expected
            }
        }
    }
})
