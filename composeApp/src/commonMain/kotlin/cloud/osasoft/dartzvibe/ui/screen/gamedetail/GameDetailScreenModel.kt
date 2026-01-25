package cloud.osasoft.dartzvibe.ui.screen.gamedetail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.GameSession
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
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class GameDetailState(
    val session: GameSession? = null,
    val players: Map<Uuid, Player> = emptyMap(),
    val expandedLegs: Set<Int> = setOf(0),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalUuidApi::class)
class GameDetailScreenModel(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
    private val gameId: Uuid,
) : ScreenModel {

    private val log = Logger.withTag("GameDetailScreenModel")

    private val _state = MutableStateFlow(GameDetailState())
    val state: StateFlow<GameDetailState> = _state.asStateFlow()

    init {
        loadGameDetail()
    }

    private fun loadGameDetail() {
        combine(
            playerRepository.getAllPlayers(),
            gameRepository.getGameSessionById(gameId),
        ) { players, session ->
            val playerMap = players.associateBy { it.id }
            GameDetailState(
                session = session,
                players = playerMap,
                expandedLegs = setOf(0),
                isLoading = false,
                error = if (session == null) "Game not found" else null,
            )
        }.onEach { newState ->
            log.d { "Loaded game detail: ${newState.session?.id}" }
            _state.value = newState
        }.catch { e ->
            log.e(e) { "Error loading game detail" }
            _state.update { it.copy(isLoading = false, error = e.message) }
        }.launchIn(screenModelScope)
    }

    fun toggleLegExpansion(legIndex: Int) {
        _state.update { currentState ->
            val currentExpanded = currentState.expandedLegs
            val newExpanded = if (legIndex in currentExpanded) {
                currentExpanded - legIndex
            } else {
                currentExpanded + legIndex
            }
            currentState.copy(expandedLegs = newExpanded)
        }
    }
}
