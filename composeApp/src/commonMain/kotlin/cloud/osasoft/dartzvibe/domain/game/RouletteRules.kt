package cloud.osasoft.dartzvibe.domain.game

/**
 * Roulette target selection, shared by [RouletteModeEngine], the active game screen, statistics and
 * the Game Details history so they always agree on which segment a round was aimed at.
 */
internal object RouletteRules {

    private const val FALLBACK_TARGET = 1

    /**
     * Target segment for the 0-based [roundIndex]. Rounds past the end of [targets] wrap around;
     * a missing or empty list (old or malformed configs) falls back to segment 1.
     */
    fun targetFor(targets: List<Int>?, roundIndex: Int): Int = if (targets.isNullOrEmpty()) {
        FALLBACK_TARGET
    } else {
        targets[roundIndex % targets.size]
    }
}
