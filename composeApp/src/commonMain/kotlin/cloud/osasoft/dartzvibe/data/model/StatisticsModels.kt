package cloud.osasoft.dartzvibe.data.model

import kotlin.math.roundToLong
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Fixed-point decimal type for statistics calculations.
 * Stores values internally as Long with 3 decimal places of precision.
 * This avoids floating-point precision issues while being simple and efficient.
 */
@JvmInline
value class FixedDecimal private constructor(private val scaledValue: Long) : Comparable<FixedDecimal> {
    /**
     * Convert to Double for display or external use.
     */
    fun toDouble(): Double = scaledValue / SCALE.toDouble()

    /**
     * Format to a specific number of decimal places.
     */
    fun format(decimalPlaces: Int = 1): String {
        val doubleValue = toDouble()
        val factor = when (decimalPlaces) {
            0 -> 1.0
            1 -> 10.0
            2 -> 100.0
            3 -> 1000.0
            else -> 10.0 // Default to 1 decimal place
        }
        val rounded = (doubleValue * factor).roundToLong() / factor
        return when (decimalPlaces) {
            0 -> rounded.toLong().toString()

            1 -> {
                val intPart = rounded.toLong()
                val decPart = ((rounded - intPart) * 10).roundToLong()
                "$intPart.${kotlin.math.abs(decPart)}"
            }

            else -> rounded.toString()
        }
    }

    operator fun plus(other: FixedDecimal): FixedDecimal =
        FixedDecimal(scaledValue + other.scaledValue)

    operator fun minus(other: FixedDecimal): FixedDecimal =
        FixedDecimal(scaledValue - other.scaledValue)

    operator fun times(other: Int): FixedDecimal =
        FixedDecimal(scaledValue * other)

    override operator fun compareTo(other: FixedDecimal): Int =
        scaledValue.compareTo(other.scaledValue)

    override fun toString(): String = format(3)

    companion object {
        /** Scale factor: 1000 = 3 decimal places */
        private const val SCALE = 1000L

        /** Zero value */
        val ZERO = FixedDecimal(0L)

        /**
         * Create from an integer.
         */
        fun fromInt(value: Int): FixedDecimal = FixedDecimal(value.toLong() * SCALE)

        /**
         * Create from a Long.
         */
        fun fromLong(value: Long): FixedDecimal = FixedDecimal(value * SCALE)

        /**
         * Divide two integers with fixed-point precision.
         */
        fun divide(numerator: Int, denominator: Int): FixedDecimal {
            if (denominator == 0) return ZERO
            // Multiply numerator by scale before dividing to preserve precision
            return FixedDecimal((numerator.toLong() * SCALE) / denominator)
        }

        /**
         * Create a percentage: (numerator / denominator) * 100.
         */
        fun percentage(numerator: Int, denominator: Int): FixedDecimal {
            if (denominator == 0) return ZERO
            // Calculate as (numerator * 100 * SCALE) / denominator
            return FixedDecimal((numerator.toLong() * 100 * SCALE) / denominator)
        }
    }
}

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
