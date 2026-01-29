package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.CricketPlayerState
import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import cloud.osasoft.dartzvibe.util.currentTimeMillis
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
 */
@OptIn(ExperimentalUuidApi::class)
class GameEngine private constructor(
    private val session: GameSession,
    private val currentTurnThrows: List<Throw> = emptyList(),
    private val pendingScore: Int? = null,
    private val isBusted: Boolean = false,
    private val isBounced: Boolean = false,
    private val pendingCricketState: CricketState? = null,
) {

    companion object {
        fun fromSession(session: GameSession): GameEngine {
            val initialCricketState = if (session.config.isCricket) {
                session.currentLeg.cricketState ?: CricketState(
                    segments = session.config.cricketSegments ?: CricketSegments.standard(),
                )
            } else {
                null
            }
            return GameEngine(session, pendingCricketState = initialCricketState)
        }
    }

    val config: GameConfig get() = session.config
    val status: GameStatus get() = session.status

    fun getCurrentPlayerId(): Uuid {
        val currentLeg = session.currentLeg
        val playerCount = config.playerIds.size
        val turnCount = currentLeg.playerTurns.size
        return config.playerIds[turnCount % playerCount]
    }

    fun getPlayerScore(playerId: Uuid): Int {
        val currentLeg = session.currentLeg
        val lastTurn = currentLeg.turns.lastOrNull { it.playerId == playerId }
        return lastTurn?.scoreAfterTurn ?: config.startingScore
    }

    fun getCurrentPlayerScore(): Int = pendingScore ?: getPlayerScore(getCurrentPlayerId())

    fun getCurrentTurnThrows(): List<Throw> = currentTurnThrows

    fun getLegsWon(playerId: Uuid): Int = session.legs.count { it.winnerId == playerId }

    fun isTurnBusted(): Boolean = isBusted

    fun isTurnBounced(): Boolean = isBounced

    fun isTurnEnded(): Boolean = isBusted || isBounced

    fun addThrow(segment: Int, multiplier: Multiplier): Pair<GameEngine, ThrowResult> {
        if (isBusted || isBounced || currentTurnThrows.size >= 3) {
            return this to ThrowResult.Bust("Turn already ended")
        }

        val throwObj = Throw(segment, multiplier)
        val currentScore = pendingScore ?: getPlayerScore(getCurrentPlayerId())

        return when (config.gameMode) {
            GameMode.CLASSIC -> addThrowClassic(throwObj, currentScore)
            GameMode.PARCHEESI -> addThrowParcheesi(throwObj, currentScore)
            GameMode.CRICKET -> addThrowCricket(throwObj)
        }
    }

    private fun addThrowClassic(throwObj: Throw, currentScore: Int): Pair<GameEngine, ThrowResult> {
        val newScore = currentScore - throwObj.score

        // Check for bust conditions
        val bustResult = checkBust(newScore, throwObj)
        if (bustResult != null) {
            val newEngine = copy(
                currentTurnThrows = currentTurnThrows + throwObj,
                pendingScore = getPlayerScore(getCurrentPlayerId()), // Reset to score before turn
                isBusted = true,
            )
            return newEngine to bustResult
        }

        // Check for checkout (score reaches exactly 0)
        if (newScore == 0) {
            val newEngine = copy(
                currentTurnThrows = currentTurnThrows + throwObj,
                pendingScore = 0,
            )
            return newEngine to ThrowResult.Checkout(getCurrentPlayerId())
        }

        // Normal throw
        val newEngine = copy(
            currentTurnThrows = currentTurnThrows + throwObj,
            pendingScore = newScore,
        )
        return newEngine to ThrowResult.Success(newScore)
    }

    private fun addThrowParcheesi(throwObj: Throw, currentScore: Int): Pair<GameEngine, ThrowResult> {
        val rawNewScore = currentScore + throwObj.score
        val targetScore = config.targetScore

        return when {
            rawNewScore < targetScore -> {
                // Check for knockouts at this score
                val knockedOut = detectKnockouts(rawNewScore, getCurrentPlayerId())
                if (knockedOut.isNotEmpty()) {
                    // Apply knockouts to session immediately
                    val updatedSession = applyKnockoutsToSession(knockedOut)
                    val newEngine = GameEngine(
                        updatedSession,
                        currentTurnThrows + throwObj,
                        rawNewScore,
                        isBusted = false,
                        isBounced = false,
                    )
                    newEngine to ThrowResult.SuccessWithKnockout(rawNewScore, knockedOut)
                } else {
                    // Normal success
                    val newEngine = copy(
                        currentTurnThrows = currentTurnThrows + throwObj,
                        pendingScore = rawNewScore,
                    )
                    newEngine to ThrowResult.Success(rawNewScore)
                }
            }

            rawNewScore == targetScore -> {
                // Potential checkout
                if (config.doubleOut && throwObj.multiplier != Multiplier.DOUBLE) {
                    // Reached target without double - bounce back to target, turn ends
                    val newEngine = copy(
                        currentTurnThrows = currentTurnThrows + throwObj,
                        pendingScore = targetScore,
                        isBounced = true,
                    )
                    newEngine to ThrowResult.BounceBack(targetScore, 0)
                } else {
                    // Checkout!
                    val newEngine = copy(
                        currentTurnThrows = currentTurnThrows + throwObj,
                        pendingScore = targetScore,
                    )
                    newEngine to ThrowResult.Checkout(getCurrentPlayerId())
                }
            }

            else -> {
                // Overshoot - bounce back
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

    private fun addThrowCricket(throwObj: Throw): Pair<GameEngine, ThrowResult> {
        val currentPlayerId = getCurrentPlayerId()
        val cricketState = pendingCricketState ?: session.currentLeg.cricketState ?: CricketState(
            segments = config.cricketSegments ?: CricketSegments.standard(),
        )
        val playerState = cricketState.getPlayerState(currentPlayerId)
        val segment = throwObj.segment
        val marksToAdd = throwObj.multiplier.value

        // Check if this is a target segment
        if (!cricketState.segments.isTarget(segment)) {
            // Not a target segment - just record the throw, no marks
            val newEngine = copy(
                currentTurnThrows = currentTurnThrows + throwObj,
            )
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

        // Calculate points scored (excess marks when we have closed but opponents haven't)
        val pointsScored = calculateCricketPoints(
            segment = segment,
            marksToAdd = marksToAdd,
            currentMarks = currentMarks,
            cricketState = cricketState,
            currentPlayerId = currentPlayerId,
        )

        // Update player state
        val newPlayerState = playerState.copy(
            marks = playerState.marks + (segment to newTotalMarks),
            points = playerState.points + pointsScored,
        )
        val newCricketState = cricketState.withPlayerState(currentPlayerId, newPlayerState)

        // Check for win condition
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

    private fun calculateCricketPoints(
        segment: Int,
        marksToAdd: Int,
        currentMarks: Int,
        cricketState: CricketState,
        currentPlayerId: Uuid,
    ): Int {
        // Can only score if we're closed (or closing with this throw) and at least one opponent isn't
        val opponentsAllClosed = config.playerIds
            .filter { it != currentPlayerId }
            .all { cricketState.getPlayerState(it).isClosed(segment) }

        if (opponentsAllClosed) {
            return 0 // Everyone is closed, no scoring
        }

        // Calculate excess marks that score points
        val totalMarksIfApplied = currentMarks + marksToAdd
        val excessMarks = maxOf(0, totalMarksIfApplied - 3)

        // Segment value times excess marks
        return excessMarks * segment
    }

    private fun checkCricketWin(cricketState: CricketState, playerId: Uuid): Boolean {
        val playerState = cricketState.getPlayerState(playerId)

        // Must have closed all segments
        val allClosed = cricketState.segments.segments.all { playerState.isClosed(it) }
        if (!allClosed) return false

        // Must have >= all opponents' points
        val playerPoints = playerState.points
        val opponentsHaveMorePoints = config.playerIds
            .filter { it != playerId }
            .any { cricketState.getPlayerState(it).points > playerPoints }

        return !opponentsHaveMorePoints
    }

    fun getCricketState(): CricketState? = pendingCricketState ?: session.currentLeg.cricketState

    fun getCricketPlayerState(playerId: Uuid): CricketPlayerState? = getCricketState()?.getPlayerState(playerId)

    fun getCricketPoints(playerId: Uuid): Int = getCricketPlayerState(playerId)?.points ?: 0

    private fun checkBust(newScore: Int, lastThrow: Throw): ThrowResult.Bust? {
        // Score below 0
        if (newScore < 0) {
            return ThrowResult.Bust("Score below zero")
        }

        // Score equals 1 with double-out (impossible to finish)
        if (newScore == 1 && config.doubleOut) {
            return ThrowResult.Bust("Score at 1 with double-out required")
        }

        // Score equals 0 but last throw wasn't a double (with double-out)
        if (newScore == 0 && config.doubleOut && lastThrow.multiplier != Multiplier.DOUBLE) {
            return ThrowResult.Bust("Must finish on a double")
        }

        return null
    }

    fun undoLastThrow(): GameEngine? {
        if (currentTurnThrows.isEmpty()) {
            return null
        }

        // For Cricket, we need to recalculate state from scratch
        if (config.isCricket) {
            return undoLastThrowCricket()
        }

        val newThrows = currentTurnThrows.dropLast(1)
        val newScore = if (newThrows.isEmpty()) {
            getPlayerScore(getCurrentPlayerId())
        } else {
            val originalScore = getPlayerScore(getCurrentPlayerId())
            if (config.isCountUp) {
                originalScore + newThrows.sumOf { it.score }
            } else {
                originalScore - newThrows.sumOf { it.score }
            }
        }

        return copy(
            currentTurnThrows = newThrows,
            pendingScore = if (newThrows.isEmpty()) null else newScore,
            isBusted = false,
            isBounced = false,
        )
    }

    private fun undoLastThrowCricket(): GameEngine? {
        val newThrows = currentTurnThrows.dropLast(1)

        // Recalculate cricket state by replaying remaining throws
        val baseCricketState = session.currentLeg.cricketState ?: CricketState(
            segments = config.cricketSegments ?: CricketSegments.standard(),
        )
        val currentPlayerId = getCurrentPlayerId()

        var cricketState = baseCricketState
        newThrows.forEach { throwObj ->
            cricketState = replayThrowForCricket(cricketState, throwObj, currentPlayerId)
        }

        return copy(
            currentTurnThrows = newThrows,
            pendingCricketState = cricketState,
            isBusted = false,
            isBounced = false,
        )
    }

    private fun replayThrowForCricket(
        cricketState: CricketState,
        throwObj: Throw,
        playerId: Uuid,
    ): CricketState {
        val segment = throwObj.segment
        if (!cricketState.segments.isTarget(segment)) {
            return cricketState
        }

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

    fun endTurn(): Pair<GameEngine, TurnResult> {
        if (config.isCricket) {
            return endTurnCricket()
        }

        val currentPlayerId = getCurrentPlayerId()
        val scoreBeforeTurn = getPlayerScore(currentPlayerId)
        val scoreAfterTurn = if (isBusted) scoreBeforeTurn else (pendingScore ?: scoreBeforeTurn)

        val turn = Turn(
            playerId = currentPlayerId,
            throws = currentTurnThrows,
            scoreBeforeTurn = scoreBeforeTurn,
            scoreAfterTurn = scoreAfterTurn,
            isBust = isBusted,
            isBounce = isBounced,
        )

        val currentLeg = session.currentLeg
        val updatedLeg = currentLeg.copy(turns = currentLeg.turns + turn)

        // Check if leg is won
        val winScore = if (config.isCountUp) config.targetScore else 0
        if (scoreAfterTurn == winScore && !isBusted && !isBounced) {
            return handleLegWon(updatedLeg, currentPlayerId)
        }

        // Knockouts are now handled per-throw in addThrowParcheesi(),
        // so we no longer check for them here.

        // Continue to next player
        val updatedLegs = session.legs.toMutableList()
        updatedLegs[session.currentLegIndex] = updatedLeg

        val newSession = session.copy(legs = updatedLegs)
        val newEngine = GameEngine(newSession)

        return newEngine to TurnResult.NextPlayer(newEngine.getCurrentPlayerId())
    }

    private fun endTurnCricket(): Pair<GameEngine, TurnResult> {
        val currentPlayerId = getCurrentPlayerId()
        val cricketState = pendingCricketState ?: session.currentLeg.cricketState ?: CricketState(
            segments = config.cricketSegments ?: CricketSegments.standard(),
        )
        val playerState = cricketState.getPlayerState(currentPlayerId)

        val turn = Turn(
            playerId = currentPlayerId,
            throws = currentTurnThrows,
            scoreBeforeTurn = 0, // Not used in Cricket
            scoreAfterTurn = playerState.points, // Store points in scoreAfterTurn for display
            isBust = false,
            isBounce = false,
        )

        val currentLeg = session.currentLeg
        val updatedLeg = currentLeg.copy(
            turns = currentLeg.turns + turn,
            cricketState = cricketState,
        )

        // Check if Cricket leg is won
        if (checkCricketWin(cricketState, currentPlayerId)) {
            return handleLegWon(updatedLeg, currentPlayerId)
        }

        // Continue to next player
        val updatedLegs = session.legs.toMutableList()
        updatedLegs[session.currentLegIndex] = updatedLeg

        val newSession = session.copy(legs = updatedLegs)
        val newEngine = GameEngine(newSession)

        return newEngine to TurnResult.NextPlayer(newEngine.getCurrentPlayerId())
    }

    private fun detectKnockouts(newScore: Int, currentPlayerId: Uuid): List<Uuid> {
        // Can't knock out at score 0
        if (newScore == 0) return emptyList()

        return config.playerIds
            .filter { it != currentPlayerId }
            .filter { getPlayerScore(it) == newScore }
    }

    private fun applyKnockoutsToSession(knockedOutPlayerIds: List<Uuid>): GameSession {
        val currentLeg = session.currentLeg
        val updatedLeg = applyKnockouts(currentLeg, knockedOutPlayerIds)
        val updatedLegs = session.legs.toMutableList()
        updatedLegs[session.currentLegIndex] = updatedLeg
        return session.copy(legs = updatedLegs)
    }

    private fun applyKnockouts(leg: Leg, knockedOutPlayerIds: List<Uuid>): Leg {
        // Create phantom turns to reset knocked out players to 0
        val knockoutTurns = knockedOutPlayerIds.map { playerId ->
            val playerScore = getPlayerScore(playerId)
            Turn(
                playerId = playerId,
                throws = emptyList(),
                scoreBeforeTurn = playerScore,
                scoreAfterTurn = 0,
                isBust = false,
                isBounce = false,
                isPhantom = true,
            )
        }
        return leg.copy(turns = leg.turns + knockoutTurns)
    }

    private fun handleLegWon(completedLeg: Leg, winnerId: Uuid): Pair<GameEngine, TurnResult> {
        val legWithWinner = completedLeg.copy(winnerId = winnerId)
        val updatedLegs = session.legs.toMutableList()
        updatedLegs[session.currentLegIndex] = legWithWinner

        val legsWon = updatedLegs.count { it.winnerId == winnerId }

        // Check if match is won
        if (legsWon >= config.legsToWin) {
            val newSession = session.copy(
                legs = updatedLegs,
                status = GameStatus.COMPLETED,
                finishedAt = currentTimeMillis(),
                winnerId = winnerId,
            )
            return GameEngine(newSession) to TurnResult.MatchWon(winnerId)
        }

        // Start new leg
        val newLegs = updatedLegs + Leg()
        val newSession = session.copy(
            legs = newLegs,
            currentLegIndex = session.currentLegIndex + 1,
        )

        return GameEngine(newSession) to TurnResult.LegWon(winnerId, matchContinues = true)
    }

    fun toGameSession(): GameSession = session

    private fun copy(
        session: GameSession = this.session,
        currentTurnThrows: List<Throw> = this.currentTurnThrows,
        pendingScore: Int? = this.pendingScore,
        isBusted: Boolean = this.isBusted,
        isBounced: Boolean = this.isBounced,
        pendingCricketState: CricketState? = this.pendingCricketState,
    ): GameEngine = GameEngine(session, currentTurnThrows, pendingScore, isBusted, isBounced, pendingCricketState)

    fun hasFirstThrowWithDouble(): Boolean {
        if (!config.doubleIn) return true // No double-in requirement

        val currentPlayerId = getCurrentPlayerId()
        val currentLeg = session.currentLeg

        // Check if player has already started (has a non-bust turn)
        val hasStarted = currentLeg.turns.any {
            it.playerId == currentPlayerId && !it.isBust && it.throws.isNotEmpty()
        }

        if (hasStarted) return true

        // Check if current turn has a double
        return currentTurnThrows.any { it.multiplier == Multiplier.DOUBLE }
    }

    fun addThrowWithDoubleInCheck(segment: Int, multiplier: Multiplier): Pair<GameEngine, ThrowResult> {
        // If double-in is required and player hasn't started yet
        if (config.doubleIn && !hasFirstThrowWithDouble()) {
            val throwObj = Throw(segment, multiplier)

            // If this is a double, it counts as starting
            if (multiplier == Multiplier.DOUBLE) {
                return addThrow(segment, multiplier)
            }

            // Otherwise, add the throw but don't count points
            val newEngine = copy(
                currentTurnThrows = currentTurnThrows + throwObj,
            )
            return newEngine to ThrowResult.Success(getPlayerScore(getCurrentPlayerId()))
        }

        return addThrow(segment, multiplier)
    }
}
