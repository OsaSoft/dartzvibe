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
) {
    // False while loading and after a load error: the counts are then unknown, not zero
    val countsKnown: Boolean
        get() = !isLoading && error == null

    // Null while the counts are unknown, so Home shows no number (MenuButton keeps the line's space)
    val playersSubtitle: String?
        get() = if (countsKnown) "$playerCount player${if (playerCount != 1) "s" else ""}" else null

    val gamesSubtitle: String?
        get() = if (countsKnown && completedGameCount > 0) "$completedGameCount completed" else null

    // While loading, these buttons look enabled (taps are ignored) so they don't flicker grey on every launch;
    // only a loaded zero, or a load error, draws them disabled.
    val statisticsEnabled: Boolean
        get() = isLoading || completedGameCount > 0

    val headToHeadEnabled: Boolean
        get() = isLoading || (completedGameCount > 0 && playerCount >= 2)

    val leaderboardEnabled: Boolean
        get() = isLoading || completedGameCount > 0
}

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
            _state.update { newState }
        }.catch { e ->
            log.e(e) { "Error loading data" }
            _state.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load players and games") }
        }.launchIn(screenModelScope)
    }
}
