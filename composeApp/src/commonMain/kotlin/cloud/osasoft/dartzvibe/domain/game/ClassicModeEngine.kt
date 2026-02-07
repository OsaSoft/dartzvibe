package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
internal data class ClassicModeEngine(
    override val session: GameSession,
    override val currentTurnThrows: List<Throw> = emptyList(),
    val pendingScore: Int? = null,
    val isBusted: Boolean = false,
) : ModeEngine {

    private val config get() = session.config

    private fun currentPlayerId() = GameEngineHelper.getCurrentPlayerId(config, session)

    private fun playerScore(playerId: kotlin.uuid.Uuid) =
        GameEngineHelper.getPlayerScore(config, session, playerId)

    override fun getCurrentPlayerScore(): Int = pendingScore ?: playerScore(currentPlayerId())

    override fun isTurnEnded(): Boolean = isBusted

    override fun addThrow(throwObj: Throw): Pair<ModeEngine, ThrowResult> {
        val currentScore = pendingScore ?: playerScore(currentPlayerId())
        val newScore = currentScore - throwObj.score

        val bustResult = checkBust(newScore, throwObj)
        if (bustResult != null) {
            val newEngine = copy(
                currentTurnThrows = currentTurnThrows + throwObj,
                pendingScore = playerScore(currentPlayerId()),
                isBusted = true,
            )
            return newEngine to bustResult
        }

        if (newScore == 0) {
            val newEngine = copy(
                currentTurnThrows = currentTurnThrows + throwObj,
                pendingScore = 0,
            )
            return newEngine to ThrowResult.Checkout(currentPlayerId())
        }

        val newEngine = copy(
            currentTurnThrows = currentTurnThrows + throwObj,
            pendingScore = newScore,
        )
        return newEngine to ThrowResult.Success(newScore)
    }

    override fun endTurn(): Pair<ModeEngine, TurnResult> {
        val currentPlayerId = currentPlayerId()
        val scoreBeforeTurn = playerScore(currentPlayerId)
        val scoreAfterTurn = if (isBusted) scoreBeforeTurn else (pendingScore ?: scoreBeforeTurn)

        val turn = Turn(
            playerId = currentPlayerId,
            throws = currentTurnThrows,
            scoreBeforeTurn = scoreBeforeTurn,
            scoreAfterTurn = scoreAfterTurn,
            isBust = isBusted,
            isBounce = false,
        )

        val currentLeg = session.currentLeg
        val updatedLeg = currentLeg.copy(turns = currentLeg.turns + turn)

        if (scoreAfterTurn == 0 && !isBusted) {
            val (newSession, result) = GameEngineHelper.handleLegWon(
                config,
                session,
                updatedLeg,
                currentPlayerId,
            )
            return ClassicModeEngine(newSession) to result
        }

        val newSession = GameEngineHelper.updateLegInSession(session, updatedLeg)
        val newEngine = ClassicModeEngine(newSession)
        return newEngine to TurnResult.NextPlayer(newEngine.currentPlayerId())
    }

    override fun undoLastThrow(): ModeEngine? {
        if (currentTurnThrows.isEmpty()) return null

        val newThrows = currentTurnThrows.dropLast(1)
        val newScore = if (newThrows.isEmpty()) {
            null
        } else {
            val originalScore = playerScore(currentPlayerId())
            originalScore - newThrows.sumOf { it.score }
        }

        return copy(
            currentTurnThrows = newThrows,
            pendingScore = newScore,
            isBusted = false,
        )
    }

    override fun withNonScoringThrow(throwObj: Throw): ModeEngine =
        copy(currentTurnThrows = currentTurnThrows + throwObj)

    private fun checkBust(newScore: Int, lastThrow: Throw): ThrowResult.Bust? {
        if (newScore < 0) {
            return ThrowResult.Bust("Score below zero")
        }
        if (newScore == 1 && config.doubleOut) {
            return ThrowResult.Bust("Score at 1 with double-out required")
        }
        if (newScore == 0 && config.doubleOut && lastThrow.multiplier != Multiplier.DOUBLE) {
            return ThrowResult.Bust("Must finish on a double")
        }
        return null
    }
}
