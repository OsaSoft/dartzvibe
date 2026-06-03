package cloud.osasoft.dartzvibe.ui.screen.statistics

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.PlayerStatistics
import cloud.osasoft.dartzvibe.data.model.StatisticsFilter
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.domain.statistics.StatisticsCalculator
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class StatisticsScreenState(
    val players: List<Player> = emptyList(),
    val selectedPlayerId: Uuid? = null,
    val selectedMode: GameMode = GameMode.CLASSIC,
    val statistics: PlayerStatistics? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val expandedSections: Set<StatSection> = emptySet(),
) {
    val selectedPlayer: Player?
        get() = players.find { it.id == selectedPlayerId }
}

enum class StatSection {
    GAMES_WON,
    GAMES_180S,
    CHECKOUT_BANDS,
}

@OptIn(ExperimentalUuidApi::class)
class StatisticsScreenModel(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
) : ScreenModel {

    private val log = Logger.withTag("StatisticsScreenModel")
    private val calculator = StatisticsCalculator()

    private val _state = MutableStateFlow(StatisticsScreenState())
    val state: StateFlow<StatisticsScreenState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = combine(
            playerRepository.getAllPlayers(),
            gameRepository.getAllGameSessions(),
        ) { players, games ->
            val currentState = _state.value
            val selectedPlayerId = currentState.selectedPlayerId ?: players.firstOrNull()?.id

            val statistics = if (selectedPlayerId != null) {
                calculator.calculatePlayerStatistics(
                    playerId = selectedPlayerId,
                    games = games,
                    players = players,
                    filter = StatisticsFilter(gameMode = currentState.selectedMode),
                )
            } else {
                null
            }

            StatisticsScreenState(
                players = players,
                selectedPlayerId = selectedPlayerId,
                selectedMode = currentState.selectedMode,
                statistics = statistics,
                isLoading = false,
                expandedSections = currentState.expandedSections,
            )
        }.onEach { newState ->
            log.d { "Statistics loaded for player ${newState.selectedPlayerId} (${newState.selectedMode})" }
            _state.update { newState }
        }.catch { e ->
            log.e(e) { "Error loading statistics" }
            _state.update { it.copy(isLoading = false, error = e.message) }
        }.launchIn(screenModelScope)
    }

    fun selectPlayer(playerId: Uuid) {
        if (playerId == _state.value.selectedPlayerId) return

        _state.update { it.copy(selectedPlayerId = playerId) }
        loadData()
    }

    fun selectMode(mode: GameMode) {
        if (mode == _state.value.selectedMode) return

        _state.update { it.copy(selectedMode = mode) }
        loadData()
    }

    fun toggleSection(section: StatSection) {
        _state.update { state ->
            val newExpanded = if (section in state.expandedSections) {
                state.expandedSections - section
            } else {
                state.expandedSections + section
            }
            state.copy(expandedSections = newExpanded)
        }
    }
}
