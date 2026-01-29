package cloud.osasoft.dartzvibe.data.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Reference to a specific game for linking stats to their source.
 */
@OptIn(ExperimentalUuidApi::class)
data class GameReference(
    val sessionId: Uuid,
    val timestamp: Long,
    val opponentNames: String,
)

/**
 * A stat achievement with game context.
 */
@OptIn(ExperimentalUuidApi::class)
data class StatAchievement(
    val value: Int,
    val game: GameReference,
)

/**
 * Complete statistics for a player across their games.
 */
@OptIn(ExperimentalUuidApi::class)
data class PlayerStatistics(
    val playerId: Uuid,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val gamesWonList: List<GameReference>,
    val legsPlayed: Int,
    val legsWon: Int,
    val totalTurns: Int,
    val totalScore: Int,
    val threeDartAverage: FixedDecimal,
    val first9Average: FixedDecimal,
    val checkoutAttempts: Int,
    val checkoutsHit: Int,
    val checkoutPercentage: FixedDecimal,
    val bestCheckout: StatAchievement?,
    val count180s: Int,
    val games180s: List<GameReference>,
    val count140Plus: Int,
    val count100Plus: Int,
    val highestTurnScore: StatAchievement?,
    val knockoutsDealt: Int = 0,
    val timesKnockedOut: Int = 0,
    val parcheesiGamesPlayed: Int = 0,
) {
    val knockoutRatio: FixedDecimal
        get() = if (timesKnockedOut > 0) {
            FixedDecimal.divide(knockoutsDealt, timesKnockedOut)
        } else if (knockoutsDealt > 0) {
            FixedDecimal.fromInt(knockoutsDealt)
        } else {
            FixedDecimal.ZERO
        }
    val winRate: FixedDecimal
        get() = if (gamesPlayed > 0) {
            FixedDecimal.divide(gamesWon, gamesPlayed)
        } else {
            FixedDecimal.ZERO
        }

    val legWinRate: FixedDecimal
        get() = if (legsPlayed > 0) {
            FixedDecimal.divide(legsWon, legsPlayed)
        } else {
            FixedDecimal.ZERO
        }

    companion object {
        fun empty(playerId: Uuid): PlayerStatistics = PlayerStatistics(
            playerId = playerId,
            gamesPlayed = 0,
            gamesWon = 0,
            gamesWonList = emptyList(),
            legsPlayed = 0,
            legsWon = 0,
            totalTurns = 0,
            totalScore = 0,
            threeDartAverage = FixedDecimal.ZERO,
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
            knockoutsDealt = 0,
            timesKnockedOut = 0,
            parcheesiGamesPlayed = 0,
        )
    }
}

/**
 * Filter criteria for statistics calculation.
 */
data class StatisticsFilter(
    val gameType: GameType? = null,
)

/**
 * Head-to-head statistics for a specific player in an H2H comparison.
 */
data class H2HPlayerStats(
    val threeDartAverage: FixedDecimal,
    val bestCheckout: Int?,
    val legsWon: Int,
    val legsPlayed: Int,
    val count180s: Int,
    val count140Plus: Int,
    val knockoutsDealt: Int = 0,
    val timesKnockedOut: Int = 0,
) {
    val legWinRate: FixedDecimal
        get() = if (legsPlayed > 0) {
            FixedDecimal.percentage(legsWon, legsPlayed)
        } else {
            FixedDecimal.ZERO
        }
}

/**
 * Summary of a single H2H game.
 */
@OptIn(ExperimentalUuidApi::class)
data class H2HGameSummary(
    val sessionId: Uuid,
    val timestamp: Long,
    val gameType: GameType,
    val winnerId: Uuid?,
    val player1LegsWon: Int,
    val player2LegsWon: Int,
)

/**
 * Complete head-to-head statistics between two players.
 */
@OptIn(ExperimentalUuidApi::class)
data class HeadToHeadStatistics(
    val player1Id: Uuid,
    val player2Id: Uuid,
    val gamesPlayed: Int,
    val player1Wins: Int,
    val player2Wins: Int,
    val player1Stats: H2HPlayerStats,
    val player2Stats: H2HPlayerStats,
    val recentGames: List<H2HGameSummary>,
) {
    val player1WinRate: FixedDecimal
        get() = if (gamesPlayed > 0) {
            FixedDecimal.percentage(player1Wins, gamesPlayed)
        } else {
            FixedDecimal.ZERO
        }

    val player2WinRate: FixedDecimal
        get() = if (gamesPlayed > 0) {
            FixedDecimal.percentage(player2Wins, gamesPlayed)
        } else {
            FixedDecimal.ZERO
        }

    companion object {
        fun empty(player1Id: Uuid, player2Id: Uuid): HeadToHeadStatistics = HeadToHeadStatistics(
            player1Id = player1Id,
            player2Id = player2Id,
            gamesPlayed = 0,
            player1Wins = 0,
            player2Wins = 0,
            player1Stats = H2HPlayerStats(
                threeDartAverage = FixedDecimal.ZERO,
                bestCheckout = null,
                legsWon = 0,
                legsPlayed = 0,
                count180s = 0,
                count140Plus = 0,
                knockoutsDealt = 0,
                timesKnockedOut = 0,
            ),
            player2Stats = H2HPlayerStats(
                threeDartAverage = FixedDecimal.ZERO,
                bestCheckout = null,
                legsWon = 0,
                legsPlayed = 0,
                count180s = 0,
                count140Plus = 0,
                knockoutsDealt = 0,
                timesKnockedOut = 0,
            ),
            recentGames = emptyList(),
        )
    }
}
