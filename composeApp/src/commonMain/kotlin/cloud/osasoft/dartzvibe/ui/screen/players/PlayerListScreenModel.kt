package cloud.osasoft.dartzvibe.ui.screen.players

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
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

data class PlayerListState(
    val players: List<Player> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val playerToDelete: Player? = null,
)

@OptIn(ExperimentalUuidApi::class)
class PlayerListScreenModel(
    private val playerRepository: PlayerRepository,
) : ScreenModel {

    private val log = Logger.withTag("PlayerListScreenModel")

    private val _state = MutableStateFlow(PlayerListState())
    val state: StateFlow<PlayerListState> = _state.asStateFlow()

    init {
        loadPlayers()
    }

    private fun loadPlayers() {
        playerRepository
            .getAllPlayers()
            .onEach { players ->
                log.d { "Loaded ${players.size} players" }
                _state.update { it.copy(players = players, isLoading = false, error = null) }
            }.catch { e ->
                log.e(e) { "Error loading players" }
                _state.update { it.copy(isLoading = false, error = e.message) }
            }.launchIn(screenModelScope)
    }

    fun showDeleteConfirmation(player: Player) {
        _state.update { it.copy(playerToDelete = player) }
    }

    fun dismissDeleteConfirmation() {
        _state.update { it.copy(playerToDelete = null) }
    }

    fun confirmDeletePlayer() {
        val player = _state.value.playerToDelete ?: return
        screenModelScope.launch {
            try {
                log.d { "Deleting player: ${player.name}" }
                playerRepository.deletePlayer(player.id)
                _state.update { it.copy(playerToDelete = null) }
            } catch (e: Exception) {
                log.e(e) { "Error deleting player" }
                _state.update { it.copy(error = e.message, playerToDelete = null) }
            }
        }
    }
}
