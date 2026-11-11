package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.Throw
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Result of applying one dart to a [CricketState].
 *
 * @property state The Cricket state after the dart.
 * @property marksAdded Marks actually added (capped so a number never exceeds 3 marks).
 * @property totalMarks The thrower's marks on the dart's segment afterwards (0 for non-Cricket numbers).
 * @property pointsScored Overflow points scored by the dart.
 */
internal data class CricketThrowOutcome(
    val state: CricketState,
    val marksAdded: Int,
    val totalMarks: Int,
    val pointsScored: Int,
)

/**
 * The Cricket marking rules, shared by [CricketModeEngine] (live play) and the game history replay,
 * so both always agree on what a dart did.
 */
@OptIn(ExperimentalUuidApi::class)
internal object CricketRules {

    private const val MARKS_TO_CLOSE = 3

    /**
     * Applies [throwObj] by [playerId] to [state]:
     * - darts on non-Cricket numbers (including misses) change nothing;
     * - marks are capped at 3 per number;
     * - marks beyond 3 score `segment` points each, but only while at least one opponent
     *   (from [playerIds]) still has the number open.
     */
    fun applyThrow(
        state: CricketState,
        throwObj: Throw,
        playerId: Uuid,
        playerIds: List<Uuid>,
    ): CricketThrowOutcome {
        val segment = throwObj.segment
        if (!state.segments.isTarget(segment)) {
            return CricketThrowOutcome(state = state, marksAdded = 0, totalMarks = 0, pointsScored = 0)
        }

        val playerState = state.getPlayerState(playerId)
        val currentMarks = playerState.getMarks(segment)
        val marksIfAllApplied = currentMarks + throwObj.multiplier.value
        val newTotalMarks = minOf(marksIfAllApplied, MARKS_TO_CLOSE)

        val opponentsAllClosed = playerIds
            .filter { it != playerId }
            .all { state.getPlayerState(it).isClosed(segment) }
        val pointsScored = if (opponentsAllClosed) {
            0
        } else {
            maxOf(0, marksIfAllApplied - MARKS_TO_CLOSE) * segment
        }

        val newPlayerState = playerState.copy(
            marks = playerState.marks + (segment to newTotalMarks),
            points = playerState.points + pointsScored,
        )
        return CricketThrowOutcome(
            state = state.withPlayerState(playerId, newPlayerState),
            marksAdded = newTotalMarks - currentMarks,
            totalMarks = newTotalMarks,
            pointsScored = pointsScored,
        )
    }

    /** A player wins once every number is closed and no opponent has more points. */
    fun isWin(state: CricketState, playerId: Uuid, playerIds: List<Uuid>): Boolean {
        val playerState = state.getPlayerState(playerId)
        val allClosed = state.segments.segments.all { playerState.isClosed(it) }
        if (!allClosed) return false

        return playerIds
            .filter { it != playerId }
            .none { state.getPlayerState(it).points > playerState.points }
    }
}
