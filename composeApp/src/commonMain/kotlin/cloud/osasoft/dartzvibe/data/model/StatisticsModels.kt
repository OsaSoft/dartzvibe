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
) {
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
        )
    }
}

/**
 * Filter criteria for statistics calculation.
 */
data class StatisticsFilter(
    val gameType: GameType? = null,
)
