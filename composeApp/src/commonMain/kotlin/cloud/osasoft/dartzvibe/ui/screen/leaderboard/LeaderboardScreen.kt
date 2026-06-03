package cloud.osasoft.dartzvibe.ui.screen.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cloud.osasoft.dartzvibe.LocalGameRepository
import cloud.osasoft.dartzvibe.LocalPlayerRepository
import cloud.osasoft.dartzvibe.data.model.FixedDecimal
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class LeaderboardScreen : Screen {

    @Composable
    override fun Content() {
        val playerRepository = LocalPlayerRepository.current
        val gameRepository = LocalGameRepository.current
        val screenModel = rememberLeaderboardScreenModel(playerRepository, gameRepository)
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LeaderboardContent(
            state = state,
            onBack = { navigator.pop() },
            onSelectMode = screenModel::selectMode,
            onSelectMetric = screenModel::selectSortMetric,
        )
    }
}

@Composable
fun rememberLeaderboardScreenModel(
    playerRepository: PlayerRepository,
    gameRepository: GameRepository,
): LeaderboardScreenModel = remember {
    LeaderboardScreenModel(playerRepository, gameRepository)
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalUuidApi::class)
@Composable
fun LeaderboardContent(
    state: LeaderboardScreenState,
    onBack: () -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onSelectMetric: (LeaderboardSortMetric) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard") },
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
                .padding(padding),
        ) {
            // Mode selector (per-mode leaderboards; no unified cross-mode ranking)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
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

            // Sort metric chips
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LeaderboardSortMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = state.sortMetric == metric,
                        onClick = { onSelectMetric(metric) },
                        label = { Text(metric.displayName) },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
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

                    state.entries.isEmpty() -> {
                        EmptyLeaderboard(modifier = Modifier.align(Alignment.Center))
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(
                                state.entries,
                                key = { it.player.id.toString() },
                            ) { entry ->
                                LeaderboardCard(
                                    entry = entry,
                                    sortMetric = state.sortMetric,
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
@Composable
private fun EmptyLeaderboard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No rankings yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Complete some games to see the leaderboard",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun LeaderboardCard(
    entry: LeaderboardEntry,
    sortMetric: LeaderboardSortMetric,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (entry.rank) {
                1 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                2, 3 -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Rank badge
            RankBadge(rank = entry.rank)

            Spacer(modifier = Modifier.width(16.dp))

            // Player avatar and name
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerAvatar(name = entry.player.name)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = entry.player.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (entry.rank <= 3) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            text = "${entry.statistics.gamesWon}/${entry.statistics.gamesPlayed} games won",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Primary stat value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = getPrimaryStatValue(entry, sortMetric),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.rank <= 3) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = getPrimaryStatLabel(entry, sortMetric),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun RankBadge(rank: Int) {
    val badgeColor = when (rank) {
        1 -> Color(0xFFFFD700)

        // Gold
        2 -> Color(0xFFC0C0C0)

        // Silver
        3 -> Color(0xFFCD7F32)

        // Bronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (rank) {
        1, 2, 3 -> Color.Black
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(badgeColor),
        contentAlignment = Alignment.Center,
    ) {
        if (rank <= 3) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = "Rank $rank",
                modifier = Modifier.size(24.dp),
                tint = textColor,
            )
        } else {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
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

@OptIn(ExperimentalUuidApi::class)
private fun getPrimaryStatValue(
    entry: LeaderboardEntry,
    metric: LeaderboardSortMetric,
): String = when (metric) {
    LeaderboardSortMetric.PRIMARY -> entry.statistics.modeStats?.primaryMetricDisplay ?: "-"
    LeaderboardSortMetric.WIN_RATE -> "${formatDecimal(entry.statistics.winRate)}%"
    LeaderboardSortMetric.GAMES_WON -> entry.statistics.gamesWon.toString()
}

@OptIn(ExperimentalUuidApi::class)
private fun getPrimaryStatLabel(
    entry: LeaderboardEntry,
    metric: LeaderboardSortMetric,
): String = when (metric) {
    LeaderboardSortMetric.PRIMARY -> entry.statistics.modeStats?.primaryMetricLabel ?: metric.displayName
    else -> metric.displayName
}

private fun formatDecimal(value: FixedDecimal): String = value.format(1)
