package cloud.osasoft.dartzvibe.ui.screen.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cloud.osasoft.dartzvibe.LocalPlayerRepository
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.domain.game.ThrowResult
import cloud.osasoft.dartzvibe.domain.game.TurnResult
import cloud.osasoft.dartzvibe.ui.screen.game.components.PlayerScoreCard
import cloud.osasoft.dartzvibe.ui.screen.game.components.ScoreInputKeypad
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ActiveGameScreen(
    private val gameRepository: GameRepository,
    private val sessionId: Uuid,
) : Screen {

    @Composable
    override fun Content() {
        val playerRepository = LocalPlayerRepository.current
        val screenModel = rememberActiveGameScreenModel(gameRepository, playerRepository, sessionId)
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        ActiveGameContent(
            state = state,
            onMultiplierChange = screenModel::setMultiplier,
            onScoreSelect = screenModel::onScoreSelect,
            onMiss = screenModel::onMiss,
            onUndo = screenModel::undoLastThrow,
            onEndTurn = screenModel::endTurn,
            onDismissLegWon = screenModel::dismissLegWonDialog,
            onDismissGameComplete = {
                screenModel.dismissGameCompleteDialog()
                navigator.pop()
            },
            onAbandonGame = {
                screenModel.abandonGame()
                navigator.pop()
            },
            onBack = { navigator.pop() },
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun rememberActiveGameScreenModel(
    gameRepository: GameRepository,
    playerRepository: cloud.osasoft.dartzvibe.data.repository.PlayerRepository,
    sessionId: Uuid,
): ActiveGameScreenModel = remember { ActiveGameScreenModel(gameRepository, playerRepository, sessionId) }

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ActiveGameContent(
    state: ActiveGameState,
    onMultiplierChange: (Multiplier) -> Unit,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onMiss: () -> Unit,
    onUndo: () -> Unit,
    onEndTurn: () -> Unit,
    onDismissLegWon: () -> Unit,
    onDismissGameComplete: () -> Unit,
    onAbandonGame: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.session
                            ?.config
                            ?.gameType
                            ?.displayName ?: "Game",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAbandonGame) {
                        Icon(Icons.Default.Close, contentDescription = "Abandon game")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Error: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // Player score cards
                PlayerScoresRow(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )

                // Current turn display
                CurrentTurnDisplay(
                    currentThrows = state.currentTurnThrows,
                    selectedMultiplier = state.selectedMultiplier,
                    lastThrowResult = state.lastThrowResult,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                )

                Spacer(modifier = Modifier.weight(1f))

                // Score input keypad
                ScoreInputKeypad(
                    selectedMultiplier = state.selectedMultiplier,
                    throwsRemaining = state.throwsRemaining,
                    canThrow = state.canThrow,
                    canUndo = state.canUndo,
                    onMultiplierChange = onMultiplierChange,
                    onScoreSelect = onScoreSelect,
                    onMiss = onMiss,
                    onUndo = onUndo,
                    onEndTurn = onEndTurn,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // Leg won dialog
    if (state.showLegWonDialog) {
        val result = state.lastTurnResult as? TurnResult.LegWon
        val winnerName = result?.let { state.getPlayer(it.winnerId)?.name } ?: "Player"

        AlertDialog(
            onDismissRequest = onDismissLegWon,
            title = { Text("Leg Won!") },
            text = { Text("$winnerName wins the leg!") },
            confirmButton = {
                TextButton(onClick = onDismissLegWon) {
                    Text("Continue")
                }
            },
        )
    }

    // Game complete dialog
    if (state.showGameCompleteDialog) {
        val result = state.lastTurnResult as? TurnResult.MatchWon
        val winnerName = result?.let { state.getPlayer(it.winnerId)?.name } ?: "Player"

        AlertDialog(
            onDismissRequest = { },
            title = { Text("Game Over!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$winnerName wins the match!",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissGameComplete) {
                    Text("Finish")
                }
            },
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
fun PlayerScoresRow(
    state: ActiveGameState,
    modifier: Modifier = Modifier,
) {
    val session = state.session ?: return
    val playerIds = session.config.playerIds
    val legsToWin = session.config.legsToWin

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        playerIds.forEach { playerId ->
            val player = state.getPlayer(playerId)
            if (player != null) {
                PlayerScoreCard(
                    player = player,
                    score = state.getPlayerScore(playerId),
                    legsWon = state.getLegsWon(playerId),
                    legsToWin = legsToWin,
                    isCurrentPlayer = playerId == state.currentPlayerId,
                    lastTurnScore = state.getLastTurnScore(playerId),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun CurrentTurnDisplay(
    currentThrows: List<Throw>,
    selectedMultiplier: Multiplier,
    lastThrowResult: ThrowResult?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Current turn throws
            Text(
                text = "Current Turn",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Show existing throws
                currentThrows.forEachIndexed { index, throwObj ->
                    ThrowDisplay(throwObj)
                }

                // Show placeholder for remaining throws
                repeat(3 - currentThrows.size) {
                    ThrowPlaceholder()
                }
            }

            // Show last throw result
            lastThrowResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                val resultText = when (result) {
                    is ThrowResult.Success -> "Score: ${result.newScore}"
                    is ThrowResult.Bust -> "BUST: ${result.reason}"
                    is ThrowResult.Checkout -> "CHECKOUT!"
                }
                val resultColor = when (result) {
                    is ThrowResult.Success -> MaterialTheme.colorScheme.onSurfaceVariant
                    is ThrowResult.Bust -> MaterialTheme.colorScheme.error
                    is ThrowResult.Checkout -> MaterialTheme.colorScheme.primary
                }
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodySmall,
                    color = resultColor,
                    fontWeight = if (result is ThrowResult.Checkout) FontWeight.Bold else FontWeight.Normal,
                )
            }

            // Turn total
            if (currentThrows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val total = currentThrows.sumOf { it.score }
                Text(
                    text = "Turn total: $total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ThrowDisplay(throwObj: Throw) {
    val displayText = when {
        throwObj.segment == 0 -> "Miss"
        throwObj.segment == 25 && throwObj.multiplier == Multiplier.DOUBLE -> "BULL"
        else -> "${throwObj.multiplier.name.first()}${throwObj.segment}"
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ThrowPlaceholder() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "-",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
