package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Darts that land the thrower's running score exactly on [opponentScore], knocking [opponentId] back to 0. */
@OptIn(ExperimentalUuidApi::class)
data class KnockoutRoute(
    val opponentId: Uuid,
    val opponentScore: Int,
    val throws: List<Throw>,
)

/**
 * Parcheesi knockout suggestions: how the thrower can land exactly on an opponent's score with the darts left.
 * Pure and cheap (a two-level loop over the 62 dart values); the rules themselves live in `ParcheesiModeEngine`.
 */
@OptIn(ExperimentalUuidApi::class)
object KnockoutCalculator {

    private const val MAX_DARTS = 3

    /** Every scoring dart, in the order the last dart of a route prefers: singles, 25, doubles, triples, Bull. */
    private val lastDartPreference: List<Throw> =
        (1..20).map { Throw(segment = it, multiplier = Multiplier.SINGLE) } +
            Throw(segment = 25, multiplier = Multiplier.SINGLE) +
            (1..20).map { Throw(segment = it, multiplier = Multiplier.DOUBLE) } +
            (1..20).map { Throw(segment = it, multiplier = Multiplier.TRIPLE) } +
            Throw(segment = 25, multiplier = Multiplier.DOUBLE)

    /** The preferred single dart for each score it can make. */
    private val lastDartByScore: Map<Int, Throw> = lastDartPreference
        .groupBy { it.score }
        .mapValues { (_, darts) -> darts.first() }

    /** Setup darts, highest first (stable, so ties keep [lastDartPreference]'s order), like checkout setups. */
    private val setupDarts: List<Throw> = lastDartPreference.sortedByDescending { it.score }

    /**
     * Up to [maxOptions] knockouts for a thrower on [currentScore] with [dartsLeft] darts, one per opponent:
     * fewest darts first, then the opponent with the higher score (the leader). Opponents on 0, at or behind the
     * thrower, or at/above [targetScore] are ignored. No double is needed, and because every dart scores and the
     * opponent is below the target, each intermediate score stays below the target (no bounce on the way).
     *
     * @param opponentScores the other players and their scores, in turn order (the final tie-break)
     */
    fun getKnockoutOptions(
        currentScore: Int,
        targetScore: Int,
        opponentScores: List<Pair<Uuid, Int>>,
        dartsLeft: Int,
        maxOptions: Int = 2,
    ): List<KnockoutRoute> = opponentScores
        .filter { (_, score) -> score > 0 && score > currentScore && score < targetScore }
        .mapNotNull { (id, score) ->
            route(gap = score - currentScore, maxDarts = dartsLeft)?.let { throws ->
                KnockoutRoute(opponentId = id, opponentScore = score, throws = throws)
            }
        }.sortedWith(compareBy<KnockoutRoute> { it.throws.size }.thenByDescending { it.opponentScore })
        .take(maxOptions)

    /** The fewest darts (at most [maxDarts], at most 3) scoring exactly [gap], or null when they can't. */
    fun route(
        gap: Int,
        maxDarts: Int,
    ): List<Throw>? = (1..minOf(maxDarts, MAX_DARTS)).firstNotNullOfOrNull { darts -> exactRoute(gap, darts) }

    /** Exactly [darts] darts scoring [gap]: the highest setup dart first, the preferred last dart by score. */
    private fun exactRoute(
        gap: Int,
        darts: Int,
    ): List<Throw>? = when {
        gap <= 0 -> null

        darts == 1 -> lastDartByScore[gap]?.let { listOf(it) }

        else -> setupDarts.firstNotNullOfOrNull { setup ->
            exactRoute(gap - setup.score, darts - 1)?.let { rest -> listOf(setup) + rest }
        }
    }
}
