package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.Leg
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** A player's last finished turn in the current leg, as the score card's "Last:" line shows it. */
sealed interface LastTurn {
    /** No turn yet in this leg. */
    data object None : LastTurn

    /** The last turn busted. */
    data object Bust : LastTurn

    /** The last turn bounced off the Parcheesi target (overshot it). */
    data object Bounce : LastTurn

    /** The last turn scored [points]. */
    data class Score(val points: Int) : LastTurn

    companion object {
        /**
         * [playerId]'s last real turn in [leg]. Parcheesi knockout phantoms are skipped ([Leg.playerTurns]), so a
         * knocked-out player keeps their own last turn. A Parcheesi bounce reads [Bounce]. Roulette counts the points
         * the turn added (darts off the target score nothing), Parcheesi the points its darts counted
         * ([ParcheesiRules.turnPoints]); every other mode counts the darts' total.
         */
        @OptIn(ExperimentalUuidApi::class)
        fun of(
            leg: Leg,
            playerId: Uuid,
            isRoulette: Boolean,
            parcheesiConfig: GameConfig? = null,
        ): LastTurn {
            val turn = leg.playerTurns.lastOrNull { it.playerId == playerId } ?: return None
            return when {
                turn.isBust -> Bust
                turn.isBounce -> Bounce
                isRoulette -> Score(turn.scoreAfterTurn - turn.scoreBeforeTurn)
                parcheesiConfig != null -> Score(ParcheesiRules.turnPoints(turn, parcheesiConfig))
                else -> Score(turn.totalScore)
            }
        }
    }
}

/** The "Last:" line: a plain number counting down, "+N" counting up, "BUST", "BOUNCE", or an en dash before the first turn. */
fun LastTurn.label(isCountUp: Boolean): String = when (this) {
    LastTurn.None -> "Last: \u2013"
    LastTurn.Bust -> "Last: BUST"
    LastTurn.Bounce -> "Last: BOUNCE"
    is LastTurn.Score -> if (isCountUp) "Last: +$points" else "Last: $points"
}
