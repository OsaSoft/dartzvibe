package cloud.osasoft.dartzvibe.ui.screen.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cloud.osasoft.dartzvibe.data.model.FixedDecimal
import cloud.osasoft.dartzvibe.data.model.GameReference
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.PlayerStatistics
import cloud.osasoft.dartzvibe.data.model.StatAchievement
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.util.formatDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class StatisticsScreen(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
) : Screen {

    @Composable
    override fun Content() {
        val screenModel = rememberStatisticsScreenModel(playerRepository, gameRepository)
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        StatisticsScreenContent(
            state = state,
            onBack = { navigator.pop() },
            onSelectPlayer = screenModel::selectPlayer,
            onSelectGameType = screenModel::selectGameType,
            onToggleSection = screenModel::toggleSection,
        )
    }
}

@Composable
fun rememberStatisticsScreenModel(
    playerRepository: PlayerRepository,
    gameRepository: GameRepository,
): StatisticsScreenModel = remember { StatisticsScreenModel(playerRepository, gameRepository) }

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreenContent(
    state: StatisticsScreenState,
    onBack: () -> Unit,
    onSelectPlayer: (Uuid) -> Unit,
    onSelectGameType: (GameType?) -> Unit,
    onToggleSection: (StatSection) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Player selector dropdown
            PlayerSelector(
                players = state.players,
                selectedPlayer = state.selectedPlayer,
                onSelectPlayer = onSelectPlayer,
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
            when {
                state.isLoading -> {
                    Text("Loading statistics...", style = MaterialTheme.typography.bodyLarge)
                }

                state.error != null -> {
                    Text(
                        "Error: ${state.error}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                state.statistics == null -> {
                    Text("Select a player to view statistics", style = MaterialTheme.typography.bodyLarge)
                }

                state.statistics.gamesPlayed == 0 -> {
                    Text(
                        "No completed games found for this player",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    StatisticsContent(
                        statistics = state.statistics,
                        expandedSections = state.expandedSections,
                        onToggleSection = onToggleSection,
                    )
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
private fun PlayerSelector(
    players: List<Player>,
    selectedPlayer: Player?,
    onSelectPlayer: (Uuid) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedPlayer?.name ?: "Select player",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            label = { Text("Player") },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { Text(player.name) },
                    onClick = {
                        onSelectPlayer(player.id)
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
private fun StatisticsContent(
    statistics: PlayerStatistics,
    expandedSections: Set<StatSection>,
    onToggleSection: (StatSection) -> Unit,
) {
    // Primary stats row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatCard(
            title = "3-Dart Avg",
            value = formatDecimal(statistics.threeDartAverage),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = "Checkout %",
            value = "${formatDecimal(statistics.checkoutPercentage)}%",
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Secondary stats row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatCard(
            title = "First 9 Avg",
            value = formatDecimal(statistics.first9Average),
            modifier = Modifier.weight(1f),
        )
        StatCardWithAchievement(
            title = "Best Checkout",
            achievement = statistics.bestCheckout,
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // High scores section
    Text(
        "High Scores",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ExpandableStatCard(
            title = "180s",
            count = statistics.count180s,
            gameRefs = statistics.games180s,
            isExpanded = StatSection.GAMES_180S in expandedSections,
            onToggle = { onToggleSection(StatSection.GAMES_180S) },
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = "140+",
            value = statistics.count140Plus.toString(),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = "100+",
            value = statistics.count100Plus.toString(),
            modifier = Modifier.weight(1f),
        )
    }

    statistics.highestTurnScore?.let { highest ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Highest Turn: ${highest.value} (vs ${highest.game.opponentNames})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Record section
    Text(
        "Record",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))

    ExpandableStatCard(
        title = "Games Won",
        count = statistics.gamesWon,
        subtitle = "${statistics.gamesWon}/${statistics.gamesPlayed}",
        gameRefs = statistics.gamesWonList,
        isExpanded = StatSection.GAMES_WON in expandedSections,
        onToggle = { onToggleSection(StatSection.GAMES_WON) },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(8.dp))

    StatCard(
        title = "Legs Won",
        value = "${statistics.legsWon}/${statistics.legsPlayed}",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun StatCardWithAchievement(
    title: String,
    achievement: StatAchievement?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = achievement?.value?.toString() ?: "-",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (achievement != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "vs ${achievement.game.opponentNames}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ExpandableStatCard(
    title: String,
    count: Int,
    gameRefs: List<GameReference>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = gameRefs.isNotEmpty()) { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle ?: count.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (gameRefs.isNotEmpty()) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded && gameRefs.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    gameRefs.take(5).forEach { ref ->
                        GameRefItem(ref)
                    }
                    if (gameRefs.size > 5) {
                        Text(
                            "... and ${gameRefs.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun GameRefItem(ref: GameReference) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "vs ${ref.opponentNames}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatTimestamp(ref.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatTimestamp(timestamp: Long): String = formatDate(timestamp)

/**
 * Format a FixedDecimal to one decimal place for display.
 */
private fun formatDecimal(value: FixedDecimal): String = value.format(1)
