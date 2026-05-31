package cloud.osasoft.dartzvibe.data.model

import kotlin.jvm.JvmInline
import kotlin.math.abs

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
     * Format to a specific number of decimal places, truncating toward zero.
     *
     * Uses exact integer arithmetic on the internal scaled value so the result is
     * identical across platforms (no floating-point rounding differences between
     * Kotlin/JVM and Kotlin/Native).
     */
    fun format(decimalPlaces: Int = 1): String {
        val places = decimalPlaces.coerceIn(0, 3)
        val negative = scaledValue < 0L
        val absScaled = abs(scaledValue)

        // Drop the digits below the requested precision, keeping `places` decimals
        // (truncation toward zero). SCALE has 3 decimals, so divide by 10^(3 - places).
        val dropDivisor = when (places) {
            0 -> SCALE
            1 -> 100L
            2 -> 10L
            else -> 1L
        }
        val truncated = absScaled / dropDivisor
        val sign = if (negative) "-" else ""

        if (places == 0) return "$sign$truncated"

        val unit = when (places) {
            1 -> 10L
            2 -> 100L
            else -> 1000L
        }
        val intPart = truncated / unit
        val decPart = (truncated % unit).toString().padStart(places, '0')
        return "$sign$intPart.$decPart"
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
