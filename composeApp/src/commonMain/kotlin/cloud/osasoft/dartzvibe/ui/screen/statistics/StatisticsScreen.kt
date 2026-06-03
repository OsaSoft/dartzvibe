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
import cloud.osasoft.dartzvibe.LocalGameRepository
import cloud.osasoft.dartzvibe.LocalPlayerRepository
import cloud.osasoft.dartzvibe.data.model.FixedDecimal
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameReference
import cloud.osasoft.dartzvibe.data.model.ModeStatistics
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.PlayerStatistics
import cloud.osasoft.dartzvibe.data.model.StatAchievement
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.util.formatDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class StatisticsScreen : Screen {

    @Composable
    override fun Content() {
        val playerRepository = LocalPlayerRepository.current
        val gameRepository = LocalGameRepository.current
        val screenModel = rememberStatisticsScreenModel(playerRepository, gameRepository)
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        StatisticsScreenContent(
            state = state,
            onBack = { navigator.pop() },
            onSelectPlayer = screenModel::selectPlayer,
            onSelectMode = screenModel::selectMode,
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
    onSelectMode: (GameMode) -> Unit,
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
            // Mode selector (mode-first: the whole content region reshapes per mode)
            Text(
                "Game Mode",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GameMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.selectedMode == mode,
                        onClick = { onSelectMode(mode) },
                        label = { Text(mode.displayName) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player selector dropdown
            PlayerSelector(
                players = state.players,
                selectedPlayer = state.selectedPlayer,
                onSelectPlayer = onSelectPlayer,
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                    Text(
                        "Select a player to view statistics",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                state.statistics.gamesPlayed == 0 -> {
                    Text(
                        "No ${state.selectedMode.displayName} games yet",
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
    when (val mode = statistics.modeStats) {
        is ModeStatistics.Classic -> ClassicContent(statistics, mode, expandedSections, onToggleSection)

        is ModeStatistics.Parcheesi -> ParcheesiContent(statistics, mode, expandedSections, onToggleSection)

        is ModeStatistics.Cricket -> CricketContent(statistics, mode, expandedSections, onToggleSection)

        is ModeStatistics.Roulette -> RouletteContent(statistics, mode, expandedSections, onToggleSection)

        is ModeStatistics.CheckoutPractice ->
            CheckoutPracticeContent(statistics, mode, expandedSections, onToggleSection)

        null -> Text(
            "No statistics available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- Per-mode content ----

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ClassicContent(
    statistics: PlayerStatistics,
    mode: ModeStatistics.Classic,
    expandedSections: Set<StatSection>,
    onToggleSection: (StatSection) -> Unit,
) {
    StatRow {
        StatCard("3-Dart Avg", formatDecimal(mode.threeDartAverage), Modifier.weight(1f))
        StatCard("Checkout %", formatPercent(mode.checkoutPercentage), Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(16.dp))
    StatRow {
        StatCard("First 9 Avg", formatDecimal(mode.first9Average), Modifier.weight(1f))
        StatCardWithAchievement("Best Checkout", mode.bestCheckout, Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(16.dp))

    SectionTitle("High Scores")
    StatRow {
        ExpandableStatCard(
            title = "180s",
            count = mode.count180s,
            gameRefs = mode.games180s,
            isExpanded = StatSection.GAMES_180S in expandedSections,
            onToggle = { onToggleSection(StatSection.GAMES_180S) },
            modifier = Modifier.weight(1f),
        )
        StatCard("140+", mode.count140Plus.toString(), Modifier.weight(1f))
        StatCard("100+", mode.count100Plus.toString(), Modifier.weight(1f))
    }
    mode.highestTurnScore?.let { highest ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Highest Turn: ${highest.value} (vs ${highest.game.opponentNames})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    RecordSection(statistics, expandedSections, onToggleSection)
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ParcheesiContent(
    statistics: PlayerStatistics,
    mode: ModeStatistics.Parcheesi,
    expandedSections: Set<StatSection>,
    onToggleSection: (StatSection) -> Unit,
) {
    val koRatio = if (mode.hasKnockoutData) formatDecimal(mode.knockoutRatio) else "N/A"
    StatRow {
        StatCard("Win Rate", formatPercent(statistics.winRate), Modifier.weight(1f))
        StatCard("KO Ratio", koRatio, Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(16.dp))
    StatRow {
        StatCard("Knockouts Dealt", mode.knockoutsDealt.toString(), Modifier.weight(1f))
        StatCard("Times Knocked Out", mode.timesKnockedOut.toString(), Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(16.dp))
    StatRow {
        StatCard("Bounce-Back", formatPercent(mode.bounceBackRate), Modifier.weight(1f))
        StatCard("Avg Turns to Win", formatDecimal(mode.avgTurnsToWin), Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(16.dp))
    RecordSection(statistics, expandedSections, onToggleSection)
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun CricketContent(
    statistics: PlayerStatistics,
    mode: ModeStatistics.Cricket,
    expandedSections: Set<StatSection>,
    onToggleSection: (StatSection) -> Unit,
) {
    StatRow {
        StatCard("MPR", formatDecimal(mode.marksPerRound), Modifier.weight(1f))
        StatCard("Close Rate", formatPercent(mode.closeRate), Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(16.dp))
    StatRow {
        StatCard("Avg Points", formatDecimal(mode.avgPointsPerGame), Modifier.weight(1f))
        StatCard("Hit Rate", formatPercent(mode.hitRate), Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(16.dp))
    RecordSection(statistics, expandedSections, onToggleSection)
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun RouletteContent(
    statistics: PlayerStatistics,
    mode: ModeStatistics.Roulette,
    expandedSections: Set<StatSection>,
    onToggleSection: (StatSection) -> Unit,
) {
    StatRow {
        StatCard("Pts / Round", formatDecimal(mode.pointsPerRound), Modifier.weight(1f))
        StatCard("Best Round", mode.bestRoundScore.toString(), Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(16.dp))
    StatRow {
        StatCard("Hit Rate", formatPercent(mode.hitRate), Modifier.weight(1f))
        StatCard("Win Rate", formatPercent(statistics.winRate), Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(16.dp))
    RecordSection(statistics, expandedSections, onToggleSection)
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun CheckoutPracticeContent(
    statistics: PlayerStatistics,
    mode: ModeStatistics.CheckoutPractice,
    expandedSections: Set<StatSection>,
    onToggleSection: (StatSection) -> Unit,
) {
    StatRow {
        StatCard("Success Rate", formatPercent(mode.successRate), Modifier.weight(1f))
        StatCard("Avg Darts", formatDecimal(mode.avgDartsToCheckout), Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(16.dp))
    StatRow {
        StatCard("Best Target", mode.bestTarget?.toString() ?: "-", Modifier.weight(1f))
        StatCard("Sessions", mode.sessionsCompleted.toString(), Modifier.weight(1f))
    }
    if (mode.bands.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle("By Difficulty")
        CheckoutBandsCard(
            mode = mode,
            isExpanded = StatSection.CHECKOUT_BANDS in expandedSections,
            onToggle = { onToggleSection(StatSection.CHECKOUT_BANDS) },
        )
    }
}

// ---- Universal record section (omitted for solo Checkout Practice) ----

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun RecordSection(
    statistics: PlayerStatistics,
    expandedSections: Set<StatSection>,
    onToggleSection: (StatSection) -> Unit,
) {
    SectionTitle("Record")
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

// ---- Shared building blocks ----

@Suppress("ktlint:standard:function-naming")
@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun StatRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
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
@Composable
private fun CheckoutBandsCard(
    mode: ModeStatistics.CheckoutPractice,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
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
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Success rate by difficulty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse difficulty" else "Expand difficulty",
                    modifier = Modifier.size(24.dp),
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    mode.bands.forEach { band ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = band.band.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${formatPercent(band.successRate)} (${band.successCount}/${band.attempts})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
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
                        contentDescription = if (isExpanded) "Collapse $title list" else "Expand $title list",
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

/** Format a FixedDecimal to one decimal place for display. */
private fun formatDecimal(value: FixedDecimal): String = value.format(1)

/** Format a percentage-valued FixedDecimal (0–100) with a trailing %. */
private fun formatPercent(value: FixedDecimal): String = "${value.format(1)}%"
