package cloud.osasoft.dartzvibe.data.model

/**
 * How a swipe on the radial dartboard picks the multiplier (only when the S/D/T buttons are hidden).
 * The grid keypad always uses up/down, whatever this is set to.
 *
 * Persisted by [value], never by name, so constants can be renamed safely.
 */
enum class SwipeMultiplierMode(val value: String) {
    /** Swipe up = double, swipe down = triple, whatever wedge is touched. */
    VERTICAL("vertical"),

    /** Like a real board: swipe toward the board center = triple (inner ring), outward = double (outer ring). */
    RADIAL("radial"),
    ;

    companion object {
        fun fromValue(value: String): SwipeMultiplierMode = entries.find { it.value == value } ?: VERTICAL
    }
}
