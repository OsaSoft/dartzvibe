package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Parcheesi scoring shared by [ParcheesiModeEngine], the score cards, the Game Details history and statistics, so
 * they can't drift apart. Nothing extra is stored: whether a dart counted is derived from the turn's darts and
 * scores, so games saved before these rules still load.
 */
object ParcheesiRules {

    /**
     * Under double-out, a dart that would reach the target exactly without a double scores nothing: the score stays,
     * nothing bounces, and the player throws on with the darts left. The dart stays in the turn as thrown.
     */
    fun isNonCountingDart(
        currentScore: Int,
        dart: Throw,
        config: GameConfig,
    ): Boolean = config.doubleOut &&
        currentScore + dart.score == config.targetScore &&
        dart.multiplier != Multiplier.DOUBLE

    /**
     * For each of a turn's [darts], whether it didn't count ([isNonCountingDart]), replayed from [startScore] (the
     * score at the turn start) the way the engine scores them: under double-in, darts before the first double score
     * nothing (and so never reach the target), [doubledIn] telling whether the player had doubled in before the turn.
     */
    fun nonCountingDarts(
        startScore: Int,
        darts: List<Throw>,
        config: GameConfig,
        doubledIn: Boolean,
    ): List<Boolean> = darts
        .fold(Triple(startScore, doubledIn || !config.doubleIn, emptyList<Boolean>())) {
                (score, scoring, flags),
                dart,
            ->
            val scores = scoring || dart.multiplier == Multiplier.DOUBLE
            when {
                !scores -> Triple(score, false, flags + false)
                isNonCountingDart(score, dart, config) -> Triple(score, true, flags + true)
                else -> Triple(score + dart.score, true, flags + false)
            }
        }.third

    /**
     * Whether [playerId] had doubled in before the turn that follows [turns] (the leg's earlier turns): any of their
     * darts there was a double. This is the `doubledIn` that [nonCountingDarts] takes.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun hasDoubledIn(
        turns: List<Turn>,
        playerId: Uuid,
    ): Boolean = turns.any { turn ->
        turn.playerId == playerId && turn.throws.any { it.multiplier == Multiplier.DOUBLE }
    }

    /**
     * The points [turn] scored: the score it added, or for a bounce the darts that counted (a bounce takes the score
     * back down, so the difference says nothing about the darts). Darts that hit the target without the double
     * double-out needs ([isNonCountingDart]) count as 0, and so do darts before doubling in under double-in (they
     * don't move the score).
     */
    fun turnPoints(
        turn: Turn,
        config: GameConfig,
    ): Int {
        if (!turn.isBounce) return turn.scoreAfterTurn - turn.scoreBeforeTurn
        // Not reachable before doubling in: one turn scores at most 180, below any target, so a player who bounces
        // already scored (and doubled in) in an earlier turn, and every dart of this turn scored
        return turn.throws
            .fold(turn.scoreBeforeTurn to 0) { (score, points), dart ->
                if (isNonCountingDart(score, dart, config)) {
                    score to points
                } else {
                    score + dart.score to points + dart.score
                }
            }.second
    }
}
