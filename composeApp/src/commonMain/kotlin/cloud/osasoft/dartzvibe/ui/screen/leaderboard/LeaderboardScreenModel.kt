package cloud.osasoft.dartzvibe.ui.screen.leaderboard

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.FixedDecimal
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

/**
 * Leaderboard ranking axis. Rankings are always scoped to a single [GameMode]; there is no
 * honest cross-mode ranking, so [PRIMARY] ranks on that mode's primary metric (MPR for
 * Cricket, 3-dart avg for Classic, success rate for Checkout, …).
 */
enum class LeaderboardSortMetric(val displayName: String) {
    PRIMARY("Top metric"),
    WIN_RATE("Win Rate"),
    GAMES_WON("Games Won"),
}

@OptIn(ExperimentalUuidApi::class)
data class LeaderboardEntry(
    val player: Player,
    val statistics: PlayerStatistics,
    val rank: Int,
)

@OptIn(ExperimentalUuidApi::class)
data class LeaderboardScreenState(
    val entries: List<LeaderboardEntry> = emptyList(),
    val allPlayerStats: Map<Player, PlayerStatistics> = emptyMap(),
    val selectedMode: GameMode = GameMode.CLASSIC,
    val sortMetric: LeaderboardSortMetric = LeaderboardSortMetric.PRIMARY,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalUuidApi::class)
class LeaderboardScreenModel(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
) : ScreenModel {

    private val log = Logger.withTag("LeaderboardScreenModel")
    private val calculator = StatisticsCalculator()

    private val _state = MutableStateFlow(LeaderboardScreenState())
    val state: StateFlow<LeaderboardScreenState> = _state.asStateFlow()

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
            val mode = _state.value.selectedMode
            val playerStats = players.associateWith { player ->
                calculator.calculatePlayerStatistics(
                    playerId = player.id,
                    games = games,
                    players = players,
                    filter = StatisticsFilter(gameMode = mode),
                )
            }
            playerStats to sortAndRank(playerStats, _state.value.sortMetric)
        }.onEach { (playerStats, entries) ->
            log.d { "Calculated stats for ${entries.size} players" }
            _state.update {
                it.copy(
                    allPlayerStats = playerStats,
                    entries = entries,
                    isLoading = false,
                )
            }
        }.catch { e ->
            log.e(e) { "Error loading leaderboard data" }
            _state.update { it.copy(isLoading = false, error = e.message) }
        }.launchIn(screenModelScope)
    }

    fun selectMode(mode: GameMode) {
        if (mode == _state.value.selectedMode) return

        _state.update { it.copy(selectedMode = mode) }
        loadData()
    }

    fun selectSortMetric(metric: LeaderboardSortMetric) {
        if (metric == _state.value.sortMetric) return

        val currentState = _state.value
        _state.update {
            it.copy(
                sortMetric = metric,
                entries = sortAndRank(currentState.allPlayerStats, metric),
            )
        }
    }

    private fun sortAndRank(
        playerStats: Map<Player, PlayerStatistics>,
        metric: LeaderboardSortMetric,
    ): List<LeaderboardEntry> {
        val playersWithGames = playerStats.filter { it.value.gamesPlayed > 0 }

        val sorted = when (metric) {
            LeaderboardSortMetric.PRIMARY ->
                playersWithGames.entries.sortedByDescending {
                    it.value.modeStats?.primaryMetric ?: FixedDecimal.ZERO
                }

            LeaderboardSortMetric.WIN_RATE ->
                playersWithGames.entries.sortedByDescending { it.value.winRate }

            LeaderboardSortMetric.GAMES_WON ->
                playersWithGames.entries.sortedByDescending { it.value.gamesWon }
        }

        return sorted.mapIndexed { index, entry ->
            LeaderboardEntry(
                player = entry.key,
                statistics = entry.value,
                rank = index + 1,
            )
        }
    }
}
