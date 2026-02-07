package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal data class ParcheesiModeEngine(
    override val session: GameSession,
    override val currentTurnThrows: List<Throw> = emptyList(),
    val pendingScore: Int? = null,
    val isBounced: Boolean = false,
) : ModeEngine {

    private val config get() = session.config

    private fun currentPlayerId() = GameEngineHelper.getCurrentPlayerId(config, session)

    private fun playerScore(playerId: Uuid) =
        GameEngineHelper.getPlayerScore(config, session, playerId)

    override fun getCurrentPlayerScore(): Int = pendingScore ?: playerScore(currentPlayerId())

    override fun isTurnEnded(): Boolean = isBounced

    override fun addThrow(throwObj: Throw): Pair<ModeEngine, ThrowResult> {
        val currentScore = pendingScore ?: playerScore(currentPlayerId())
        val rawNewScore = currentScore + throwObj.score
        val targetScore = config.targetScore

        return when {
            rawNewScore < targetScore -> {
                val knockedOut = detectKnockouts(rawNewScore, currentPlayerId())
                if (knockedOut.isNotEmpty()) {
                    val updatedSession = applyKnockoutsToSession(knockedOut)
                    val newEngine = ParcheesiModeEngine(
                        updatedSession,
                        currentTurnThrows + throwObj,
                        rawNewScore,
                        isBounced = false,
                    )
                    newEngine to ThrowResult.SuccessWithKnockout(rawNewScore, knockedOut)
                } else {
                    val newEngine = copy(
                        currentTurnThrows = currentTurnThrows + throwObj,
                        pendingScore = rawNewScore,
                    )
                    newEngine to ThrowResult.Success(rawNewScore)
                }
            }

            rawNewScore == targetScore -> {
                if (config.doubleOut && throwObj.multiplier != Multiplier.DOUBLE) {
                    val newEngine = copy(
                        currentTurnThrows = currentTurnThrows + throwObj,
                        pendingScore = targetScore,
                        isBounced = true,
                    )
                    newEngine to ThrowResult.BounceBack(targetScore, 0)
                } else {
                    val newEngine = copy(
                        currentTurnThrows = currentTurnThrows + throwObj,
                        pendingScore = targetScore,
                    )
                    newEngine to ThrowResult.Checkout(currentPlayerId())
                }
            }

            else -> {
                val overshoot = rawNewScore - targetScore
                val bouncedScore = targetScore - overshoot
                val newEngine = copy(
                    currentTurnThrows = currentTurnThrows + throwObj,
                    pendingScore = bouncedScore,
                    isBounced = true,
                )
                newEngine to ThrowResult.BounceBack(bouncedScore, overshoot)
            }
        }
    }

    override fun endTurn(): Pair<ModeEngine, TurnResult> {
        val currentPlayerId = currentPlayerId()
        val scoreBeforeTurn = playerScore(currentPlayerId)
        val scoreAfterTurn = if (isBounced) {
            pendingScore ?: scoreBeforeTurn
        } else {
            pendingScore ?: scoreBeforeTurn
        }

        val turn = Turn(
            playerId = currentPlayerId,
            throws = currentTurnThrows,
            scoreBeforeTurn = scoreBeforeTurn,
            scoreAfterTurn = scoreAfterTurn,
            isBust = false,
            isBounce = isBounced,
        )

        val currentLeg = session.currentLeg
        val updatedLeg = currentLeg.copy(turns = currentLeg.turns + turn)

        val winScore = config.targetScore
        if (scoreAfterTurn == winScore && !isBounced) {
            val (newSession, result) = GameEngineHelper.handleLegWon(
                config,
                session,
                updatedLeg,
                currentPlayerId,
            )
            return ParcheesiModeEngine(newSession) to result
        }

        val newSession = GameEngineHelper.updateLegInSession(session, updatedLeg)
        val newEngine = ParcheesiModeEngine(newSession)
        return newEngine to TurnResult.NextPlayer(newEngine.currentPlayerId())
    }

    override fun undoLastThrow(): ModeEngine? {
        if (currentTurnThrows.isEmpty()) return null

        val newThrows = currentTurnThrows.dropLast(1)
        val newScore = if (newThrows.isEmpty()) {
            null
        } else {
            val originalScore = playerScore(currentPlayerId())
            originalScore + newThrows.sumOf { it.score }
        }

        return copy(
            currentTurnThrows = newThrows,
            pendingScore = newScore,
            isBounced = false,
        )
    }

    override fun withNonScoringThrow(throwObj: Throw): ModeEngine =
        copy(currentTurnThrows = currentTurnThrows + throwObj)

    private fun detectKnockouts(newScore: Int, currentPlayerId: Uuid): List<Uuid> {
        if (newScore == 0) return emptyList()
        return config.playerIds
            .filter { it != currentPlayerId }
            .filter { playerScore(it) == newScore }
    }

    private fun applyKnockoutsToSession(knockedOutPlayerIds: List<Uuid>): GameSession {
        val currentLeg = session.currentLeg
        val updatedLeg = applyKnockouts(currentLeg, knockedOutPlayerIds)
        val updatedLegs = session.legs.toMutableList()
        updatedLegs[session.currentLegIndex] = updatedLeg
        return session.copy(legs = updatedLegs)
    }

    private fun applyKnockouts(leg: Leg, knockedOutPlayerIds: List<Uuid>): Leg {
        val knockoutTurns = knockedOutPlayerIds.map { playerId ->
            val playerScoreValue = playerScore(playerId)
            Turn(
                playerId = playerId,
                throws = emptyList(),
                scoreBeforeTurn = playerScoreValue,
                scoreAfterTurn = 0,
                isBust = false,
                isBounce = false,
                isPhantom = true,
            )
        }
        return leg.copy(turns = leg.turns + knockoutTurns)
    }
}
