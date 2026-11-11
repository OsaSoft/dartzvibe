package cloud.osasoft.dartzvibe.domain.history

import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import cloud.osasoft.dartzvibe.domain.game.CricketRules
import cloud.osasoft.dartzvibe.domain.game.RouletteRules
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/*
 * Mode-specific context for the Game Details turn history.
 *
 * Everything here is derived from what is already stored (turns, config and leg state), so games
 * saved before this existed display the same way and nothing new has to be persisted.
 */

/** What a single dart achieved, used to emphasize or mute it in the history. */
enum class DartOutcome {
    /** The dart counted: it hit the Roulette target, or added Cricket marks or points. */
    HIT,

    /** The dart landed on the board but did nothing for the thrower. */
    NO_EFFECT,

    /** The dart missed the board (stored as segment 0). */
    MISS,
}

private const val MISS_SEGMENT = 0
private const val BULL_SEGMENT = 25
private const val LEGACY_BULLSEYE_SEGMENT = 50

private fun noEffectOutcome(dart: Throw): DartOutcome = if (dart.segment == MISS_SEGMENT) {
    DartOutcome.MISS
} else {
    DartOutcome.NO_EFFECT
}

/**
 * Badge label for modes that emphasize darts by outcome instead of by multiplier color, so the
 * multiplier has to stay readable in the text: "Miss", "7", "D7", "T7", "25" and "Bull".
 */
fun historyDartLabel(dart: Throw): String = when {
    dart.segment == MISS_SEGMENT -> "Miss"

    dart.segment == LEGACY_BULLSEYE_SEGMENT -> "Bull"

    dart.segment == BULL_SEGMENT && dart.multiplier == Multiplier.DOUBLE -> "Bull"

    dart.segment == BULL_SEGMENT -> "25"

    else -> when (dart.multiplier) {
        Multiplier.SINGLE -> dart.segment.toString()
        Multiplier.DOUBLE -> "D${dart.segment}"
        Multiplier.TRIPLE -> "T${dart.segment}"
    }
}

/** Spoken description of a dart whose badge is styled by [outcome], e.g. "T7, hit" or "Miss". */
fun historyDartDescription(dart: Throw, outcome: DartOutcome): String = when (outcome) {
    DartOutcome.HIT -> "${historyDartLabel(dart)}, hit"
    DartOutcome.NO_EFFECT -> "${historyDartLabel(dart)}, no effect"
    DartOutcome.MISS -> "Miss"
}

// ---- Roulette ----

/** Same rule as `RouletteModeEngine`: only a dart on the round's target segment scores. */
fun rouletteDartOutcome(dart: Throw, target: Int): DartOutcome = if (dart.segment == target) {
    DartOutcome.HIT
} else {
    noEffectOutcome(dart)
}

/**
 * One Roulette turn with its round context.
 *
 * @property round 1-based round number within the leg.
 * @property points Points the turn scored, from the stored scores.
 */
data class RouletteTurnContext(
    val turn: Turn,
    val round: Int,
    val target: Int,
    val dartOutcomes: List<DartOutcome>,
    val points: Int,
)

@OptIn(ExperimentalUuidApi::class)
fun rouletteTurnContexts(leg: Leg, config: GameConfig): List<RouletteTurnContext> {
    val playerCount = config.playerIds.size.coerceAtLeast(1)
    return leg.playerTurns.mapIndexed { index, turn ->
        val roundIndex = index / playerCount
        val target = RouletteRules.targetFor(config.rouletteTargetSegments, roundIndex)
        RouletteTurnContext(
            turn = turn,
            round = roundIndex + 1,
            target = target,
            dartOutcomes = turn.throws.map { dart -> rouletteDartOutcome(dart, target) },
            points = turn.scoreAfterTurn - turn.scoreBeforeTurn,
        )
    }
}

// ---- Cricket ----

/** What one dart did in Cricket, replayed through [CricketRules]. */
data class CricketDartEffect(
    val dart: Throw,
    val marksAdded: Int,
    val pointsScored: Int,
) {
    val outcome: DartOutcome
        get() = if (marksAdded > 0 || pointsScored > 0) DartOutcome.HIT else noEffectOutcome(dart)
}

/**
 * What one Cricket turn did.
 *
 * @property closedSegments Numbers the thrower closed during this turn, in the leg's segment order.
 */
