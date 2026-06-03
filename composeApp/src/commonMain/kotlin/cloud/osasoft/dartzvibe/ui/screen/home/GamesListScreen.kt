package cloud.osasoft.dartzvibe.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cloud.osasoft.dartzvibe.LocalGameRepository
import cloud.osasoft.dartzvibe.LocalPlayerRepository
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.ui.screen.game.ActiveGameScreen
import cloud.osasoft.dartzvibe.ui.screen.gamedetail.GameDetailScreen
import cloud.osasoft.dartzvibe.util.formatDateTime
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
data class GameWithPlayers(
    val session: GameSession,
    val players: List<Player>,
)

@OptIn(ExperimentalUuidApi::class)
data class GamesListState(
    val games: List<GameWithPlayers> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val gameToDelete: GameSession? = null,
)

@OptIn(ExperimentalUuidApi::class)
class GamesListScreenModel(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
) : ScreenModel {

    private val log = Logger.withTag("GamesListScreenModel")

    private val _state = MutableStateFlow(GamesListState())
    val state: StateFlow<GamesListState> = _state.asStateFlow()

    init {
        loadGames()
    }

    private fun loadGames() {
        combine(
            playerRepository.getAllPlayers(),
            gameRepository.getAllGameSessions(),
        ) { players, games ->
            games.map { session ->
                val gamePlayers = session.config.playerIds.mapNotNull { playerId ->
                    players.find { it.id == playerId }
                }
                GameWithPlayers(session, gamePlayers)
            }
        }.onEach { gamesWithPlayers ->
            log.d { "Loaded ${gamesWithPlayers.size} games" }
            _state.update { it.copy(games = gamesWithPlayers, isLoading = false) }
        }.catch { e ->
            log.e(e) { "Error loading games" }
            _state.update { it.copy(isLoading = false, error = e.message) }
        }.launchIn(screenModelScope)
    }

    fun showDeleteConfirmation(session: GameSession) {
        _state.update { it.copy(gameToDelete = session) }
    }

    fun dismissDeleteConfirmation() {
        _state.update { it.copy(gameToDelete = null) }
    }

    fun confirmDelete() {
        val session = _state.value.gameToDelete ?: return
        screenModelScope.launch {
            try {
                gameRepository.deleteGameSession(session.id)
                _state.update { it.copy(gameToDelete = null) }
            } catch (e: Exception) {
                log.e(e) { "Error deleting game" }
                _state.update { it.copy(error = e.message, gameToDelete = null) }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
class GamesListScreen : Screen {

    @Composable
    override fun Content() {
        val playerRepository = LocalPlayerRepository.current
        val gameRepository = LocalGameRepository.current
        val screenModel = remember { GamesListScreenModel(playerRepository, gameRepository) }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        GamesListContent(
            state = state,
            onGameClick = { session ->
                when (session.status) {
                    GameStatus.IN_PROGRESS -> {
                        navigator.push(ActiveGameScreen(session.id))
                    }

                    GameStatus.COMPLETED,
                    GameStatus.ABANDONED,
                    -> {
                        navigator.push(GameDetailScreen(session.id))
                    }
                }
            },
            onDeleteGame = { screenModel.showDeleteConfirmation(it) },
            onConfirmDelete = { screenModel.confirmDelete() },
            onDismissDelete = { screenModel.dismissDeleteConfirmation() },
            onBack = { navigator.pop() },
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun GamesListContent(
    state: GamesListState,
    onGameClick: (GameSession) -> Unit,
    onDeleteGame: (GameSession) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Games") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null -> {
                    Text(
                        text = "Error: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                state.games.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No games yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Start a new game from the home screen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.games, key = { it.session.id }) { gameWithPlayers ->
                            GameCard(
                                game = gameWithPlayers,
                                onClick = { onGameClick(gameWithPlayers.session) },
                                onDelete = { onDeleteGame(gameWithPlayers.session) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    state.gameToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete Game") },
            text = { Text("Are you sure you want to delete this game?") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
fun GameCard(
    game: GameWithPlayers,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val session = game.session
    val isInProgress = session.status == GameStatus.IN_PROGRESS

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInProgress) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status icon
            Icon(
                imageVector = when (session.status) {
                    GameStatus.IN_PROGRESS -> Icons.Default.Refresh
                    GameStatus.COMPLETED -> Icons.Default.PlayArrow
                    GameStatus.ABANDONED -> Icons.Default.Delete
                },
                contentDescription = null,
                tint = when (session.status) {
                    GameStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                    GameStatus.COMPLETED -> MaterialTheme.colorScheme.outline
                    GameStatus.ABANDONED -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Game info
            Column(modifier = Modifier.weight(1f)) {
                // Players
                Text(
                    text = game.players.joinToString(" vs ") { it.name },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                // Status
                Text(
                    text = when (session.status) {
                        GameStatus.IN_PROGRESS -> "In Progress"

                        GameStatus.COMPLETED -> {
                            val winner = game.players.find { it.id == session.winnerId }
                            winner?.let { "${it.name} won" } ?: "Completed"
                        }

                        GameStatus.ABANDONED -> "Abandoned"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (session.status) {
                        GameStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                        GameStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
                        GameStatus.ABANDONED -> MaterialTheme.colorScheme.error
                    },
                )

                // Mode badge + game type
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeBadge(mode = session.config.gameMode)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = session.config.displayDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Legs info
                if (session.config.legsToWin > 1) {
                    Text(
                        text = "Best of ${session.config.legsToWin * 2 - 1} legs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                // Timestamp
                Text(
                    text = when (session.status) {
                        GameStatus.COMPLETED -> "Finished at ${formatDateTime(session.finishedAt ?: session.startedAt)}"

                        GameStatus.ABANDONED -> "Abandoned at ${formatDateTime(
                            session.finishedAt ?: session.startedAt,
                        )}"

                        GameStatus.IN_PROGRESS -> "Started at ${formatDateTime(session.startedAt)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Actions
            if (isInProgress) {
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Distinct colour per game mode, for at-a-glance scanning of the history list. */
private fun modeColor(mode: GameMode): Color = when (mode) {
    GameMode.CLASSIC -> Color(0xFF1565C0)
    GameMode.PARCHEESI -> Color(0xFF6A1B9A)
    GameMode.CRICKET -> Color(0xFF2E7D32)
    GameMode.CHECKOUT_PRACTICE -> Color(0xFFEF6C00)
    GameMode.ROULETTE -> Color(0xFFC62828)
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ModeBadge(mode: GameMode) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(modeColor(mode))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = mode.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}
