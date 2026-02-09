package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw

/**
 * Represents a possible checkout path (sequence of throws to reach exactly 0).
 */
data class CheckoutPath(
    val throws: List<Throw>,
) {
    val totalScore: Int get() = throws.sumOf { it.score }
}

/**
 * Calculator for dart checkout combinations.
 * Provides optimal checkout paths for scores up to 170 (maximum 3-dart checkout with bull).
 */
object CheckoutCalculator {

    private const val MAX_CHECKOUT = 170

    /**
     * Returns possible checkout paths for the given score.
     *
     * @param score The current score to checkout from
     * @param doubleOut Whether a double is required to finish (standard rule)
     * @return List of checkout paths sorted by preference, or null if checkout is impossible
     */
    fun getCheckoutOptions(
        score: Int,
        doubleOut: Boolean,
        maxDarts: Int = 3,
    ): List<CheckoutPath>? = when {
        maxDarts <= 0 -> null
        score <= 0 -> null
        score > MAX_CHECKOUT -> null
        score == 1 && doubleOut -> null
        else -> calculateCheckouts(score, doubleOut, maxDarts)
    }

    /**
     * Calculates checkout paths for the given score.
     * Prioritizes: fewest darts first, then highest-scoring setup throws.
     */
    private fun calculateCheckouts(score: Int, doubleOut: Boolean, maxDarts: Int): List<CheckoutPath>? {
        val paths = mutableListOf<CheckoutPath>()

        // Try 1-dart finishes
        findOneDartFinishes(score, doubleOut)?.let { paths.addAll(it) }
        if (paths.size >= 3) return paths.take(3)

        // Try 2-dart finishes
        if (maxDarts >= 2) {
            findTwoDartFinishes(score, doubleOut).let { paths.addAll(it) }
            if (paths.size >= 3) return paths.take(3)
        }

        // Try 3-dart finishes
        if (maxDarts >= 3) {
            findThreeDartFinishes(score, doubleOut).let { paths.addAll(it) }
        }

        return paths.take(3).ifEmpty { null }
    }

    /**
     * Finds 1-dart checkouts for the given score.
     */
    private fun findOneDartFinishes(score: Int, doubleOut: Boolean): List<CheckoutPath>? {
        if (doubleOut) {
            // Must finish on a double
            return when {
                score == 50 -> listOf(CheckoutPath(listOf(bull())))

                score in 2..40 && score % 2 == 0 -> {
                    listOf(CheckoutPath(listOf(double(score / 2))))
                }

                else -> null
            }
        }

        // Without double-out, prefer simpler finishes
        return when {
            score in 1..20 -> listOf(CheckoutPath(listOf(single(score))))
            score == 25 -> listOf(CheckoutPath(listOf(outerBull())))
            score == 50 -> listOf(CheckoutPath(listOf(bull())))
            score in 21..40 && score % 2 == 0 -> listOf(CheckoutPath(listOf(double(score / 2))))
            score in 21..60 && score % 3 == 0 -> listOf(CheckoutPath(listOf(triple(score / 3))))
            else -> null
        }
    }

    /**
     * Finds 2-dart checkouts for the given score.
     * Returns paths sorted by setup score (highest first).
     */
    private fun findTwoDartFinishes(score: Int, doubleOut: Boolean): List<CheckoutPath> {
        val finishingThrows = if (doubleOut) getDoubleFinishingThrows() else getAllSingleThrows()

        return finishingThrows
            .filter { finish -> score - finish.score > 0 }
            .flatMap { finish ->
                val remaining = score - finish.score
                getAllSingleThrows()
                    .filter { it.score == remaining }
                    .map { setup -> CheckoutPath(listOf(setup, finish)) }
            }
            .sortedByDescending { it.throws.first().score }
    }

    /**
     * Finds 3-dart checkouts for the given score.
     * Returns paths sorted by total setup score (highest first).
     */
    private fun findThreeDartFinishes(score: Int, doubleOut: Boolean): List<CheckoutPath> {
        val finishingThrows = if (doubleOut) getDoubleFinishingThrows() else getAllSingleThrows()
        val allThrows = getAllSingleThrows()

        return finishingThrows
            .filter { finish -> score - finish.score > 0 }
            .flatMap { finish ->
                val remainingAfterFinish = score - finish.score
                allThrows
                    .filter { setup1 -> remainingAfterFinish - setup1.score > 0 }
                    .flatMap { setup1 ->
                        val remainingAfterSetup1 = remainingAfterFinish - setup1.score
                        allThrows
                            .filter { it.score == remainingAfterSetup1 }
                            .map { setup2 -> CheckoutPath(listOf(setup1, setup2, finish)) }
                    }
            }
            .sortedByDescending { path -> path.throws.dropLast(1).sumOf { it.score } }
    }

    /**
     * Returns all possible finishing throws (doubles and bull).
     */
    private fun getDoubleFinishingThrows(): List<Throw> =
        (1..20).map { double(it) } + bull()

    /**
     * Returns all possible single throws including singles, doubles, and triples.
     */
    private fun getAllSingleThrows(): List<Throw> =
        (1..20).map { single(it) } +
            outerBull() +
            (1..20).map { double(it) } +
            (1..20).map { triple(it) } +
            bull()

    /**
     * Generates a list of random valid checkout targets for practice.
     *
     * @param count Number of targets to generate
     * @param range The score range to pick from
     * @param doubleOut Whether double-out is required (filters out impossible scores like 169, 168, 166, 165, 163, 162, 159)
     * @return List of random valid checkout scores
     */
    fun generateCheckoutTargets(count: Int, range: IntRange, doubleOut: Boolean): List<Int> {
        val validScores = range.filter { score ->
            getCheckoutOptions(score, doubleOut) != null
        }
        return (1..count).map { validScores.random() }
    }

    /**
     * Maps a checkout practice game type to its score range.
     */
    fun getScoreRange(gameType: GameType): IntRange = when (gameType) {
        GameType.CHECKOUT_EASY -> 2..40
        GameType.CHECKOUT_MEDIUM -> 41..100
        GameType.CHECKOUT_HARD -> 101..170
        GameType.CHECKOUT_FULL -> 2..170
        else -> 2..170
    }

    // Helper functions to create throws
    private fun single(segment: Int): Throw = Throw(segment = segment, multiplier = Multiplier.SINGLE)
    private fun double(segment: Int): Throw = Throw(segment = segment, multiplier = Multiplier.DOUBLE)
    private fun triple(segment: Int): Throw = Throw(segment = segment, multiplier = Multiplier.TRIPLE)
    private fun outerBull(): Throw = Throw(segment = 25, multiplier = Multiplier.SINGLE)
    private fun bull(): Throw = Throw(segment = 25, multiplier = Multiplier.DOUBLE)
}

/**
 * Extension function to format a Throw for display.
 */
fun Throw.toDisplayString(): String = when {
    segment == 25 && multiplier == Multiplier.DOUBLE -> "Bull"
    segment == 25 -> "S25"
    else -> "${multiplier.name.first()}$segment"
}
