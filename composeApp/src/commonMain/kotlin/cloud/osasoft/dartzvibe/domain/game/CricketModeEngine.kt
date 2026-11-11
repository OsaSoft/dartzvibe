package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
internal data class CricketModeEngine(
    override val session: GameSession,
    override val currentTurnThrows: List<Throw> = emptyList(),
    val pendingCricketState: CricketState,
) : ModeEngine {

    private val config get() = session.config

    private fun currentPlayerId() = GameEngineHelper.getCurrentPlayerId(config, session)

    override fun getCurrentPlayerScore(): Int =
        pendingCricketState.getPlayerState(currentPlayerId()).points

    override fun isTurnEnded(): Boolean = false

    override fun addThrow(throwObj: Throw): Pair<ModeEngine, ThrowResult> {
        val currentPlayerId = currentPlayerId()
        val outcome = CricketRules.applyThrow(
            state = pendingCricketState,
            throwObj = throwObj,
            playerId = currentPlayerId,
            playerIds = config.playerIds,
        )
        val newEngine = copy(
            currentTurnThrows = currentTurnThrows + throwObj,
            pendingCricketState = outcome.state,
        )

        if (!pendingCricketState.segments.isTarget(throwObj.segment)) {
            return newEngine to ThrowResult.CricketMarks(
                segment = throwObj.segment,
                marksAdded = 0,
                totalMarks = 0,
                pointsScored = 0,
            )
        }

        if (CricketRules.isWin(outcome.state, currentPlayerId, config.playerIds)) {
            return newEngine to ThrowResult.CricketWin(currentPlayerId)
        }

        return newEngine to ThrowResult.CricketMarks(
            segment = throwObj.segment,
            marksAdded = outcome.marksAdded,
            totalMarks = outcome.totalMarks,
            pointsScored = outcome.pointsScored,
        )
    }

    override fun endTurn(): Pair<ModeEngine, TurnResult> {
        val currentPlayerId = currentPlayerId()
        val playerState = pendingCricketState.getPlayerState(currentPlayerId)

        val turn = Turn(
            playerId = currentPlayerId,
            throws = currentTurnThrows,
            scoreBeforeTurn = 0,
            scoreAfterTurn = playerState.points,
            isBust = false,
            isBounce = false,
        )

        val currentLeg = session.currentLeg
        val updatedLeg = currentLeg.copy(
            turns = currentLeg.turns + turn,
            cricketState = pendingCricketState,
        )

        if (CricketRules.isWin(pendingCricketState, currentPlayerId, config.playerIds)) {
            val (newSession, result) = GameEngineHelper.handleLegWon(
                config,
                session,
                updatedLeg,
                currentPlayerId,
            )
            return CricketModeEngine(
                newSession,
                pendingCricketState = newSession.currentLeg.cricketState ?: CricketState(
                    segments = config.cricketSegments ?: CricketSegments.standard(),
                ),
            ) to result
        }

        val newSession = GameEngineHelper.updateLegInSession(session, updatedLeg)
        val newEngine = CricketModeEngine(
            newSession,
            pendingCricketState = pendingCricketState,
        )
        return newEngine to TurnResult.NextPlayer(newEngine.currentPlayerId())
    }

    override fun undoLastThrow(): ModeEngine? {
        if (currentTurnThrows.isEmpty()) return null

        val newThrows = currentTurnThrows.dropLast(1)

        val baseCricketState = session.currentLeg.cricketState ?: CricketState(
            segments = config.cricketSegments ?: CricketSegments.standard(),
        )
        val currentPlayerId = currentPlayerId()

        val cricketState = newThrows.fold(baseCricketState) { state, throwObj ->
            CricketRules.applyThrow(
                state = state,
                throwObj = throwObj,
                playerId = currentPlayerId,
                playerIds = config.playerIds,
            ).state
        }

        return copy(
            currentTurnThrows = newThrows,
            pendingCricketState = cricketState,
        )
    }

    fun getCricketState(): CricketState = pendingCricketState

    override fun withNonScoringThrow(throwObj: Throw): ModeEngine =
        copy(currentTurnThrows = currentTurnThrows + throwObj)
}
