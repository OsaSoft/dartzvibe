package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.CheckoutPracticeState
import cloud.osasoft.dartzvibe.data.model.CheckoutRoundResult
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
internal data class CheckoutPracticeModeEngine(
    override val session: GameSession,
    override val currentTurnThrows: List<Throw> = emptyList(),
    val pendingCheckoutPracticeState: CheckoutPracticeState,
    val pendingScore: Int? = null,
    val isBusted: Boolean = false,
) : ModeEngine {

    private val config get() = session.config

    private fun currentPlayerId() = GameEngineHelper.getCurrentPlayerId(config, session)

    override fun getCurrentPlayerScore(): Int = when {
        pendingScore != null -> pendingScore
        pendingCheckoutPracticeState.isComplete -> 0
        else -> pendingCheckoutPracticeState.currentTarget
    }

    override fun isTurnEnded(): Boolean = isBusted

    override fun addThrow(throwObj: Throw): Pair<ModeEngine, ThrowResult> {
        val currentScore = pendingScore ?: pendingCheckoutPracticeState.currentTarget
        val newScore = currentScore - throwObj.score

        val bustResult = checkBust(newScore, throwObj)
        if (bustResult != null) {
            val newEngine = copy(
                currentTurnThrows = currentTurnThrows + throwObj,
                pendingScore = pendingCheckoutPracticeState.currentTarget,
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
        val currentTarget = pendingCheckoutPracticeState.currentTarget
        val scoreAfterTurn = if (isBusted) currentTarget else (pendingScore ?: currentTarget)
        val isSuccess = scoreAfterTurn == 0 && !isBusted

        val turn = Turn(
            playerId = currentPlayerId,
            throws = currentTurnThrows,
            scoreBeforeTurn = currentTarget,
            scoreAfterTurn = scoreAfterTurn,
            isBust = isBusted,
        )

        val roundResult = CheckoutRoundResult(
            target = currentTarget,
            success = isSuccess,
            dartsUsed = currentTurnThrows.size,
            throws = currentTurnThrows,
        )

        val newState = pendingCheckoutPracticeState.copy(
            currentRoundIndex = pendingCheckoutPracticeState.currentRoundIndex + 1,
            roundResults = pendingCheckoutPracticeState.roundResults + roundResult,
        )

        val currentLeg = session.currentLeg
        val updatedLeg = currentLeg.copy(
            turns = currentLeg.turns + turn,
            checkoutPracticeState = newState,
        )

        if (newState.isComplete) {
            val updatedLegs = session.legs.toMutableList()
            updatedLegs[session.currentLegIndex] = updatedLeg.copy(winnerId = currentPlayerId)
            val newSession = session.copy(
                legs = updatedLegs,
                status = GameStatus.COMPLETED,
                finishedAt = currentTimeMillis(),
                winnerId = currentPlayerId,
            )
            return CheckoutPracticeModeEngine(
                session = newSession,
                pendingCheckoutPracticeState = newState,
            ) to TurnResult.MatchWon(currentPlayerId)
        }

        val newSession = GameEngineHelper.updateLegInSession(session, updatedLeg)
        val newEngine = CheckoutPracticeModeEngine(
            session = newSession,
            pendingCheckoutPracticeState = newState,
        )
        return newEngine to TurnResult.NextPlayer(currentPlayerId)
    }

    override fun undoLastThrow(): ModeEngine? {
        if (currentTurnThrows.isEmpty()) return null

        val newThrows = currentTurnThrows.dropLast(1)
        val newScore = if (newThrows.isEmpty()) {
            null
        } else {
            val target = pendingCheckoutPracticeState.currentTarget
            target - newThrows.sumOf { it.score }
        }

        return copy(
            currentTurnThrows = newThrows,
            pendingScore = newScore,
            isBusted = false,
        )
    }

    override fun withNonScoringThrow(throwObj: Throw): ModeEngine =
        copy(currentTurnThrows = currentTurnThrows + throwObj)

    fun getCheckoutPracticeState(): CheckoutPracticeState = pendingCheckoutPracticeState

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
