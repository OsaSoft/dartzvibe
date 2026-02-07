package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal object GameEngineHelper {

    fun getCurrentPlayerId(config: GameConfig, session: GameSession): Uuid {
        val currentLeg = session.currentLeg
        val playerCount = config.playerIds.size
        val turnCount = currentLeg.playerTurns.size
        return config.playerIds[turnCount % playerCount]
    }

    fun getPlayerScore(config: GameConfig, session: GameSession, playerId: Uuid): Int {
        val currentLeg = session.currentLeg
        val lastTurn = currentLeg.turns.lastOrNull { it.playerId == playerId }
        return lastTurn?.scoreAfterTurn ?: config.startingScore
    }

    fun getLegsWon(session: GameSession, playerId: Uuid): Int =
        session.legs.count { it.winnerId == playerId }

    fun handleLegWon(
        config: GameConfig,
        session: GameSession,
        completedLeg: Leg,
        winnerId: Uuid,
    ): Pair<GameSession, TurnResult> {
        val legWithWinner = completedLeg.copy(winnerId = winnerId)
        val updatedLegs = session.legs.toMutableList()
        updatedLegs[session.currentLegIndex] = legWithWinner

        val legsWon = updatedLegs.count { it.winnerId == winnerId }

        if (legsWon >= config.legsToWin) {
            val newSession = session.copy(
                legs = updatedLegs,
                status = GameStatus.COMPLETED,
                finishedAt = currentTimeMillis(),
                winnerId = winnerId,
            )
            return newSession to TurnResult.MatchWon(winnerId)
        }

        val newLegs = updatedLegs + Leg()
        val newSession = session.copy(
            legs = newLegs,
            currentLegIndex = session.currentLegIndex + 1,
        )

        return newSession to TurnResult.LegWon(winnerId, matchContinues = true)
    }

    fun updateLegInSession(session: GameSession, updatedLeg: Leg): GameSession {
        val updatedLegs = session.legs.toMutableList()
        updatedLegs[session.currentLegIndex] = updatedLeg
        return session.copy(legs = updatedLegs)
    }
}
