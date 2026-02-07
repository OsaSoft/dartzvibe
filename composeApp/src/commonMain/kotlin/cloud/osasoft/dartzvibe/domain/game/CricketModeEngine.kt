package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.CricketPlayerState
import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
        val playerState = pendingCricketState.getPlayerState(currentPlayerId)
        val segment = throwObj.segment
        val marksToAdd = throwObj.multiplier.value

        if (!pendingCricketState.segments.isTarget(segment)) {
            val newEngine = copy(currentTurnThrows = currentTurnThrows + throwObj)
            return newEngine to ThrowResult.CricketMarks(
                segment = segment,
                marksAdded = 0,
                totalMarks = 0,
                pointsScored = 0,
            )
        }

        val currentMarks = playerState.getMarks(segment)
        val totalMarksIfAllApplied = currentMarks + marksToAdd
        val actualMarksAdded = minOf(marksToAdd, 3 - currentMarks)
        val newTotalMarks = minOf(totalMarksIfAllApplied, 3)

        val pointsScored = calculateCricketPoints(
            segment = segment,
            marksToAdd = marksToAdd,
            currentMarks = currentMarks,
            cricketState = pendingCricketState,
            currentPlayerId = currentPlayerId,
        )

        val newPlayerState = playerState.copy(
            marks = playerState.marks + (segment to newTotalMarks),
            points = playerState.points + pointsScored,
        )
        val newCricketState = pendingCricketState.withPlayerState(currentPlayerId, newPlayerState)

        if (checkCricketWin(newCricketState, currentPlayerId)) {
            val newEngine = copy(
                currentTurnThrows = currentTurnThrows + throwObj,
                pendingCricketState = newCricketState,
            )
            return newEngine to ThrowResult.CricketWin(currentPlayerId)
        }

        val newEngine = copy(
            currentTurnThrows = currentTurnThrows + throwObj,
            pendingCricketState = newCricketState,
        )
        return newEngine to ThrowResult.CricketMarks(
            segment = segment,
            marksAdded = actualMarksAdded,
            totalMarks = newTotalMarks,
            pointsScored = pointsScored,
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

        if (checkCricketWin(pendingCricketState, currentPlayerId)) {
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

        var cricketState = baseCricketState
        newThrows.forEach { throwObj ->
            cricketState = replayThrowForCricket(cricketState, throwObj, currentPlayerId)
        }

        return copy(
            currentTurnThrows = newThrows,
            pendingCricketState = cricketState,
        )
    }

    fun getCricketState(): CricketState = pendingCricketState

    fun getCricketPlayerState(playerId: Uuid): CricketPlayerState =
        pendingCricketState.getPlayerState(playerId)

    fun getCricketPoints(playerId: Uuid): Int = getCricketPlayerState(playerId).points

    private fun calculateCricketPoints(
        segment: Int,
        marksToAdd: Int,
        currentMarks: Int,
        cricketState: CricketState,
        currentPlayerId: Uuid,
    ): Int {
        val opponentsAllClosed = config.playerIds
            .filter { it != currentPlayerId }
            .all { cricketState.getPlayerState(it).isClosed(segment) }

        if (opponentsAllClosed) return 0

        val totalMarksIfApplied = currentMarks + marksToAdd
        val excessMarks = maxOf(0, totalMarksIfApplied - 3)
        return excessMarks * segment
    }

    private fun checkCricketWin(cricketState: CricketState, playerId: Uuid): Boolean {
        val playerState = cricketState.getPlayerState(playerId)

        val allClosed = cricketState.segments.segments.all { playerState.isClosed(it) }
        if (!allClosed) return false

        val playerPoints = playerState.points
        val opponentsHaveMorePoints = config.playerIds
            .filter { it != playerId }
            .any { cricketState.getPlayerState(it).points > playerPoints }

        return !opponentsHaveMorePoints
    }

    private fun replayThrowForCricket(
        cricketState: CricketState,
        throwObj: Throw,
        playerId: Uuid,
    ): CricketState {
        val segment = throwObj.segment
        if (!cricketState.segments.isTarget(segment)) return cricketState

        val playerState = cricketState.getPlayerState(playerId)
        val currentMarks = playerState.getMarks(segment)
        val marksToAdd = throwObj.multiplier.value
        val newTotalMarks = minOf(currentMarks + marksToAdd, 3)

        val pointsScored = calculateCricketPoints(
            segment = segment,
            marksToAdd = marksToAdd,
            currentMarks = currentMarks,
            cricketState = cricketState,
            currentPlayerId = playerId,
        )

        val newPlayerState = playerState.copy(
            marks = playerState.marks + (segment to newTotalMarks),
            points = playerState.points + pointsScored,
        )
        return cricketState.withPlayerState(playerId, newPlayerState)
    }
}
