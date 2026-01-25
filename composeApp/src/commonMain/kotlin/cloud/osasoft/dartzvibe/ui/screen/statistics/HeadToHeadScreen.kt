package cloud.osasoft.dartzvibe.ui.screen.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cloud.osasoft.dartzvibe.data.model.FixedDecimal
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.H2HGameSummary
import cloud.osasoft.dartzvibe.data.model.HeadToHeadStatistics
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.util.formatDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HeadToHeadScreen(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
) : Screen {

    @Composable
    override fun Content() {
        val screenModel = rememberHeadToHeadScreenModel(playerRepository, gameRepository)
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        HeadToHeadContent(
            state = state,
            onBack = { navigator.pop() },
            onSelectPlayer1 = screenModel::selectPlayer1,
            onSelectPlayer2 = screenModel::selectPlayer2,
            onSwapPlayers = screenModel::swapPlayers,
            onSelectGameType = screenModel::selectGameType,
        )
    }
}

@Composable
fun rememberHeadToHeadScreenModel(
    playerRepository: PlayerRepository,
    gameRepository: GameRepository,
): HeadToHeadScreenModel = remember {
    HeadToHeadScreenModel(playerRepository, gameRepository)
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalUuidApi::class)
@Composable
fun HeadToHeadContent(
    state: HeadToHeadScreenState,
    onBack: () -> Unit,
    onSelectPlayer1: (Uuid) -> Unit,
    onSelectPlayer2: (Uuid) -> Unit,
    onSwapPlayers: () -> Unit,
    onSelectGameType: (GameType?) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Head-to-Head") },
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

                state.players.size < 2 -> {
                    Text(
                        text = "Need at least 2 players for head-to-head comparison",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // Player selection row
                        PlayerSelectionRow(
                            players = state.players,
                            selectedPlayer1 = state.selectedPlayer1,
                            selectedPlayer2 = state.selectedPlayer2,
                            onSelectPlayer1 = onSelectPlayer1,
                            onSelectPlayer2 = onSelectPlayer2,
                            onSwapPlayers = onSwapPlayers,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Game type filter chips
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = state.selectedGameType == null,
                                onClick = { onSelectGameType(null) },
                                label = { Text("All Games") },
                            )
                            GameType.entries.forEach { gameType ->
                                FilterChip(
                                    selected = state.selectedGameType == gameType,
                                    onClick = { onSelectGameType(gameType) },
                                    label = { Text(gameType.displayName) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Statistics content
                        val stats = state.statistics
                        if (stats != null && state.selectedPlayer1 != null && state.selectedPlayer2 != null) {
                            if (stats.gamesPlayed == 0) {
                                Text(
                                    text = "No head-to-head games found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )
                            } else {
                                H2HStatisticsContent(
                                    stats = stats,
                                    player1 = state.selectedPlayer1,
                                    player2 = state.selectedPlayer2,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
private fun PlayerSelectionRow(
    players: List<Player>,
    selectedPlayer1: Player?,
    selectedPlayer2: Player?,
    onSelectPlayer1: (Uuid) -> Unit,
    onSelectPlayer2: (Uuid) -> Unit,
    onSwapPlayers: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerDropdown(
            label = "Player 1",
            players = players,
            selectedPlayer = selectedPlayer1,
            excludePlayer = selectedPlayer2,
            onSelect = onSelectPlayer1,
            modifier = Modifier.weight(1f),
        )

        IconButton(
            onClick = onSwapPlayers,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Icon(
                Icons.Default.SwapHoriz,
                contentDescription = "Swap players",
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        PlayerDropdown(
            label = "Player 2",
            players = players,
            selectedPlayer = selectedPlayer2,
            excludePlayer = selectedPlayer1,
            onSelect = onSelectPlayer2,
            modifier = Modifier.weight(1f),
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
private fun PlayerDropdown(
    label: String,
    players: List<Player>,
    selectedPlayer: Player?,
    excludePlayer: Player?,
    onSelect: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedPlayer?.name ?: "Select",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            label = { Text(label) },
            singleLine = true,
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            players
                .filter { it.id != excludePlayer?.id }
                .forEach { player ->
                    DropdownMenuItem(
                        text = { Text(player.name) },
                        onClick = {
                            onSelect(player.id)
                            expanded = false
                        },
                    )
                }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun H2HStatisticsContent(
    stats: HeadToHeadStatistics,
    player1: Player,
    player2: Player,
) {
    // Win/Loss Record Card
    WinLossRecordCard(
        stats = stats,
        player1 = player1,
        player2 = player2,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Comparison Stats Grid
    ComparisonStatsGrid(
        stats = stats,
        player1 = player1,
        player2 = player2,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Recent Games Section
    RecentGamesSection(
        games = stats.recentGames,
        player1 = player1,
        player2 = player2,
    )
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun WinLossRecordCard(
    stats: HeadToHeadStatistics,
    player1: Player,
    player2: Player,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Games Record",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Player 1 side
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayerAvatar(name = player1.name)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = player1.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (stats.player1Wins > stats.player2Wins) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "${formatDecimal(stats.player1WinRate)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }

                // Score display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stats.player1Wins.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.player1Wins > stats.player2Wins) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                    Text(
                        text = " - ",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stats.player2Wins.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.player2Wins > stats.player1Wins) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                }

                // Player 2 side
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayerAvatar(name = player2.name)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = player2.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (stats.player2Wins > stats.player1Wins) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "${formatDecimal(stats.player2WinRate)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${stats.gamesPlayed} games played",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun PlayerAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ComparisonStatsGrid(
    stats: HeadToHeadStatistics,
    player1: Player,
    player2: Player,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Statistics Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header row
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1.2f))
                Text(
                    text = player1.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = player2.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats rows
            ComparisonRow(
                label = "3-Dart Avg",
                value1 = formatDecimal(stats.player1Stats.threeDartAverage),
                value2 = formatDecimal(stats.player2Stats.threeDartAverage),
                higher1IsBetter = true,
                value1Higher = stats.player1Stats.threeDartAverage > stats.player2Stats.threeDartAverage,
            )

            ComparisonRow(
                label = "Best Checkout",
                value1 = stats.player1Stats.bestCheckout?.toString() ?: "-",
                value2 = stats.player2Stats.bestCheckout?.toString() ?: "-",
                higher1IsBetter = true,
                value1Higher = (stats.player1Stats.bestCheckout ?: 0) >
                    (stats.player2Stats.bestCheckout ?: 0),
            )

            ComparisonRow(
                label = "Leg Win Rate",
                value1 = "${formatDecimal(stats.player1Stats.legWinRate)}%",
                value2 = "${formatDecimal(stats.player2Stats.legWinRate)}%",
                higher1IsBetter = true,
                value1Higher = stats.player1Stats.legWinRate > stats.player2Stats.legWinRate,
            )

            ComparisonRow(
                label = "180s",
                value1 = stats.player1Stats.count180s.toString(),
                value2 = stats.player2Stats.count180s.toString(),
                higher1IsBetter = true,
                value1Higher = stats.player1Stats.count180s > stats.player2Stats.count180s,
            )

            ComparisonRow(
                label = "140+",
                value1 = stats.player1Stats.count140Plus.toString(),
                value2 = stats.player2Stats.count140Plus.toString(),
                higher1IsBetter = true,
                value1Higher = stats.player1Stats.count140Plus > stats.player2Stats.count140Plus,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ComparisonRow(
    label: String,
    value1: String,
    value2: String,
    higher1IsBetter: Boolean,
    value1Higher: Boolean,
) {
    val value1Wins = higher1IsBetter == value1Higher && value1 != value2
    val value2Wins = higher1IsBetter != value1Higher && value1 != value2

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f),
        )
        Text(
            text = value1,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (value1Wins) FontWeight.Bold else FontWeight.Normal,
            color = if (value1Wins) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value2,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (value2Wins) FontWeight.Bold else FontWeight.Normal,
            color = if (value2Wins) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun RecentGamesSection(
    games: List<H2HGameSummary>,
    player1: Player,
    player2: Player,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent Games",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    games.forEach { game ->
                        RecentGameRow(
                            game = game,
                            player1 = player1,
                            player2 = player2,
                        )
                    }
                }
            }

            if (!expanded && games.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${games.size} games",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun RecentGameRow(
    game: H2HGameSummary,
    player1: Player,
    player2: Player,
) {
    val winner = when (game.winnerId) {
        player1.id -> player1
        player2.id -> player2
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (winner != null) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Winner",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = winner?.name ?: "Draw",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "${game.gameType.displayName} | ${formatDate(game.timestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = "${game.player1LegsWon} - ${game.player2LegsWon}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatDecimal(value: FixedDecimal): String = value.format(1)
