package cloud.osasoft.dartzvibe.ui.screen.game

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.domain.game.GameEngine
import cloud.osasoft.dartzvibe.domain.game.ThrowResult
import cloud.osasoft.dartzvibe.domain.game.TurnResult
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
) {
    val currentPlayerId: Uuid?
        get() = engine?.getCurrentPlayerId()

    val currentPlayerScore: Int
        get() = engine?.getCurrentPlayerScore() ?: 0

    val currentTurnThrows: List<Throw>
        get() = engine?.getCurrentTurnThrows() ?: emptyList()

    val throwsRemaining: Int
        get() = 3 - currentTurnThrows.size

    val isBusted: Boolean
        get() = lastThrowResult is ThrowResult.Bust

    val canThrow: Boolean
        get() = throwsRemaining > 0 && !isBusted

    val canUndo: Boolean
        get() = currentTurnThrows.isNotEmpty()

    val isGameComplete: Boolean
        get() = session?.status == GameStatus.COMPLETED

    fun getPlayerScore(playerId: Uuid): Int = engine?.getPlayerScore(playerId) ?: session?.config?.startingScore ?: 0

    fun getLegsWon(playerId: Uuid): Int = engine?.getLegsWon(playerId) ?: 0

    fun getPlayer(playerId: Uuid): Player? = players[playerId]

    fun getLastTurnScore(playerId: Uuid): Int? {
        val currentLeg = session?.currentLeg ?: return null
        val lastTurn = currentLeg.turns.lastOrNull { it.playerId == playerId }
        return if (lastTurn != null && !lastTurn.isBust) {
            lastTurn.totalScore
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
        val engine = _state.value.engine ?: return

        val (newEngine, result) = engine.addThrow(segment, multiplier)

        _state.update {
            it.copy(
                engine = newEngine,
                selectedMultiplier = Multiplier.SINGLE,
                lastThrowResult = result,
            )
        }

        // Only auto-end turn on checkout (leg won)
        if (result is ThrowResult.Checkout) {
            endTurn()
        }
    }

    fun undoLastThrow() {
        val engine = _state.value.engine ?: return
        val newEngine = engine.undoLastThrow() ?: return

        _state.update {
            it.copy(
                engine = newEngine,
                lastThrowResult = null,
            )
        }
    }

    fun endTurn() {
        val engine = _state.value.engine ?: return

        val (newEngine, result) = engine.endTurn()

        _state.update {
            it.copy(
                engine = newEngine,
                session = newEngine.toGameSession(),
                lastTurnResult = result,
                showGameCompleteDialog = result is TurnResult.MatchWon,
                showLegWonDialog = result is TurnResult.LegWon,
                selectedMultiplier = Multiplier.SINGLE,
                lastThrowResult = null,
            )
        }

        // Save game state
        saveGame()
    }

    private fun saveGame() {
        val session = _state.value.engine?.toGameSession() ?: return

        screenModelScope.launch {
            try {
                _state.update { it.copy(isSaving = true) }
                gameRepository.updateGameSession(session)
                _state.update { it.copy(isSaving = false, session = session) }
                log.d { "Saved game session: ${session.id}" }
            } catch (e: Exception) {
                log.e(e) { "Error saving game" }
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun dismissLegWonDialog() {
        _state.update { it.copy(showLegWonDialog = false, lastTurnResult = null) }
    }

    fun dismissGameCompleteDialog() {
        _state.update { it.copy(showGameCompleteDialog = false, lastTurnResult = null) }
    }

    fun abandonGame() {
        val session = _state.value.session ?: return

        screenModelScope.launch {
            try {
                val abandonedSession = session.copy(status = GameStatus.ABANDONED)
                gameRepository.updateGameSession(abandonedSession)
                _state.update { it.copy(session = abandonedSession) }
            } catch (e: Exception) {
                log.e(e) { "Error abandoning game" }
            }
        }
    }
}
