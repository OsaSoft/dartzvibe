package cloud.osasoft.dartzvibe.ui.screen.statistics

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.HeadToHeadStatistics
import cloud.osasoft.dartzvibe.data.model.Player
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
data class HeadToHeadScreenState(
    val players: List<Player> = emptyList(),
    val allGames: List<GameSession> = emptyList(),
    val selectedPlayer1: Player? = null,
    val selectedPlayer2: Player? = null,
    val selectedGameType: GameType? = null,
    val statistics: HeadToHeadStatistics? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalUuidApi::class)
class HeadToHeadScreenModel(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
) : ScreenModel {

    private val log = Logger.withTag("HeadToHeadScreenModel")
    private val calculator = StatisticsCalculator()

    private val _state = MutableStateFlow(HeadToHeadScreenState())
    val state: StateFlow<HeadToHeadScreenState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            playerRepository.getAllPlayers(),
            gameRepository.getAllGameSessions(),
        ) { players, games ->
            val sortedPlayers = players.sortedBy { it.name }

            HeadToHeadScreenState(
                players = sortedPlayers,
                allGames = games,
                selectedPlayer1 = sortedPlayers.getOrNull(0),
                selectedPlayer2 = sortedPlayers.getOrNull(1),
                selectedGameType = null,
                isLoading = false,
            )
        }.onEach { newState ->
            log.d { "Loaded ${newState.players.size} players" }
            _state.update { newState }
            calculateStatistics()
        }.catch { e ->
            log.e(e) { "Error loading data" }
            _state.update { it.copy(isLoading = false, error = e.message) }
        }.launchIn(screenModelScope)
    }

    fun selectPlayer1(playerId: Uuid) {
        val player = _state.value.players.find { it.id == playerId }
        if (player != null && player.id != _state.value.selectedPlayer2?.id) {
            _state.update { it.copy(selectedPlayer1 = player) }
            calculateStatistics()
        }
    }

    fun selectPlayer2(playerId: Uuid) {
        val player = _state.value.players.find { it.id == playerId }
        if (player != null && player.id != _state.value.selectedPlayer1?.id) {
            _state.update { it.copy(selectedPlayer2 = player) }
            calculateStatistics()
        }
    }

    fun swapPlayers() {
        val currentState = _state.value
        _state.update {
            it.copy(
                selectedPlayer1 = currentState.selectedPlayer2,
                selectedPlayer2 = currentState.selectedPlayer1,
            )
        }
        calculateStatistics()
    }

    fun selectGameType(gameType: GameType?) {
        _state.update { it.copy(selectedGameType = gameType) }
        calculateStatistics()
    }

    private fun calculateStatistics() {
        val currentState = _state.value
        val player1 = currentState.selectedPlayer1
        val player2 = currentState.selectedPlayer2

        if (player1 == null || player2 == null) {
            _state.update { it.copy(statistics = null) }
            return
        }

        val stats = calculator.calculateHeadToHeadStatistics(
            player1Id = player1.id,
            player2Id = player2.id,
            games = currentState.allGames,
            gameTypeFilter = currentState.selectedGameType,
        )

        _state.update { it.copy(statistics = stats) }
    }
}
