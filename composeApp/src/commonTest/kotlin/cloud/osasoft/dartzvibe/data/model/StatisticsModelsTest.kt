package cloud.osasoft.dartzvibe.data.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for Statistics data models.
 */
@OptIn(ExperimentalUuidApi::class)
class StatisticsModelsTest : FreeSpec({

    val playerId = Uuid.parse("00000000-0000-0000-0000-000000000001")

    "PlayerStatistics" - {
        "Should calculate win rate correctly" {
            // GIVEN stats with 4 games, 3 wins
            val stats = PlayerStatistics(
                playerId = playerId,
                gamesPlayed = 4,
                gamesWon = 3,
                gamesWonList = emptyList(),
                legsPlayed = 5,
                legsWon = 4,
                totalTurns = 20,
                totalScore = 2000,
                threeDartAverage = FixedDecimal.fromInt(100),
                first9Average = FixedDecimal.fromInt(120),
                checkoutAttempts = 10,
                checkoutsHit = 5,
                checkoutPercentage = FixedDecimal.fromInt(50),
                bestCheckout = null,
                count180s = 2,
                games180s = emptyList(),
                count140Plus = 5,
                count100Plus = 10,
                highestTurnScore = null,
            )

            // THEN win rate is 3/4 = 0.75
            stats.winRate.format(2) shouldBe "0.75"
        }

        "Should calculate leg win rate correctly" {
            // GIVEN stats with 5 legs, 4 wins
            val stats = PlayerStatistics(
                playerId = playerId,
                gamesPlayed = 2,
                gamesWon = 1,
                gamesWonList = emptyList(),
                legsPlayed = 5,
                legsWon = 4,
                totalTurns = 20,
                totalScore = 2000,
                threeDartAverage = FixedDecimal.fromInt(100),
                first9Average = FixedDecimal.fromInt(120),
                checkoutAttempts = 10,
                checkoutsHit = 5,
                checkoutPercentage = FixedDecimal.fromInt(50),
                bestCheckout = null,
                count180s = 2,
                games180s = emptyList(),
                count140Plus = 5,
                count100Plus = 10,
                highestTurnScore = null,
            )

            // THEN leg win rate is 4/5 = 0.8
            stats.legWinRate.format(1) shouldBe "0.8"
        }

        "Should return 0 win rate when no games played" {
            // GIVEN empty stats
            val stats = PlayerStatistics.empty(playerId)

            // THEN win rate is 0
            stats.winRate shouldBe FixedDecimal.ZERO
            stats.legWinRate shouldBe FixedDecimal.ZERO
        }

        "Should create empty stats correctly" {
            // GIVEN empty stats factory
            val stats = PlayerStatistics.empty(playerId)

            // THEN all values are zero/empty
            stats.playerId shouldBe playerId
            stats.gamesPlayed shouldBe 0
            stats.gamesWon shouldBe 0
            stats.gamesWonList.size shouldBe 0
            stats.legsPlayed shouldBe 0
            stats.legsWon shouldBe 0
            stats.totalTurns shouldBe 0
            stats.totalScore shouldBe 0
            stats.threeDartAverage shouldBe FixedDecimal.ZERO
            stats.first9Average shouldBe FixedDecimal.ZERO
            stats.checkoutAttempts shouldBe 0
            stats.checkoutsHit shouldBe 0
            stats.checkoutPercentage shouldBe FixedDecimal.ZERO
            stats.bestCheckout shouldBe null
            stats.count180s shouldBe 0
            stats.games180s.size shouldBe 0
            stats.count140Plus shouldBe 0
            stats.count100Plus shouldBe 0
            stats.highestTurnScore shouldBe null
        }
    }

    "GameReference" - {
        "Should store game reference data correctly" {
            // GIVEN a game reference
            val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")
            val timestamp = 1704067200000L // Jan 1, 2024

            val ref = GameReference(
                sessionId = sessionId,
                timestamp = timestamp,
                opponentNames = "Bob vs Charlie",
            )

            // THEN all fields are stored
            ref.sessionId shouldBe sessionId
            ref.timestamp shouldBe timestamp
            ref.opponentNames shouldBe "Bob vs Charlie"
        }
    }

    "StatAchievement" - {
        "Should store achievement with game reference" {
            // GIVEN an achievement
            val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")
            val gameRef = GameReference(
                sessionId = sessionId,
                timestamp = 1704067200000L,
                opponentNames = "Bob",
            )

            val achievement = StatAchievement(
                value = 170,
                game = gameRef,
            )

            // THEN achievement stores value and game reference
            achievement.value shouldBe 170
            achievement.game shouldBe gameRef
        }
    }

    "StatisticsFilter" - {
        "Should have null game type by default" {
            // GIVEN default filter
            val filter = StatisticsFilter()

            // THEN game type is null (all games)
            filter.gameType shouldBe null
        }

        "Should store game type filter" {
            // GIVEN filter with game type
            val filter = StatisticsFilter(gameType = GameType.CLASSIC_501)

            // THEN game type is stored
            filter.gameType shouldBe GameType.CLASSIC_501
        }
    }

    "FixedDecimal" - {
        "Should format with correct decimal places" {
            // GIVEN a fixed decimal representing 123.456
            val value = FixedDecimal.divide(123456, 1000)

            // THEN formatting works correctly
            value.format(0) shouldBe "123"
            value.format(1) shouldBe "123.4"
        }

        "Should perform basic arithmetic" {
            // GIVEN two values
            val a = FixedDecimal.fromInt(10)
            val b = FixedDecimal.fromInt(5)

            // THEN arithmetic works
            (a + b) shouldBe FixedDecimal.fromInt(15)
            (a - b) shouldBe FixedDecimal.fromInt(5)
            (a * 3) shouldBe FixedDecimal.fromInt(30)
        }

        "Should calculate percentage correctly" {
            // GIVEN numerator 1 and denominator 2
            val percentage = FixedDecimal.percentage(1, 2)

            // THEN percentage is 50
            percentage shouldBe FixedDecimal.fromInt(50)
        }

        "Should handle division by zero" {
            // GIVEN division by zero
            val result = FixedDecimal.divide(10, 0)

            // THEN returns ZERO
            result shouldBe FixedDecimal.ZERO
        }

        "Should compare values correctly" {
            // GIVEN various values
            val a = FixedDecimal.fromInt(10)
            val b = FixedDecimal.fromInt(5)
            val c = FixedDecimal.fromInt(10)

            // THEN comparison works
            (a > b) shouldBe true
            (b < a) shouldBe true
            (a == c) shouldBe true
        }
    }
})
