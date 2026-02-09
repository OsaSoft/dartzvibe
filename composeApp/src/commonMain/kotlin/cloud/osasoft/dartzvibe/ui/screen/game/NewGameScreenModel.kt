package cloud.osasoft.dartzvibe.ui.screen.game

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.domain.game.CheckoutCalculator
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class NewGameState(
    val availablePlayers: List<Player> = emptyList(),
    val selectedPlayerIds: List<Uuid> = emptyList(),
    val gameType: GameType = GameType.CLASSIC_501,
    val gameMode: GameMode = GameMode.CLASSIC,
    val doubleIn: Boolean = false,
    val doubleOut: Boolean = true,
    val legsToWin: Int = 1,
    val checkoutPracticeRounds: Int = 10,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val createdSession: GameSession? = null,
) {
    val selectedPlayers: List<Player>
        get() = selectedPlayerIds.mapNotNull { id ->
            availablePlayers.find { it.id == id }
        }

    val maxPlayers: Int
        get() = if (gameMode == GameMode.CHECKOUT_PRACTICE) 1 else 4

    val isValid: Boolean
        get() = selectedPlayerIds.size in 1..maxPlayers

    val legsOptions: List<Int> = listOf(1, 3, 5, 7)

    val roundsOptions: List<Int> = listOf(5, 10, 15, 20)

    val isCheckoutPractice: Boolean
        get() = gameMode == GameMode.CHECKOUT_PRACTICE
}

@OptIn(ExperimentalUuidApi::class)
class NewGameScreenModel(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
) : ScreenModel {

    private val log = Logger.withTag("NewGameScreenModel")

    private val _state = MutableStateFlow(NewGameState())
    val state: StateFlow<NewGameState> = _state.asStateFlow()

    init {
        loadPlayers()
    }

    private fun loadPlayers() {
        playerRepository
            .getAllPlayers()
            .onEach { players ->
                log.d { "Loaded ${players.size} players" }
                _state.update { it.copy(availablePlayers = players, isLoading = false, error = null) }
            }.catch { e ->
                log.e(e) { "Error loading players" }
                _state.update { it.copy(isLoading = false, error = e.message) }
            }.launchIn(screenModelScope)
    }

    fun setGameType(gameType: GameType) {
        _state.update { it.copy(gameType = gameType) }
    }

    fun setGameMode(gameMode: GameMode) {
        _state.update { state ->
            val newType = if (state.gameType in gameMode.supportedTypes) {
                state.gameType
            } else {
                gameMode.supportedTypes.first()
            }
            val newPlayerIds = if (gameMode == GameMode.CHECKOUT_PRACTICE && state.selectedPlayerIds.size > 1) {
                state.selectedPlayerIds.take(1)
            } else {
                state.selectedPlayerIds
            }
            state.copy(gameMode = gameMode, gameType = newType, selectedPlayerIds = newPlayerIds)
        }
    }

    fun setDoubleIn(enabled: Boolean) {
        _state.update { it.copy(doubleIn = enabled) }
    }

    fun setDoubleOut(enabled: Boolean) {
        _state.update { it.copy(doubleOut = enabled) }
    }

    fun setLegsToWin(legs: Int) {
        _state.update { it.copy(legsToWin = legs) }
    }

    fun setCheckoutPracticeRounds(rounds: Int) {
        _state.update { it.copy(checkoutPracticeRounds = rounds) }
    }

    fun togglePlayerSelection(playerId: Uuid) {
        _state.update { currentState ->
            val currentSelection = currentState.selectedPlayerIds
            val newSelection = if (playerId in currentSelection) {
                currentSelection - playerId
            } else if (currentSelection.size < currentState.maxPlayers) {
                currentSelection + playerId
            } else {
                currentSelection
            }
            currentState.copy(selectedPlayerIds = newSelection)
        }
    }

    fun movePlayerUp(playerId: Uuid) {
        _state.update { currentState ->
            val currentSelection = currentState.selectedPlayerIds.toMutableList()
            val index = currentSelection.indexOf(playerId)
            if (index > 0) {
                currentSelection.removeAt(index)
                currentSelection.add(index - 1, playerId)
            }
            currentState.copy(selectedPlayerIds = currentSelection)
        }
    }

    fun movePlayerDown(playerId: Uuid) {
        _state.update { currentState ->
            val currentSelection = currentState.selectedPlayerIds.toMutableList()
            val index = currentSelection.indexOf(playerId)
            if (index >= 0 && index < currentSelection.size - 1) {
                currentSelection.removeAt(index)
                currentSelection.add(index + 1, playerId)
            }
            currentState.copy(selectedPlayerIds = currentSelection)
        }
    }

    fun startGame() {
        val currentState = _state.value
        if (!currentState.isValid) {
            _state.update { it.copy(error = "Please select at least 1 player") }
            return
        }

        _state.update { it.copy(isSaving = true, error = null) }
        screenModelScope.launch {
            try {
                val isCheckout = currentState.gameMode == GameMode.CHECKOUT_PRACTICE
                val isCricket = currentState.gameMode == GameMode.CRICKET

                val cricketSegments = when (currentState.gameType) {
                    GameType.CRICKET_REGULAR -> CricketSegments.standard()
                    GameType.CRICKET_RANDOM -> CricketSegments.random()
                    else -> null
                }

                val checkoutTargets = if (isCheckout) {
                    val range = CheckoutCalculator.getScoreRange(currentState.gameType)
                    CheckoutCalculator.generateCheckoutTargets(
                        count = currentState.checkoutPracticeRounds,
                        range = range,
                        doubleOut = true,
                    )
                } else {
                    null
                }

                val config = GameConfig(
                    gameType = currentState.gameType,
                    gameMode = currentState.gameMode,
                    doubleIn = if (isCricket || isCheckout) false else currentState.doubleIn,
                    doubleOut = if (isCricket) {
                        false
                    } else if (isCheckout) {
                        true
                    } else {
                        currentState.doubleOut
                    },
                    playerIds = currentState.selectedPlayerIds,
                    legsToWin = if (isCheckout) 1 else currentState.legsToWin,
                    cricketSegments = cricketSegments,
                    checkoutPracticeTargets = checkoutTargets,
                )
                val session = gameRepository.createGameSession(config)
                log.d { "Created game session: ${session.id}" }
                _state.update { it.copy(isSaving = false, createdSession = session) }
            } catch (e: Exception) {
                log.e(e) { "Error creating game session" }
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
