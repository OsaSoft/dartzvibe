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

    fun statsWith(
        gamesPlayed: Int,
        gamesWon: Int,
        legsPlayed: Int = 0,
        legsWon: Int = 0,
    ): PlayerStatistics = PlayerStatistics(
        playerId = playerId,
        gamesPlayed = gamesPlayed,
        gamesWon = gamesWon,
        gamesWonList = emptyList(),
        legsPlayed = legsPlayed,
        legsWon = legsWon,
        totalTurns = 0,
        modeStats = null,
    )

    "PlayerStatistics" - {
        "Should calculate win rate as a percentage" {
            // GIVEN stats with 4 games, 3 wins
            val stats = statsWith(gamesPlayed = 4, gamesWon = 3)

            // THEN win rate is 3/4 = 75%
            stats.winRate.format(1) shouldBe "75.0"
        }

        "Should calculate leg win rate as a percentage" {
            // GIVEN stats with 5 legs, 4 wins
            val stats = statsWith(gamesPlayed = 2, gamesWon = 1, legsPlayed = 5, legsWon = 4)

            // THEN leg win rate is 4/5 = 80%
            stats.legWinRate.format(1) shouldBe "80.0"
        }

        "Should return 0 win rate when no games played" {
            // GIVEN empty stats
            val stats = PlayerStatistics.empty(playerId)

            // THEN win rate is 0
            stats.winRate shouldBe FixedDecimal.ZERO
            stats.legWinRate shouldBe FixedDecimal.ZERO
        }

        "Should create empty stats with null modeStats" {
            // GIVEN empty stats factory
            val stats = PlayerStatistics.empty(playerId)

            // THEN all universal values are zero and there are no mode stats
            stats.playerId shouldBe playerId
            stats.gamesPlayed shouldBe 0
            stats.gamesWon shouldBe 0
            stats.gamesWonList.size shouldBe 0
            stats.legsPlayed shouldBe 0
            stats.legsWon shouldBe 0
            stats.totalTurns shouldBe 0
            stats.modeStats shouldBe null
        }
    }

    "ModeStatistics" - {
        "Classic primary metric is the 3-dart average" {
            val classic = ModeStatistics.Classic(
                threeDartAverage = FixedDecimal.fromInt(80),
                first9Average = FixedDecimal.ZERO,
                checkoutAttempts = 0,
                checkoutsHit = 0,
                checkoutPercentage = FixedDecimal.ZERO,
                bestCheckout = null,
                count180s = 0,
                games180s = emptyList(),
                count140Plus = 0,
                count100Plus = 0,
                highestTurnScore = null,
            )
            classic.primaryMetric shouldBe FixedDecimal.fromInt(80)
            classic.primaryMetricLabel shouldBe "3-dart avg"
            classic.primaryMetricDisplay shouldBe "80.0"
        }

        "Parcheesi knockout ratio is N/A signalled by hasKnockoutData" {
            val none = ModeStatistics.Parcheesi(
                threeDartAverage = FixedDecimal.ZERO,
                knockoutsDealt = 0,
                timesKnockedOut = 0,
                bounceBackRate = FixedDecimal.ZERO,
                avgTurnsToWin = FixedDecimal.ZERO,
                winRate = FixedDecimal.ZERO,
            )
            none.hasKnockoutData shouldBe false

            val some = none.copy(knockoutsDealt = 2)
            some.hasKnockoutData shouldBe true
        }
    }

    "GameReference" - {
        "Should store game reference data correctly" {
            val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")
            val timestamp = 1704067200000L
            val ref = GameReference(sessionId, timestamp, "Bob vs Charlie")
            ref.sessionId shouldBe sessionId
            ref.timestamp shouldBe timestamp
            ref.opponentNames shouldBe "Bob vs Charlie"
        }
    }

    "StatAchievement" - {
        "Should store achievement with game reference" {
            val gameRef = GameReference(
                sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100"),
                timestamp = 1704067200000L,
                opponentNames = "Bob",
            )
            val achievement = StatAchievement(value = 170, game = gameRef)
            achievement.value shouldBe 170
            achievement.game shouldBe gameRef
        }
    }

    "StatisticsFilter" - {
        "Should default to no mode and no type" {
            val filter = StatisticsFilter()
            filter.gameMode shouldBe null
            filter.gameType shouldBe null
        }

        "Should store mode and type filters" {
            val filter = StatisticsFilter(gameMode = GameMode.CRICKET, gameType = GameType.CRICKET_REGULAR)
            filter.gameMode shouldBe GameMode.CRICKET
            filter.gameType shouldBe GameType.CRICKET_REGULAR
        }
    }

    "FixedDecimal" - {
        "Should format with correct decimal places" {
            val value = FixedDecimal.divide(123456, 1000)
            value.format(0) shouldBe "123"
            value.format(1) shouldBe "123.4"
        }

        "Should perform basic arithmetic" {
            val a = FixedDecimal.fromInt(10)
            val b = FixedDecimal.fromInt(5)
            (a + b) shouldBe FixedDecimal.fromInt(15)
            (a - b) shouldBe FixedDecimal.fromInt(5)
            (a * 3) shouldBe FixedDecimal.fromInt(30)
        }

        "Should calculate percentage correctly" {
            FixedDecimal.percentage(1, 2) shouldBe FixedDecimal.fromInt(50)
        }

        "Should handle division by zero" {
            FixedDecimal.divide(10, 0) shouldBe FixedDecimal.ZERO
        }

        "Should compare values correctly" {
            val a = FixedDecimal.fromInt(10)
            val b = FixedDecimal.fromInt(5)
            val c = FixedDecimal.fromInt(10)
            (a > b) shouldBe true
            (b < a) shouldBe true
            (a == c) shouldBe true
        }
    }
})