@OptIn(ExperimentalUuidApi::class)
data class CricketTurnEffect(
    val turn: Turn,
    val darts: List<CricketDartEffect>,
    val closedSegments: List<Int>,
) {
    val marksAdded: Int get() = darts.sumOf { it.marksAdded }
    val pointsScored: Int get() = darts.sumOf { it.pointsScored }
}

/**
 * Replays the leg's turns through [CricketRules], the same rules the engine applies while playing,
 * starting from an empty board on the leg's segments (falling back to the config's, as the engine does).
 */
@OptIn(ExperimentalUuidApi::class)
fun cricketTurnEffects(leg: Leg, config: GameConfig): List<CricketTurnEffect> {
    val initialState = CricketState(
        segments = leg.cricketState?.segments ?: config.cricketSegments ?: CricketSegments.standard(),
    )
    return leg.playerTurns
        .fold(initialState to emptyList<CricketTurnEffect>()) { (stateBefore, effects), turn ->
            val (stateAfter, darts) = turn.throws
                .fold(stateBefore to emptyList<CricketDartEffect>()) { (state, dartEffects), dart ->
                    val outcome = CricketRules.applyThrow(
                        state = state,
                        throwObj = dart,
                        playerId = turn.playerId,
                        playerIds = config.playerIds,
                    )
                    outcome.state to dartEffects + CricketDartEffect(
                        dart = dart,
                        marksAdded = outcome.marksAdded,
                        pointsScored = outcome.pointsScored,
                    )
                }
            val before = stateBefore.getPlayerState(turn.playerId)
            val after = stateAfter.getPlayerState(turn.playerId)
            val closedSegments = stateAfter.segments.segments.filter { segment ->
                after.isClosed(segment) && !before.isClosed(segment)
            }
            stateAfter to effects + CricketTurnEffect(turn = turn, darts = darts, closedSegments = closedSegments)
        }
        .second
}

// ---- Parcheesi ----

/** A player knocked back to 0, and the score they had before. */
@OptIn(ExperimentalUuidApi::class)
data class ParcheesiKnockout(
    val playerId: Uuid,
    val scoreBefore: Int,
)

/** One Parcheesi turn with the knockouts it caused. */
data class ParcheesiTurnContext(
    val turn: Turn,
    val knockouts: List<ParcheesiKnockout>,
) {
    val isBounce: Boolean get() = turn.isBounce
}

/**
 * @property turns One entry per [Leg.playerTurns] entry.
 * @property unfinishedTurnKnockouts Knockouts dealt during a turn that was never finished (the game
 *   was saved or abandoned mid-turn), so there is no stored turn to attach them to.
 */
data class ParcheesiLegHistory(
    val turns: List<ParcheesiTurnContext>,
    val unfinishedTurnKnockouts: List<ParcheesiKnockout>,
)

/**
 * Links knockouts to the turns that caused them.
 *
 * `ParcheesiModeEngine` records a knockout as a phantom turn (`isPhantom`, score reset to 0) the
 * moment the knocking dart is thrown, and appends the thrower's own turn only when that turn ends.
 * So every phantom belongs to the **next** player turn in [Leg.turns]. Position alone decides it:
 * the thrower may keep scoring after the knockout, so scores need not match.
 */
@OptIn(ExperimentalUuidApi::class)
fun parcheesiLegHistory(leg: Leg): ParcheesiLegHistory {
    val playerTurns = leg.playerTurns
    val knockoutsByTurnIndex = leg.turns
        .fold(0 to emptyMap<Int, List<ParcheesiKnockout>>()) { (playerTurnIndex, knockouts), turn ->
            if (turn.isPhantom) {
                val knockout = ParcheesiKnockout(playerId = turn.playerId, scoreBefore = turn.scoreBeforeTurn)
                val turnKnockouts = knockouts[playerTurnIndex].orEmpty() + knockout
                playerTurnIndex to knockouts + (playerTurnIndex to turnKnockouts)
            } else {
                playerTurnIndex + 1 to knockouts
            }
        }
        .second
    return ParcheesiLegHistory(
        turns = playerTurns.mapIndexed { index, turn ->
            ParcheesiTurnContext(turn = turn, knockouts = knockoutsByTurnIndex[index].orEmpty())
        },
        unfinishedTurnKnockouts = knockoutsByTurnIndex[playerTurns.size].orEmpty(),
    )
}
