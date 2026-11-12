package cloud.osasoft.dartzvibe.ui.screen.game

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.CheckoutPracticeState
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.RouletteState
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.domain.game.CheckoutCalculator
import cloud.osasoft.dartzvibe.domain.game.CheckoutPath
import cloud.osasoft.dartzvibe.domain.game.GameEngine
import cloud.osasoft.dartzvibe.domain.game.GameEngineHelper
import cloud.osasoft.dartzvibe.domain.game.RewindResult
import cloud.osasoft.dartzvibe.domain.game.RouletteRules
import cloud.osasoft.dartzvibe.domain.game.ThrowResult
import cloud.osasoft.dartzvibe.domain.game.TurnResult
import cloud.osasoft.dartzvibe.domain.game.TurnRewind
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class ActiveGameState(
    val session: GameSession? = null,
    val players: Map<Uuid, Player> = emptyMap(),
    val engine: GameEngine? = null,
    val selectedMultiplier: Multiplier = Multiplier.SINGLE,
    val lastThrowResult: ThrowResult? = null,
    val lastTurnResult: TurnResult? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showGameCompleteDialog: Boolean = false,
    val showLegWonDialog: Boolean = false,
    val showKnockoutDialog: Boolean = false,
    val knockedOutPlayerIds: List<Uuid> = emptyList(),
    /**
     * The engine after the winning `endTurn` (next leg, or the finished match) while a win dialog shows. [engine]
     * stays the one before it, so the screen behind the dialog shows the leg as it ended.
     */
    val pendingWinEngine: GameEngine? = null,
) {
    val currentPlayerId: Uuid?
        get() = engine?.getCurrentPlayerId()

    val currentPlayerScore: Int
        get() = engine?.getCurrentPlayerScore() ?: 0

    val currentTurnThrows: List<Throw>
        get() = engine?.getCurrentTurnThrows() ?: emptyList()

    val currentRoundNumber: Int
        get() = session?.let { GameEngineHelper.getRoundNumber(it) } ?: 1

    val throwsRemaining: Int
        get() = 3 - currentTurnThrows.size

    val isBusted: Boolean
        get() = lastThrowResult is ThrowResult.Bust

    val isBounced: Boolean
        get() = lastThrowResult is ThrowResult.BounceBack

    /** A reopened winning turn (after a dialog Undo) takes no more darts: End Turn wins again, Undo removes it. */
    val isWinningDartThrown: Boolean
        get() = lastThrowResult is ThrowResult.Checkout || lastThrowResult is ThrowResult.CricketWin

    val isTurnEnded: Boolean
        get() = isBusted || isBounced || isWinningDartThrown

    val canThrow: Boolean
        get() = throwsRemaining > 0 && !isTurnEnded

    /** Darts in the current turn, or an earlier turn in this leg to go back to. Never into a finished leg. */
    // Lazy: a rewind check replays the leg, so compute it at most once per state
    val canUndo: Boolean by lazy { engine?.let { TurnRewind.canUndo(it) } == true }

    /** Whether the win dialogs offer Undo: false when the stored leg can't be replayed. */
    val canUndoWin: Boolean by lazy { pendingWinEngine?.let { TurnRewind.canUndoWin(it) } == true }

    /** A win dialog is up, or its win is still pending: darts, End Turn and the normal Undo do nothing. */
    val isWinDialogUp: Boolean
        get() = pendingWinEngine != null || showLegWonDialog || showGameCompleteDialog

    /** Continue swapped the next leg in under the Leg Won dialog; the UI closes it once that leg is drawn. */
    val isLegWonDialogClosing: Boolean
        get() = showLegWonDialog && pendingWinEngine == null

    /** The state after the win, for the results dialogs (final scores, round results); this state otherwise. */
    val resultState: ActiveGameState by lazy {
        pendingWinEngine?.let { copy(engine = it, session = it.toGameSession(), pendingWinEngine = null) } ?: this
    }

    val isGameComplete: Boolean
        get() = session?.status == GameStatus.COMPLETED

    val isCountUp: Boolean
        get() = session?.config?.isCountUp == true

    val isCricket: Boolean
        get() = session?.config?.isCricket == true

    val targetScore: Int
        get() = session?.config?.targetScore ?: 501

    val cricketState: CricketState?
        get() = engine?.getCricketState()

    val isCheckoutPractice: Boolean
        get() = session?.config?.isCheckoutPractice == true

    val checkoutPracticeState: CheckoutPracticeState?
        get() = engine?.getCheckoutPracticeState()

    val currentCheckoutTarget: Int?
        get() = checkoutPracticeState?.let { state ->
            if (state.isComplete) null else state.currentTarget
        }

    val checkoutRoundNumber: Int
        get() = (checkoutPracticeState?.currentRoundIndex ?: 0) + 1

    val checkoutTotalRounds: Int
        get() = checkoutPracticeState?.totalRounds ?: 0

    val isRoulette: Boolean
        get() = session?.config?.isRoulette == true

    val rouletteState: RouletteState?
        get() = engine?.getRouletteState()

    /**
     * Current Roulette target, or null outside Roulette. Mirrors `RouletteModeEngine`: a missing or empty
     * target list falls back to 1 and a missing state to the first round, so the input pad always renders.
     */
    val currentRouletteTarget: Int?
        get() {
            if (!isRoulette) return null
            val roundIndex = rouletteState?.currentRoundIndex ?: 0
            return RouletteRules.targetFor(session?.config?.rouletteTargetSegments, roundIndex)
        }

    /**
     * Points the current player has scored so far this turn in Roulette, or null outside Roulette.
     * Taken from the engine (pending score minus committed score), so it always matches the score cards.
     */
    val rouletteTurnPoints: Int?
        get() {
            if (!isRoulette) return null
            val playerId = currentPlayerId ?: return 0
            return currentPlayerScore - getPlayerScore(playerId)
        }

    val rouletteRoundNumber: Int
        get() = (rouletteState?.currentRoundIndex ?: 0) + 1

    val rouletteTotalRounds: Int?
        get() = session?.config?.rouletteRounds

    val rouletteTargetScore: Int?
        get() = session?.config?.rouletteTargetScore

    /** Whether the mode can have a checkout, so the screen reserves the checkout slot (Classic, Parcheesi, Practice). */
    val hasCheckoutSlot: Boolean
        get() = when (session?.config?.gameMode) {
            GameMode.CLASSIC, GameMode.PARCHEESI, GameMode.CHECKOUT_PRACTICE -> true
            GameMode.CRICKET, GameMode.ROULETTE, null -> false
        }

    val checkoutOptions: List<CheckoutPath>?
        get() {
            // No checkout hints for Cricket or Roulette, nor once the turn has ended: after a bust or bounce the
            // score is back at the turn start (or the bounced score), which this turn can no longer finish
            if (!hasCheckoutSlot || isTurnEnded) return null

            val doubleOut = session?.config?.doubleOut ?: true
            // For Parcheesi, calculate remaining to target
            val remaining = if (isCountUp) {
                targetScore - currentPlayerScore
            } else {
                currentPlayerScore
            }
            return if (remaining in 2..170) {
                CheckoutCalculator.getCheckoutOptions(remaining, doubleOut, maxDarts = throwsRemaining)
            } else {
                null
            }
        }

    fun getPlayerScore(playerId: Uuid): Int = engine?.getPlayerScore(playerId) ?: session?.config?.startingScore ?: 0

    fun getLegsWon(playerId: Uuid): Int = engine?.getLegsWon(playerId) ?: 0

    fun getPlayer(playerId: Uuid): Player? = players[playerId]

    fun getLastTurnScore(playerId: Uuid): Int? {
        val currentLeg = session?.currentLeg ?: return null
        val lastTurn = currentLeg.turns.lastOrNull { it.playerId == playerId }
        return if (lastTurn != null && !lastTurn.isBust) {
            if (isRoulette) {
                lastTurn.scoreAfterTurn - lastTurn.scoreBeforeTurn
            } else {
                lastTurn.totalScore
            }
        } else {
            null
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
class ActiveGameScreenModel(
    private val gameRepository: GameRepository,
    private val playerRepository: PlayerRepository,
    private val sessionId: Uuid,
) : ScreenModel {

    private val log = Logger.withTag("ActiveGameScreenModel")

    private val _state = MutableStateFlow(ActiveGameState())
    val state: StateFlow<ActiveGameState> = _state.asStateFlow()

    // Declared before init: loadGame() may run synchronously on an immediate dispatcher
    private val saveMutex = Mutex()
    private var pendingSave: GameSession? = null
    private var lastSaveRequested: GameSession? = null

    init {
        loadGame()
    }

    private fun loadGame() {
        screenModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                // Load game session
                val session = gameRepository.getGameSessionById(sessionId).first()
                if (session == null) {
                    _state.update { it.copy(isLoading = false, error = "Game not found") }
                    return@launch
                }

                // Load players
                val allPlayers = playerRepository.getAllPlayers().first()
                val playersMap = allPlayers.associateBy { it.id }

                // Create game engine
                val engine = GameEngine.fromSession(session)
                lastSaveRequested = session

                _state.update {
                    it.copy(
                        session = session,
                        players = playersMap,
                        engine = engine,
                        isLoading = false,
                    )
                }

                log.d { "Loaded game session: ${session.id}" }
            } catch (e: Exception) {
                log.e(e) { "Error loading game" }
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setMultiplier(multiplier: Multiplier) {
        _state.update { it.copy(selectedMultiplier = multiplier) }
    }

    fun onMiss() {
        submitThrow(0, Multiplier.SINGLE)
    }

    fun onScoreSelect(segment: Int, multiplier: Multiplier) {
        // Validate: no triple bull
        if (segment == 25 && multiplier == Multiplier.TRIPLE) {
            return
        }
        submitThrow(segment, multiplier)
    }

    private fun submitThrow(segment: Int, multiplier: Multiplier) {
        // The keypad is hidden then; also guards a reopened winning turn against a late tap
        if (!_state.value.canThrow || _state.value.isWinDialogUp) return
        val engine = _state.value.engine ?: return

        val (newEngine, result) = engine.addThrow(segment, multiplier)

        // Check for knockout in throw result (Parcheesi mode)
        val knockedOutIds = if (result is ThrowResult.SuccessWithKnockout) {
            result.knockedOutPlayerIds
        } else {
            emptyList()
        }

        _state.update {
            it.copy(
                engine = newEngine,
                session = newEngine.toGameSession(),
                selectedMultiplier = Multiplier.SINGLE,
                lastThrowResult = result,
                showKnockoutDialog = knockedOutIds.isNotEmpty(),
                knockedOutPlayerIds = knockedOutIds,
            )
        }

        // Auto-end turn on checkout (leg won) or Cricket win
        when (result) {
            is ThrowResult.Checkout -> endTurn()
            is ThrowResult.CricketWin -> endTurn()
            else -> {}
        }
    }

    /**
     * Removes the last dart or, with no darts thrown, reopens the previous player's turn in this leg
     * with all their darts. Never crosses into a finished leg.
     */
    fun undo() {
        if (_state.value.isWinDialogUp) return
        val engine = _state.value.engine ?: return
        val rewind = TurnRewind.undo(engine) ?: return
        applyRewind(rewind)
    }

    /**
     * Undo from the leg-won / game-over dialogs: closes the dialog, reverts the leg (and match) win and reopens
     * the winning turn with its last dart in place.
     */
    fun undoWin() {
        val current = _state.value
        // Only while the win is pending: not after a second tap, nor once Continue swapped in the next leg
        if (current.pendingWinEngine == null || !current.canUndoWin) return
        val engine = current.engine ?: return
        // The screen already shows the engine before the winning End Turn, which is what rewinding the win
        // produces (TurnRewindTest): keep it, so nothing moves when the dialog closes
        _state.update {
            it.copy(
                lastTurnResult = null,
                showGameCompleteDialog = false,
                showLegWonDialog = false,
                pendingWinEngine = null,
            )
        }
        // In-progress darts are never saved: persist the state at the start of the reopened turn
        val turnStart = TurnRewind.turnStartSession(engine.toGameSession())
        if (turnStart != lastSaveRequested) saveGame(turnStart)
    }

    private fun applyRewind(rewind: RewindResult) {
        val newEngine = rewind.engine
        _state.update {
            it.copy(
                engine = newEngine,
                session = newEngine.toGameSession(),
                selectedMultiplier = Multiplier.SINGLE,
                lastThrowResult = rewind.lastThrowResult,
                lastTurnResult = null,
                showGameCompleteDialog = false,
                showLegWonDialog = false,
                showKnockoutDialog = false,
                knockedOutPlayerIds = emptyList(),
                pendingWinEngine = null,
            )
        }
        // In-progress darts are never saved: persist the state at the start of the reopened turn
        val turnStart = TurnRewind.turnStartSession(newEngine.toGameSession())
        if (turnStart != lastSaveRequested) saveGame(turnStart)
    }

    fun endTurn() {
        if (_state.value.isWinDialogUp) return
        val engine = _state.value.engine ?: return

        val (newEngine, result) = engine.endTurn()

        val knockedOutIds = if (result is TurnResult.NextPlayerWithKnockout) {
            result.knockedOutPlayerIds
        } else {
            emptyList()
        }
        val isWin = result is TurnResult.LegWon || result is TurnResult.MatchWon

        _state.update {
            if (isWin) {
                // Freeze the screen on the leg as it ended behind the dialog; Continue swaps in the next leg
                it.copy(
                    pendingWinEngine = newEngine,
                    lastTurnResult = result,
                    showGameCompleteDialog = result is TurnResult.MatchWon,
                    showLegWonDialog = result is TurnResult.LegWon,
                    selectedMultiplier = Multiplier.SINGLE,
                )
            } else {
                it.copy(
                    engine = newEngine,
                    session = newEngine.toGameSession(),
                    lastTurnResult = result,
                    showKnockoutDialog = knockedOutIds.isNotEmpty(),
                    knockedOutPlayerIds = knockedOutIds,
                    selectedMultiplier = Multiplier.SINGLE,
                    lastThrowResult = null,
                )
            }
        }

        // Save game state
        saveGame(newEngine.toGameSession())
    }

    /**
     * Saves are serialized and latest-wins: each launch writes whatever session is newest when it gets the lock,
     * so a slow earlier save (e.g. the COMPLETED one before a dialog Undo) can never land after a newer one.
     */
    private fun saveGame(session: GameSession) {
        pendingSave = session
        lastSaveRequested = session
        screenModelScope.launch {
            saveMutex.withLock {
                val toSave = pendingSave ?: return@withLock
                pendingSave = null
                try {
                    _state.update { it.copy(isSaving = true) }
                    gameRepository.updateGameSession(toSave)
                    _state.update { it.copy(isSaving = false) }
                    log.d { "Saved game session: ${toSave.id}" }
                } catch (e: Exception) {
                    log.e(e) { "Error saving game" }
                    _state.update { it.copy(isSaving = false, error = e.message) }
                }
            }
        }
    }

    /**
     * Continue, step 1: swaps the next leg in while the Leg Won dialog stays up, so the ended leg is never seen
     * without the dialog. The UI calls [closeLegWonDialog] once a frame of the new leg is drawn. A repeated tap
     * does nothing.
     */
    fun continueToNextLeg() {
        _state.update {
            if (!it.showLegWonDialog) return@update it
            val next = it.pendingWinEngine ?: return@update it
            it.copy(
                engine = next,
                session = next.toGameSession(),
                lastThrowResult = null,
                pendingWinEngine = null,
            )
        }
    }

    /** Continue, step 2: closes the Leg Won dialog over the next leg; nothing while the win is still pending. */
    fun closeLegWonDialog() {
        _state.update {
            if (!it.isLegWonDialogClosing) return@update it
            it.copy(showLegWonDialog = false, lastTurnResult = null)
        }
    }

    // Finish has no ScreenModel call: the screen pops with the Game Over dialog still up, so the ended game is
    // never seen without it

    fun dismissKnockoutDialog() {
        _state.update { it.copy(showKnockoutDialog = false, knockedOutPlayerIds = emptyList()) }
    }

    fun abandonGame() {
        val session = _state.value.engine?.toGameSession() ?: return
        // Without the in-progress turn, so a knockout phantom it caused isn't saved without its causing turn
        val abandonedSession = TurnRewind.turnStartSession(session).copy(status = GameStatus.ABANDONED)
        _state.update { it.copy(session = abandonedSession) }
        saveGame(abandonedSession)
    }
}
