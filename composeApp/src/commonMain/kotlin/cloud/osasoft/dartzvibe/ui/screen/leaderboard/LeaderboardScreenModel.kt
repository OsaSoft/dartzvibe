package cloud.osasoft.dartzvibe.ui.screen.leaderboard

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.PlayerStatistics
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

enum class LeaderboardSortMetric(val displayName: String) {
    WIN_RATE("Win Rate"),
    THREE_DART_AVERAGE("3-Dart Avg"),
    GAMES_WON("Games Won"),
    COUNT_180S("180s"),
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
    val sortMetric: LeaderboardSortMetric = LeaderboardSortMetric.WIN_RATE,
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

    private var allPlayerStats: Map<Player, PlayerStatistics> = emptyMap()

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            playerRepository.getAllPlayers(),
            gameRepository.getAllGameSessions(),
        ) { players, games ->
            allPlayerStats = players.associateWith { player ->
                calculator.calculatePlayerStatistics(
                    playerId = player.id,
                    games = games,
                    players = players,
                )
            }
            sortAndRank(_state.value.sortMetric)
        }.onEach { entries ->
            log.d { "Calculated stats for ${entries.size} players" }
            _state.update { it.copy(entries = entries, isLoading = false) }
        }.catch { e ->
            log.e(e) { "Error loading leaderboard data" }
            _state.update { it.copy(isLoading = false, error = e.message) }
        }.launchIn(screenModelScope)
    }

    fun selectSortMetric(metric: LeaderboardSortMetric) {
        if (metric == _state.value.sortMetric) return

        val sortedEntries = sortAndRank(metric)
        _state.update {
            it.copy(
                sortMetric = metric,
                entries = sortedEntries,
            )
        }
    }

    private fun sortAndRank(metric: LeaderboardSortMetric): List<LeaderboardEntry> {
        val playersWithGames = allPlayerStats.filter { it.value.gamesPlayed > 0 }

        val sorted = when (metric) {
            LeaderboardSortMetric.WIN_RATE -> {
                playersWithGames.entries.sortedByDescending { it.value.winRate }
            }

            LeaderboardSortMetric.THREE_DART_AVERAGE -> {
                playersWithGames.entries.sortedByDescending { it.value.threeDartAverage }
            }

            LeaderboardSortMetric.GAMES_WON -> {
                playersWithGames.entries.sortedByDescending { it.value.gamesWon }
            }

            LeaderboardSortMetric.COUNT_180S -> {
                playersWithGames.entries.sortedByDescending { it.value.count180s }
            }
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
