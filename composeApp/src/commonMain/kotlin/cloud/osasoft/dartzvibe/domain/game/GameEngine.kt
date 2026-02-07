package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Result of adding a throw to the current turn.
 */
@OptIn(ExperimentalUuidApi::class)
sealed class ThrowResult {
    data class Success(val newScore: Int) : ThrowResult()

    data class Bust(val reason: String) : ThrowResult()

    data class Checkout(val winnerId: Uuid) : ThrowResult()

    data class BounceBack(val newScore: Int, val overshoot: Int) : ThrowResult()

    data class SuccessWithKnockout(
        val newScore: Int,
        val knockedOutPlayerIds: List<Uuid>,
    ) : ThrowResult()

    data class CricketMarks(
        val segment: Int,
        val marksAdded: Int,
        val totalMarks: Int,
        val pointsScored: Int,
    ) : ThrowResult()

    data class CricketWin(val winnerId: Uuid) : ThrowResult()
}

/**
 * Result of ending a turn.
 */
@OptIn(ExperimentalUuidApi::class)
sealed class TurnResult {
    data class NextPlayer(val playerId: Uuid) : TurnResult()

    data class LegWon(val winnerId: Uuid, val matchContinues: Boolean) : TurnResult()

    data class MatchWon(val winnerId: Uuid) : TurnResult()

    data class NextPlayerWithKnockout(
        val playerId: Uuid,
        val knockedOutPlayerIds: List<Uuid>,
    ) : TurnResult()
}

/**
 * Core game engine that manages game state and logic.
 * This class is immutable - operations return new engine instances.
 *
 * Delegates mode-specific logic to [ModeEngine] implementations:
 * [ClassicModeEngine], [ParcheesiModeEngine], and [CricketModeEngine].
 */
@OptIn(ExperimentalUuidApi::class)
class GameEngine private constructor(
    private val modeEngine: ModeEngine,
) {

    companion object {
        fun fromSession(session: GameSession): GameEngine {
            val engine = when (session.config.gameMode) {
                GameMode.CLASSIC -> ClassicModeEngine(session)

                GameMode.PARCHEESI -> ParcheesiModeEngine(session)

                GameMode.CRICKET -> {
                    val initialCricketState = session.currentLeg.cricketState ?: CricketState(
                        segments = session.config.cricketSegments ?: CricketSegments.standard(),
                    )
                    CricketModeEngine(session, pendingCricketState = initialCricketState)
                }
            }
            return GameEngine(engine)
        }
    }

    val config: GameConfig get() = modeEngine.session.config
    val status: GameStatus get() = modeEngine.session.status

    fun getCurrentPlayerId(): Uuid =
        GameEngineHelper.getCurrentPlayerId(config, modeEngine.session)

    fun getPlayerScore(playerId: Uuid): Int =
        GameEngineHelper.getPlayerScore(config, modeEngine.session, playerId)

    fun getCurrentPlayerScore(): Int = modeEngine.getCurrentPlayerScore()

    fun getCurrentTurnThrows(): List<Throw> = modeEngine.currentTurnThrows

    fun getLegsWon(playerId: Uuid): Int =
        GameEngineHelper.getLegsWon(modeEngine.session, playerId)

    fun isTurnEnded(): Boolean = modeEngine.isTurnEnded()

    fun addThrow(segment: Int, multiplier: Multiplier): Pair<GameEngine, ThrowResult> {
        if (modeEngine.isTurnEnded() || modeEngine.currentTurnThrows.size >= 3) {
            return this to ThrowResult.Bust("Turn already ended")
        }

        if (config.doubleIn && !hasDoubledIn() && multiplier != Multiplier.DOUBLE) {
            val throwObj = Throw(segment, multiplier)
            val newModeEngine = modeEngine.withNonScoringThrow(throwObj)
            return GameEngine(newModeEngine) to ThrowResult.Success(getPlayerScore(getCurrentPlayerId()))
        }

        val throwObj = Throw(segment, multiplier)
        val (newModeEngine, result) = modeEngine.addThrow(throwObj)
        return GameEngine(newModeEngine) to result
    }

    fun endTurn(): Pair<GameEngine, TurnResult> {
        val (newModeEngine, result) = modeEngine.endTurn()
        return GameEngine(newModeEngine) to result
    }

    fun undoLastThrow(): GameEngine? {
        val newModeEngine = modeEngine.undoLastThrow() ?: return null
        return GameEngine(newModeEngine)
    }

    fun getCricketState(): CricketState? =
        (modeEngine as? CricketModeEngine)?.getCricketState()
            ?: modeEngine.session.currentLeg.cricketState

    fun toGameSession(): GameSession = modeEngine.session

    private fun hasDoubledIn(): Boolean {
        val currentPlayerId = getCurrentPlayerId()
        val currentLeg = modeEngine.session.currentLeg

        val hasDoubleInPreviousTurn = currentLeg.turns.any { turn ->
            turn.playerId == currentPlayerId && turn.throws.any { it.multiplier == Multiplier.DOUBLE }
        }
        if (hasDoubleInPreviousTurn) return true

        return modeEngine.currentTurnThrows.any { it.multiplier == Multiplier.DOUBLE }
    }
}
