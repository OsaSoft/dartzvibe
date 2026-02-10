package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.RouletteState
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal data class RouletteModeEngine(
    override val session: GameSession,
    override val currentTurnThrows: List<Throw> = emptyList(),
    val pendingRouletteState: RouletteState,
    val pendingTurnPoints: Int = 0,
) : ModeEngine {

    private val config get() = session.config

    private fun currentPlayerId(): Uuid = GameEngineHelper.getCurrentPlayerId(config, session)

    private val currentTargetSegment: Int
        get() {
            val targets = config.rouletteTargetSegments ?: emptyList()
            return if (targets.isEmpty()) {
                1
            } else {
                targets[pendingRouletteState.currentRoundIndex % targets.size]
            }
        }

    override fun getCurrentPlayerScore(): Int =
        GameEngineHelper.getPlayerScore(config, session, currentPlayerId()) + pendingTurnPoints

    override fun isTurnEnded(): Boolean = false

    override fun addThrow(throwObj: Throw): Pair<ModeEngine, ThrowResult> {
        val pointsScored = if (throwObj.segment == currentTargetSegment) {
            throwObj.multiplier.value
        } else {
            0
        }

        val newTurnPoints = pendingTurnPoints + pointsScored
        val newTotalScore = GameEngineHelper.getPlayerScore(config, session, currentPlayerId()) + newTurnPoints

        val newEngine = copy(
            currentTurnThrows = currentTurnThrows + throwObj,
            pendingTurnPoints = newTurnPoints,
        )
        return newEngine to ThrowResult.RouletteHit(
            segment = throwObj.segment,
            pointsScored = pointsScored,
            newTotalScore = newTotalScore,
        )
    }

    override fun endTurn(): Pair<ModeEngine, TurnResult> {
        val currentPlayerId = currentPlayerId()
        val scoreBeforeTurn = GameEngineHelper.getPlayerScore(config, session, currentPlayerId)
        val scoreAfterTurn = scoreBeforeTurn + pendingTurnPoints

        val turn = Turn(
            playerId = currentPlayerId,
            throws = currentTurnThrows,
            scoreBeforeTurn = scoreBeforeTurn,
            scoreAfterTurn = scoreAfterTurn,
        )

        val currentLeg = session.currentLeg
        val updatedLeg = currentLeg.copy(
            turns = currentLeg.turns + turn,
            rouletteState = pendingRouletteState,
        )

        val updatedSession = GameEngineHelper.updateLegInSession(session, updatedLeg)

        // Check if a full round is complete (all players have thrown)
        val turnsInLeg = updatedLeg.playerTurns.size
        val playerCount = config.playerIds.size
        val isRoundComplete = turnsInLeg % playerCount == 0

        if (isRoundComplete) {
            val completedRoundIndex = pendingRouletteState.currentRoundIndex
            val nextRoundIndex = completedRoundIndex + 1

            // Check win conditions at round boundary
            val winResult = checkWinConditions(updatedSession, nextRoundIndex)
            if (winResult != null) {
                return winResult
            }

            // Advance to next round
            val newRouletteState = pendingRouletteState.copy(currentRoundIndex = nextRoundIndex)
            val legWithNewState = updatedLeg.copy(rouletteState = newRouletteState)
            val sessionWithNewState = GameEngineHelper.updateLegInSession(session, legWithNewState)

            val nextPlayerId = GameEngineHelper.getCurrentPlayerId(config, sessionWithNewState)
            return RouletteModeEngine(
                session = sessionWithNewState,
                pendingRouletteState = newRouletteState,
            ) to TurnResult.NextPlayer(nextPlayerId)
        }

        // Not end of round yet, just move to next player
        val nextPlayerId = GameEngineHelper.getCurrentPlayerId(config, updatedSession)
        return RouletteModeEngine(
            session = updatedSession,
            pendingRouletteState = pendingRouletteState,
        ) to TurnResult.NextPlayer(nextPlayerId)
    }

    private fun checkWinConditions(
        updatedSession: GameSession,
        nextRoundIndex: Int,
    ): Pair<ModeEngine, TurnResult>? {
        val playerScores = config.playerIds.map { playerId ->
            playerId to GameEngineHelper.getPlayerScore(config, updatedSession, playerId)
        }

        when (config.gameType) {
            GameType.ROULETTE_ROUNDS -> {
                val totalRounds = config.rouletteRounds ?: return null
                if (nextRoundIndex >= totalRounds) {
                    return findRoundsWinner(updatedSession, playerScores, totalRounds)
                }
            }

            GameType.ROULETTE_SCORE -> {
                val targetScore = config.rouletteTargetScore ?: return null
                val playersAtTarget = playerScores.filter { it.second >= targetScore }
                if (playersAtTarget.isNotEmpty()) {
                    // Highest score wins (round is already complete so it's fair)
                    val maxScore = playerScores.maxOf { it.second }
                    val winners = playerScores.filter { it.second == maxScore }
                    if (winners.size == 1) {
                        return finishGame(updatedSession, winners.first().first)
                    }
                    // Tie at target score - continue playing (sudden death)
                }
            }

            else -> {}
        }
        return null
    }

    private fun findRoundsWinner(
        updatedSession: GameSession,
        playerScores: List<Pair<Uuid, Int>>,
        totalRounds: Int,
    ): Pair<ModeEngine, TurnResult>? {
        val maxScore = playerScores.maxOf { it.second }
        val winners = playerScores.filter { it.second == maxScore }
        if (winners.size == 1) {
            return finishGame(updatedSession, winners.first().first)
        }
        // Tie - sudden death: continue playing more rounds
        // Don't declare a winner; the game continues
        return null
    }

    private fun finishGame(
        updatedSession: GameSession,
        winnerId: Uuid,
    ): Pair<ModeEngine, TurnResult> {
        val completedLeg = updatedSession.currentLeg.copy(winnerId = winnerId)
        val updatedLegs = updatedSession.legs.toMutableList()
        updatedLegs[updatedSession.currentLegIndex] = completedLeg
        val finishedSession = updatedSession.copy(
            legs = updatedLegs,
            status = GameStatus.COMPLETED,
            finishedAt = currentTimeMillis(),
            winnerId = winnerId,
        )
        return RouletteModeEngine(
            session = finishedSession,
            pendingRouletteState = pendingRouletteState,
        ) to TurnResult.MatchWon(winnerId)
    }

    override fun undoLastThrow(): ModeEngine? {
        if (currentTurnThrows.isEmpty()) return null

        val removedThrow = currentTurnThrows.last()
        val removedPoints = if (removedThrow.segment == currentTargetSegment) {
            removedThrow.multiplier.value
        } else {
            0
        }

        return copy(
            currentTurnThrows = currentTurnThrows.dropLast(1),
            pendingTurnPoints = pendingTurnPoints - removedPoints,
        )
    }

    override fun withNonScoringThrow(throwObj: Throw): ModeEngine =
        copy(currentTurnThrows = currentTurnThrows + throwObj)

    fun getRouletteState(): RouletteState = pendingRouletteState
}
