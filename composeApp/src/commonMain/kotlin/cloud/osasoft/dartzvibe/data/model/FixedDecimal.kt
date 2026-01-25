package cloud.osasoft.dartzvibe.data.model

import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.roundToLong

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
                "$intPart.${abs(decPart)}"
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
