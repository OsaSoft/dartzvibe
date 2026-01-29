package cloud.osasoft.dartzvibe.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cloud.osasoft.dartzvibe.LocalGameRepository
import cloud.osasoft.dartzvibe.LocalPlayerRepository
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.ui.screen.game.ActiveGameScreen
import cloud.osasoft.dartzvibe.ui.screen.game.NewGameScreen
import cloud.osasoft.dartzvibe.ui.screen.leaderboard.LeaderboardScreen
import cloud.osasoft.dartzvibe.ui.screen.players.PlayerListScreen
import cloud.osasoft.dartzvibe.ui.screen.settings.SettingsScreen
import cloud.osasoft.dartzvibe.ui.screen.statistics.HeadToHeadScreen
import cloud.osasoft.dartzvibe.ui.screen.statistics.StatisticsScreen
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val playerRepository = LocalPlayerRepository.current
        val gameRepository = LocalGameRepository.current
        val screenModel = rememberHomeScreenModel(playerRepository, gameRepository)
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        HomeScreenContent(
            state = state,
            onNewGame = {
                navigator.push(NewGameScreen())
            },
            onResumeGame = { sessionId ->
                navigator.push(ActiveGameScreen(sessionId))
            },
            onViewGames = {
                navigator.push(GamesListScreen())
            },
            onViewStatistics = {
                navigator.push(StatisticsScreen())
            },
            onViewHeadToHead = {
                navigator.push(HeadToHeadScreen())
            },
            onViewLeaderboard = {
                navigator.push(LeaderboardScreen())
            },
            onManagePlayers = {
                navigator.push(PlayerListScreen())
            },
            onSettings = {
                navigator.push(SettingsScreen())
            },
        )
    }
}

@Composable
fun rememberHomeScreenModel(
    playerRepository: PlayerRepository,
    gameRepository: GameRepository,
): HomeScreenModel = remember { HomeScreenModel(playerRepository, gameRepository) }

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun HomeScreenContent(
    state: HomeScreenState,
    onNewGame: () -> Unit,
    onResumeGame: (kotlin.uuid.Uuid) -> Unit,
    onViewGames: () -> Unit,
    onViewStatistics: () -> Unit,
    onViewHeadToHead: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onManagePlayers: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DartzVibe") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // App title/logo area
            Text(
                text = "DartzVibe",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Track your darts games",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Resume game button (if there's an in-progress game)
            state.inProgressGame?.let { session ->
                val playerNames = state.inProgressGamePlayers.joinToString(" vs ") { it.name }
                Button(
                    onClick = { onResumeGame(session.id) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            "Resume Game",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${session.config.displayDescription} - $playerNames",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // New Game button
            MenuButton(
                text = "New Game",
                icon = Icons.Default.PlayArrow,
                enabled = state.playerCount >= 2,
                onClick = onNewGame,
            )

            if (state.playerCount < 2) {
                Text(
                    text = "Add at least 2 players to start a game",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Games history button
            MenuButton(
                text = "Games",
                subtitle = if (state.completedGameCount > 0) "${state.completedGameCount} completed" else null,
                icon = Icons.AutoMirrored.Filled.List,
                onClick = onViewGames,
                isPrimary = false,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics button
            MenuButton(
                text = "Statistics",
                icon = Icons.Default.BarChart,
                onClick = onViewStatistics,
                enabled = state.completedGameCount > 0,
                isPrimary = false,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Head-to-Head button
            MenuButton(
                text = "Head-to-Head",
                icon = Icons.Default.People,
                onClick = onViewHeadToHead,
                enabled = state.completedGameCount > 0 && state.playerCount >= 2,
                isPrimary = false,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Leaderboard button
            MenuButton(
                text = "Leaderboard",
                icon = Icons.Default.Leaderboard,
                onClick = onViewLeaderboard,
                enabled = state.completedGameCount > 0,
                isPrimary = false,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Manage Players button
            MenuButton(
                text = "Players",
                subtitle = "${state.playerCount} player${if (state.playerCount != 1) "s" else ""}",
                icon = Icons.Default.Person,
                onClick = onManagePlayers,
                isPrimary = false,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun MenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(text, style = MaterialTheme.typography.titleMedium)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(text, style = MaterialTheme.typography.titleMedium)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
