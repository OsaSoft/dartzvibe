package cloud.osasoft.dartzvibe.ui.screen.game

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import cloud.osasoft.dartzvibe.LocalAppSettingsRepository
import cloud.osasoft.dartzvibe.LocalGameRepository
import cloud.osasoft.dartzvibe.LocalPlayerRepository
import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.domain.game.ThrowResult
import cloud.osasoft.dartzvibe.domain.game.TurnResult
import cloud.osasoft.dartzvibe.ui.screen.game.components.CheckoutHint
import cloud.osasoft.dartzvibe.ui.screen.game.components.CricketScoreboard
import cloud.osasoft.dartzvibe.ui.screen.game.components.PlayerScoreCard
import cloud.osasoft.dartzvibe.ui.screen.game.components.RadialDartboard
import cloud.osasoft.dartzvibe.ui.screen.game.components.ScoreInputKeypad
import cloud.osasoft.dartzvibe.ui.screen.settings.SettingsScreen
import dartzvibe.composeapp.generated.resources.Res
import dartzvibe.composeapp.generated.resources.forever_alone_bw
import org.jetbrains.compose.resources.painterResource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ActiveGameScreen(
    private val sessionId: Uuid,
) : Screen {

    @Composable
    override fun Content() {
        val playerRepository = LocalPlayerRepository.current
        val gameRepository = LocalGameRepository.current
        val appSettingsRepository = LocalAppSettingsRepository.current
        val screenModel = rememberActiveGameScreenModel(gameRepository, playerRepository, sessionId)
        val state by screenModel.state.collectAsState()
        val settings by appSettingsRepository.getSettings().collectAsState(initial = AppSettingsData())
        val navigator = LocalNavigator.currentOrThrow

        ActiveGameContent(
            state = state,
            settings = settings,
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
            onDismissKnockout = screenModel::dismissKnockoutDialog,
            onAbandonGame = {
                screenModel.abandonGame()
                navigator.pop()
            },
            onSettings = { navigator.push(SettingsScreen()) },
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
    settings: AppSettingsData,
    onMultiplierChange: (Multiplier) -> Unit,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onMiss: () -> Unit,
    onUndo: () -> Unit,
    onEndTurn: () -> Unit,
    onDismissLegWon: () -> Unit,
    onDismissGameComplete: () -> Unit,
    onDismissKnockout: () -> Unit,
    onAbandonGame: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
) {
    var showAbandonConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            when {
                                state.isCheckoutPractice -> "Checkout Practice"

                                state.isRoulette -> "Roulette"

                                else ->
                                    state.session
                                        ?.config
                                        ?.gameType
                                        ?.displayName ?: "Game"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                        val isSoloGame = state.session?.config?.playerIds?.size == 1
                        if (isSoloGame && !state.isCheckoutPractice && !state.isRoulette) {
                            Image(
                                painter = painterResource(Res.drawable.forever_alone_bw),
                                contentDescription = "Forever alone",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { showAbandonConfirmDialog = true }) {
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
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Player score cards, Cricket scoreboard, Checkout Practice header, or Roulette header
                if (state.isRoulette) {
                    RouletteHeader(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                    PlayerScoresRow(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                } else if (state.isCheckoutPractice) {
                    CheckoutPracticeHeader(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                } else if (state.isCricket) {
                    val session = state.session
                    val cricketState = state.cricketState
                    if (session != null && cricketState != null) {
                        CricketScoreboard(
                            segments = cricketState.segments,
                            playerIds = session.config.playerIds,
                            players = state.players,
                            cricketState = cricketState,
                            currentPlayerId = state.currentPlayerId,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    PlayerScoresRow(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }

                // Current turn display
                CurrentTurnDisplay(
                    turnNumber = state.currentTurnNumber,
                    currentThrows = state.currentTurnThrows,
                    selectedMultiplier = state.selectedMultiplier,
                    lastThrowResult = state.lastThrowResult,
                    canUndo = state.canUndo,
                    onUndo = onUndo,
                    isCricket = state.session?.config?.gameMode == GameMode.CRICKET,
                    isRoulette = state.isRoulette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                )

                // Checkout suggestions
                state.checkoutOptions?.let { options ->
                    CheckoutHint(
                        checkoutOptions = options,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }

                // Score input keypad (hidden when turn is complete)
                if (state.canThrow) {
                    if (settings.useRadialKeypad) {
                        RadialDartboard(
                            selectedMultiplier = state.selectedMultiplier,
                            canThrow = state.canThrow,
                            showMultiplierButtons = settings.showMultiplierButtons,
                            onMultiplierChange = onMultiplierChange,
                            onScoreSelect = onScoreSelect,
                            onMiss = onMiss,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        ScoreInputKeypad(
                            selectedMultiplier = state.selectedMultiplier,
                            canThrow = state.canThrow,
                            showMultiplierButtons = settings.showMultiplierButtons,
                            onMultiplierChange = onMultiplierChange,
                            onScoreSelect = onScoreSelect,
                            onMiss = onMiss,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    // End Turn button (shown when turn is complete - 3 throws or bust)
                    FilledTonalButton(
                        onClick = onEndTurn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(if (state.isCheckoutPractice) "Next Target" else "End Turn")
                    }
                }
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
        if (state.isRoulette) {
            RouletteResultsDialog(
                state = state,
                onDismiss = onDismissGameComplete,
            )
        } else if (state.isCheckoutPractice) {
            CheckoutPracticeResultsDialog(
                state = state,
                onDismiss = onDismissGameComplete,
            )
        } else {
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

    // Abandon game confirmation dialog
    if (showAbandonConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonConfirmDialog = false },
            title = { Text("Abandon Game") },
            text = { Text("Are you sure you want to abandon this game? It cannot be resumed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAbandonConfirmDialog = false
                        onAbandonGame()
                    },
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonConfirmDialog = false }) {
                    Text("No")
                }
            },
        )
    }

    // Knockout dialog (Parcheesi mode)
    if (state.showKnockoutDialog) {
        val knockedOutNames = state.knockedOutPlayerIds.mapNotNull { state.getPlayer(it)?.name }
        val knockedOutText = when (knockedOutNames.size) {
            1 -> "${knockedOutNames.first()} was knocked back to 0!"
            2 -> "${knockedOutNames[0]} and ${knockedOutNames[1]} were knocked back to 0!"
            else -> knockedOutNames.joinToString(", ") + " were knocked back to 0!"
        }

        AlertDialog(
            onDismissRequest = onDismissKnockout,
            title = { Text("Knockout!") },
            text = { Text(knockedOutText) },
            confirmButton = {
                TextButton(onClick = onDismissKnockout) {
                    Text("Continue")
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
            val isCurrentPlayer = playerId == state.currentPlayerId
            if (player != null) {
                PlayerScoreCard(
                    player = player,
                    score = if (isCurrentPlayer) state.currentPlayerScore else state.getPlayerScore(playerId),
                    legsWon = state.getLegsWon(playerId),
                    legsToWin = legsToWin,
                    isCurrentPlayer = isCurrentPlayer,
                    lastTurnScore = state.getLastTurnScore(playerId),
                    isCountUp = state.isCountUp,
                    targetScore = if (state.isCountUp) state.targetScore else null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun CurrentTurnDisplay(
    turnNumber: Int,
    currentThrows: List<Throw>,
    selectedMultiplier: Multiplier,
    lastThrowResult: ThrowResult?,
    canUndo: Boolean,
    onUndo: () -> Unit,
    isCricket: Boolean = false,
    isRoulette: Boolean = false,
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
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Current turn throws
            Text(
                text = "Turn $turnNumber",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Throw boxes
                currentThrows.forEach { throwObj ->
                    ThrowDisplay(throwObj)
                }

                // Placeholder for remaining throws
                repeat(3 - currentThrows.size) {
                    ThrowPlaceholder()
                }

                // Turn total or status (BUST/BOUNCE/OUT!/WIN!)
                if (currentThrows.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val isBust = lastThrowResult is ThrowResult.Bust
                    val isBounce = lastThrowResult is ThrowResult.BounceBack
                    val isCheckout = lastThrowResult is ThrowResult.Checkout
                    val isCricketWin = lastThrowResult is ThrowResult.CricketWin
                    val isCricketMarks = lastThrowResult is ThrowResult.CricketMarks
                    val isRouletteHit = lastThrowResult is ThrowResult.RouletteHit
                    val displayText = when {
                        isBust -> "BUST"

                        isBounce -> "BOUNCE"

                        isCheckout -> "OUT!"

                        isCricketWin -> "WIN!"

                        isCricketMarks -> {
                            val cricketResult = lastThrowResult
                            if (cricketResult.pointsScored > 0) {
                                "+${cricketResult.pointsScored}"
                            } else if (cricketResult.marksAdded > 0) {
                                "${cricketResult.totalMarks}/3"
                            } else {
                                "-"
                            }
                        }

                        isRouletteHit -> {
                            val rouletteResult = lastThrowResult as ThrowResult.RouletteHit
                            if (rouletteResult.pointsScored > 0) {
                                "+${rouletteResult.pointsScored}"
                            } else {
                                "Miss"
                            }
                        }

                        isRoulette -> "0"

                        else -> if (isCricket) "-" else "${currentThrows.sumOf { it.score }}"
                    }
                    val displayColor = when {
                        isBust -> MaterialTheme.colorScheme.error

                        isBounce -> MaterialTheme.colorScheme.tertiary

                        isCheckout -> MaterialTheme.colorScheme.primary

                        isCricketWin -> MaterialTheme.colorScheme.primary

                        isCricketMarks && lastThrowResult.pointsScored > 0 ->
                            MaterialTheme.colorScheme.primary

                        isRouletteHit && (lastThrowResult as ThrowResult.RouletteHit).pointsScored > 0 ->
                            MaterialTheme.colorScheme.primary

                        isRouletteHit -> MaterialTheme.colorScheme.error

                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = displayColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Undo button
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Undo")
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
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
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
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "-",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
fun CheckoutPracticeHeader(
    state: ActiveGameState,
    modifier: Modifier = Modifier,
) {
    val practiceState = state.checkoutPracticeState ?: return

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Round progress
            Text(
                text = "Round ${state.checkoutRoundNumber} of ${state.checkoutTotalRounds}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Target score (large)
            Text(
                text = "Target: ${state.currentCheckoutTarget ?: 0}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Remaining score
            val remaining = state.currentPlayerScore
            if (remaining != state.currentCheckoutTarget) {
                Text(
                    text = "Remaining: $remaining",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Success count
            Text(
                text = "Checkouts: ${practiceState.successCount}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
fun CheckoutPracticeResultsDialog(
    state: ActiveGameState,
    onDismiss: () -> Unit,
) {
    val practiceState = state.checkoutPracticeState ?: return
    val totalRounds = practiceState.totalRounds
    val successCount = practiceState.successCount
    val successRate = if (totalRounds > 0) (successCount * 100) / totalRounds else 0

    val successfulRounds = practiceState.roundResults.filter { it.success }
    val avgDartsPerSuccess = if (successfulRounds.isNotEmpty()) {
        val total = successfulRounds.sumOf { it.dartsUsed } * 10 / successfulRounds.size
        "${total / 10}.${total % 10}"
    } else {
        "0.0"
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Practice Complete!") },
        text = {
            Column {
                // Summary stats
                Text(
                    text = "$successCount/$totalRounds - $successRate%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (successfulRounds.isNotEmpty()) {
                    Text(
                        text = "Avg darts per checkout: $avgDartsPerSuccess",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Round-by-round results
                Text(
                    text = "Round Results:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                practiceState.roundResults.forEachIndexed { index, result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${index + 1}. Target: ${result.target}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = if (result.success) {
                                "${result.dartsUsed} darts"
                            } else {
                                "Miss"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (result.success) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Finish")
            }
        },
    )
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
fun RouletteHeader(
    state: ActiveGameState,
    modifier: Modifier = Modifier,
) {
    val target = state.currentRouletteTarget ?: return

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Round info
            val roundInfo = when (state.session?.config?.gameType) {
                GameType.ROULETTE_ROUNDS -> {
                    val total = state.rouletteTotalRounds ?: 0
                    "Round ${state.rouletteRoundNumber} of $total"
                }

                GameType.ROULETTE_SCORE -> {
                    val targetScore = state.rouletteTargetScore ?: 0
                    "Round ${state.rouletteRoundNumber} - Target: $targetScore pts"
                }

                else -> "Round ${state.rouletteRoundNumber}"
            }
            Text(
                text = roundInfo,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Target segment (large/prominent)
            val targetDisplay = if (target == 25) "BULL" else "$target"
            Text(
                text = targetDisplay,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Text(
                text = "Hit this target!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
fun RouletteResultsDialog(
    state: ActiveGameState,
    onDismiss: () -> Unit,
) {
    val session = state.session ?: return
    val winnerName = session.winnerId?.let { state.getPlayer(it)?.name } ?: "Player"

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Roulette Complete!") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$winnerName wins!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Final scores
                Text(
                    text = "Final Scores:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                session.config.playerIds.forEach { playerId ->
                    val player = state.getPlayer(playerId)
                    val score = state.getPlayerScore(playerId)
                    val isWinner = playerId == session.winnerId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = player?.name ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            text = "$score pts",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                            color = if (isWinner) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Finish")
            }
        },
    )
}
