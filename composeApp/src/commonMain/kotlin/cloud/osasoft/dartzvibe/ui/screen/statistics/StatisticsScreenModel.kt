package cloud.osasoft.dartzvibe.ui.screen.statistics

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.PlayerStatistics
import cloud.osasoft.dartzvibe.data.model.StatisticsFilter
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.domain.statistics.StatisticsCalculator
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
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class StatisticsScreenState(
    val players: List<Player> = emptyList(),
    val selectedPlayerId: Uuid? = null,
    val selectedGameType: GameType? = null,
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

    init {
        loadData()
    }

    private fun loadData() {
        combine(
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
                    filter = StatisticsFilter(gameType = currentState.selectedGameType),
                )
            } else {
                null
            }

            StatisticsScreenState(
                players = players,
                selectedPlayerId = selectedPlayerId,
                selectedGameType = currentState.selectedGameType,
                statistics = statistics,
                isLoading = false,
                expandedSections = currentState.expandedSections,
            )
        }.onEach { newState ->
            log.d { "Statistics loaded for player ${newState.selectedPlayerId}" }
            _state.value = newState
        }.catch { e ->
            log.e(e) { "Error loading statistics" }
            _state.update { it.copy(isLoading = false, error = e.message) }
        }.launchIn(screenModelScope)
    }

    fun selectPlayer(playerId: Uuid) {
        if (playerId == _state.value.selectedPlayerId) return

        _state.update { it.copy(selectedPlayerId = playerId) }
        // Trigger recalculation
        loadData()
    }

    fun selectGameType(gameType: GameType?) {
        if (gameType == _state.value.selectedGameType) return

        _state.update { it.copy(selectedGameType = gameType) }
        // Trigger recalculation
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
