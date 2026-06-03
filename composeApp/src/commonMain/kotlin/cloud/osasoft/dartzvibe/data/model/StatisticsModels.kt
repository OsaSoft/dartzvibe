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
 * Mode-specific statistics. Each game mode tracks vastly different things, so the
 * mode-specific metrics live in their own sealed subtype rather than as optional fields
 * on a flat struct. This mirrors the [GameMode] / ModeEngine / ThrowResult sealed pattern
 * and makes it impossible to read a Cricket metric off a Classic result.
 *
 * [primaryMetric] is the single value a mode's leaderboard ranks on. It is only ever
 * compared *within* a single mode — there is no honest cross-mode ranking.
 */
sealed interface ModeStatistics {
    /** The value this mode's leaderboard sorts on. */
    val primaryMetric: FixedDecimal

    /** Short label describing [primaryMetric] (e.g. "MPR", "3-dart avg"). */
    val primaryMetricLabel: String

    /** [primaryMetric] formatted for display (with a % suffix where it is a percentage). */
    val primaryMetricDisplay: String

    /** Classic countdown (501/301). */
    data class Classic(
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
    ) : ModeStatistics {
        override val primaryMetric: FixedDecimal get() = threeDartAverage
        override val primaryMetricLabel: String get() = "3-dart avg"
        override val primaryMetricDisplay: String get() = threeDartAverage.format(1)
    }

    /** Parcheesi count-up with knockouts. */
    data class Parcheesi(
        val threeDartAverage: FixedDecimal,
        val knockoutsDealt: Int,
        val timesKnockedOut: Int,
        val bounceBackRate: FixedDecimal,
        val avgTurnsToWin: FixedDecimal,
        val winRate: FixedDecimal,
    ) : ModeStatistics {
        val knockoutRatio: FixedDecimal
            get() = when {
                timesKnockedOut > 0 -> FixedDecimal.divide(knockoutsDealt, timesKnockedOut)
                knockoutsDealt > 0 -> FixedDecimal.fromInt(knockoutsDealt)
                else -> FixedDecimal.ZERO
            }

        /** True when no knockout has been dealt or taken — UI should show "N/A". */
        val hasKnockoutData: Boolean get() = knockoutsDealt > 0 || timesKnockedOut > 0

        override val primaryMetric: FixedDecimal get() = winRate
        override val primaryMetricLabel: String get() = "Win rate"
        override val primaryMetricDisplay: String get() = "${winRate.format(1)}%"
    }

    /** Cricket segment-marking. */
    data class Cricket(
        val marksPerRound: FixedDecimal,
        val closeRate: FixedDecimal,
        val avgPointsPerGame: FixedDecimal,
        val hitRate: FixedDecimal,
    ) : ModeStatistics {
        override val primaryMetric: FixedDecimal get() = marksPerRound
        override val primaryMetricLabel: String get() = "MPR"
        override val primaryMetricDisplay: String get() = marksPerRound.format(1)
    }

    /** Checkout practice — solo training. */
    data class CheckoutPractice(
        val successRate: FixedDecimal,
        val successCount: Int,
        val totalRounds: Int,
        val avgDartsToCheckout: FixedDecimal,
        val bestTarget: Int?,
        val sessionsCompleted: Int,
        val bands: List<CheckoutBandStat>,
    ) : ModeStatistics {
        override val primaryMetric: FixedDecimal get() = successRate
        override val primaryMetricLabel: String get() = "Success rate"
        override val primaryMetricDisplay: String get() = "${successRate.format(1)}%"
    }

    /** Roulette random-target. */
    data class Roulette(
        val pointsPerRound: FixedDecimal,
        val bestRoundScore: Int,
        val hitRate: FixedDecimal,
        val winRate: FixedDecimal,
    ) : ModeStatistics {
        override val primaryMetric: FixedDecimal get() = pointsPerRound
        override val primaryMetricLabel: String get() = "Pts/round"
        override val primaryMetricDisplay: String get() = pointsPerRound.format(1)
    }
}

/**
 * Checkout-practice success broken down by difficulty band (one [GameType] per band).
 * Bands are self-selected, so the success rate is only meaningful next to its band.
 */
data class CheckoutBandStat(
    val band: GameType,
    val successCount: Int,
    val attempts: Int,
) {
    val successRate: FixedDecimal
        get() = FixedDecimal.percentage(successCount, attempts)
}

/**
 * Statistics for a player within a single game mode.
 *
 * Universal fields ([gamesPlayed], [winRate], …) are comparable across modes; everything
 * mode-specific lives in [modeStats]. [modeStats] is null only when no mode is selected
 * (no mode-scoped view) or the player has no games in the selected mode.
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
    val modeStats: ModeStatistics?,
) {
    /** Win rate as a percentage (0–100). */
    val winRate: FixedDecimal
        get() = FixedDecimal.percentage(gamesWon, gamesPlayed)

    /** Leg win rate as a percentage (0–100). */
    val legWinRate: FixedDecimal
        get() = FixedDecimal.percentage(legsWon, legsPlayed)

    companion object {
        fun empty(playerId: Uuid): PlayerStatistics = PlayerStatistics(
            playerId = playerId,
            gamesPlayed = 0,
            gamesWon = 0,
            gamesWonList = emptyList(),
            legsPlayed = 0,
            legsWon = 0,
            totalTurns = 0,
            modeStats = null,
        )
    }
}

/**
 * Filter criteria for statistics calculation.
 *
 * [gameMode] scopes stats to a single mode and selects which [ModeStatistics] subtype is
 * produced. When null, universal stats are computed across all modes and [PlayerStatistics.modeStats]
 * is null. [gameType] narrows further within a mode (e.g. only 501).
 */
data class StatisticsFilter(
    val gameMode: GameMode? = null,
    val gameType: GameType? = null,
)

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
    val player1Stats: PlayerStatistics,
    val player2Stats: PlayerStatistics,
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
            player1Stats = PlayerStatistics.empty(player1Id),
            player2Stats = PlayerStatistics.empty(player2Id),
            recentGames = emptyList(),
        )
    }
}
