package cloud.osasoft.dartzvibe.ui.screen.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
data class HomeScreenState(
    val playerCount: Int = 0,
    val completedGameCount: Int = 0,
    val inProgressGame: GameSession? = null,
    val inProgressGamePlayers: List<Player> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalUuidApi::class)
class HomeScreenModel(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
) : ScreenModel {

    private val log = Logger.withTag("HomeScreenModel")

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            playerRepository.getAllPlayers(),
            gameRepository.getAllGameSessions(),
        ) { players, games ->
            val inProgressGame = games.find { it.status == GameStatus.IN_PROGRESS }
            val inProgressPlayers = inProgressGame?.config?.playerIds?.mapNotNull { playerId ->
                players.find { it.id == playerId }
            } ?: emptyList()

            HomeScreenState(
                playerCount = players.size,
                completedGameCount = games.count { it.status == GameStatus.COMPLETED },
                inProgressGame = inProgressGame,
                inProgressGamePlayers = inProgressPlayers,
                isLoading = false,
            )
        }.onEach { newState ->
            log.d { "Loaded: ${newState.playerCount} players, ${newState.completedGameCount} completed games" }
            _state.value = newState
        }.catch { e ->
            log.e(e) { "Error loading data" }
            _state.update { it.copy(isLoading = false, error = e.message) }
        }.launchIn(screenModelScope)
    }
}
