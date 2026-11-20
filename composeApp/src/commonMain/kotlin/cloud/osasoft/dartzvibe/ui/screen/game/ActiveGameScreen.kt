package cloud.osasoft.dartzvibe.ui.screen.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cloud.osasoft.dartzvibe.LocalAppSettings
import cloud.osasoft.dartzvibe.LocalGameRepository
import cloud.osasoft.dartzvibe.LocalPlayerRepository
import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.domain.game.CricketTurnSummary
import cloud.osasoft.dartzvibe.domain.game.ThrowResult
import cloud.osasoft.dartzvibe.domain.game.TurnResult
import cloud.osasoft.dartzvibe.ui.screen.game.components.CheckoutHint
import cloud.osasoft.dartzvibe.ui.screen.game.components.CricketInputPad
import cloud.osasoft.dartzvibe.ui.screen.game.components.CricketMinRowHeight
import cloud.osasoft.dartzvibe.ui.screen.game.components.CricketRowHeight
import cloud.osasoft.dartzvibe.ui.screen.game.components.CricketScoreboard
import cloud.osasoft.dartzvibe.ui.screen.game.components.CricketTargetKeyHeight
import cloud.osasoft.dartzvibe.ui.screen.game.components.KeypadPadding
import cloud.osasoft.dartzvibe.ui.screen.game.components.KeypadRowGap
import cloud.osasoft.dartzvibe.ui.screen.game.components.PlayerScoreCard
import cloud.osasoft.dartzvibe.ui.screen.game.components.RadialDartboard
import cloud.osasoft.dartzvibe.ui.screen.game.components.RouletteInputPad
import cloud.osasoft.dartzvibe.ui.screen.game.components.ScoreInputKeypad
import cloud.osasoft.dartzvibe.ui.screen.game.components.checkoutSlotText
import cloud.osasoft.dartzvibe.ui.screen.game.components.cricketPadHeightPx
import cloud.osasoft.dartzvibe.ui.screen.game.components.fitScoreboardRowHeight
import cloud.osasoft.dartzvibe.ui.screen.game.components.fitTextScale
import cloud.osasoft.dartzvibe.ui.screen.game.components.knockoutSlotText
import cloud.osasoft.dartzvibe.ui.screen.game.components.scoreCardTargetSuffix
import cloud.osasoft.dartzvibe.ui.screen.pushFrom
import cloud.osasoft.dartzvibe.ui.screen.settings.SettingsScreen
import cloud.osasoft.dartzvibe.ui.theme.multiplierColors
import dartzvibe.composeapp.generated.resources.Res
import dartzvibe.composeapp.generated.resources.forever_alone_bw
import org.jetbrains.compose.resources.painterResource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ActiveGameScreen(
    private val sessionId: Uuid,
) : Screen {

    // Unique per instance: the ScreenModel store is keyed by it, so a new screen never reuses an old model
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val playerRepository = LocalPlayerRepository.current
        val gameRepository = LocalGameRepository.current
        val screenModel = rememberActiveGameScreenModel(gameRepository, playerRepository, sessionId)
        val state by screenModel.state.collectAsState()
        val settings = LocalAppSettings.current
        val navigator = LocalNavigator.currentOrThrow

        ActiveGameContent(
            state = state,
            settings = settings,
            onMultiplierChange = screenModel::setMultiplier,
            onScoreSelect = screenModel::onScoreSelect,
            onMiss = screenModel::onMiss,
            onUndo = screenModel::undo,
            onUndoWin = screenModel::undoWin,
            onEndTurn = screenModel::endTurn,
            onContinueLegWon = screenModel::continueToNextLeg,
            onCloseLegWon = screenModel::closeLegWonDialog,
            // The dialog leaves with the screen: closing it first would flash the ended game
            onDismissGameComplete = { navigator.pop() },
            onDismissKnockout = screenModel::dismissKnockoutDialog,
            onAbandonGame = {
                screenModel.abandonGame()
                navigator.pop()
            },
            onSettings = { navigator.pushFrom(this@ActiveGameScreen, SettingsScreen()) },
            onBack = { navigator.pop() },
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun Screen.rememberActiveGameScreenModel(
    gameRepository: GameRepository,
    playerRepository: PlayerRepository,
    sessionId: Uuid,
): ActiveGameScreenModel = rememberScreenModel { ActiveGameScreenModel(gameRepository, playerRepository, sessionId) }

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
    onUndoWin: () -> Unit,
    onEndTurn: () -> Unit,
    onContinueLegWon: () -> Unit,
    onCloseLegWon: () -> Unit,
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // The screen's height without the bars: stable for the whole game
                val viewportHeight = maxHeight
                HeaderThenInput(
                    viewportHeight = viewportHeight,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    // Cricket: on short phones the scoreboard rows shrink so the pad fits below them
                    shrink = cricketHeaderShrink(state = state, showMultiplierButtons = settings.showMultiplierButtons),
                    header = { cricketRowHeight ->
                        GameHeader(
                            state = state,
                            showCheckoutHints = settings.showCheckoutHints,
                            showKnockoutHints = settings.showKnockoutHints,
                            cricketRowHeight = cricketRowHeight,
                            onUndo = onUndo,
                        )
                    },
                    input = { heightLeft ->
                        GameInput(
                            state = state,
                            settings = settings,
                            // Classic, Parcheesi, Checkout Practice and Cricket fit the input to the height their fixed
                            // rows leave, so short phones don't scroll; Roulette keeps main's layout
                            maxHeight = heightLeft.takeIf { state.hasCheckoutSlot || state.isCricket },
                            onMultiplierChange = onMultiplierChange,
                            onScoreSelect = onScoreSelect,
                            onMiss = onMiss,
                            onEndTurn = onEndTurn,
                        )
                    },
                )
            }
        }
    }

    // Undo on the win dialogs, hidden when the stored leg can't be rewound
    val undoWin = onUndoWin.takeIf { (state.showLegWonDialog || state.showGameCompleteDialog) && state.canUndoWin }

    // Continue swapped the next leg in under the dialog: close it only after a frame of that leg is drawn, so the
    // ended leg never shows without the dialog
    if (state.isLegWonDialogClosing) {
        LaunchedEffect(Unit) {
            repeat(2) { withFrameNanos { } }
            onCloseLegWon()
        }
    }

    // Leg won dialog
    if (state.showLegWonDialog) {
        val result = state.lastTurnResult as? TurnResult.LegWon
        val winnerName = result?.let { state.getPlayer(it.winnerId)?.name } ?: "Player"

        AlertDialog(
            // Only Continue or Undo close it
            onDismissRequest = { },
            title = { Text("Leg Won!") },
            text = { Text("$winnerName wins the leg!") },
            confirmButton = {
                TextButton(onClick = onContinueLegWon) {
                    Text("Continue")
                }
            },
            dismissButton = undoWin?.let { { UndoWinButton(onClick = it) } },
        )
    }

    // Game complete dialog
    if (state.showGameCompleteDialog) {
        if (state.isRoulette) {
            // Final scores and winner come from the state after the last turn; the screen behind keeps the round
            RouletteResultsDialog(
                state = state.resultState,
                onDismiss = onDismissGameComplete,
                onUndo = undoWin,
            )
        } else if (state.isCheckoutPractice) {
            CheckoutPracticeResultsDialog(
                state = state.resultState,
                onDismiss = onDismissGameComplete,
                onUndo = undoWin,
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
                dismissButton = undoWin?.let { { UndoWinButton(onClick = it) } },
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

/**
 * Everything above the input: scores or mode header, the current turn and (in checkout modes) the checkout slot.
 * [cricketRowHeight] set (Cricket on a short phone): the compact scoreboard with rows that tall, and the compact turn
 * card.
 */
@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun GameHeader(
    state: ActiveGameState,
    showCheckoutHints: Boolean,
    showKnockoutHints: Boolean,
    cricketRowHeight: Dp?,
    onUndo: () -> Unit,
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
                rowHeight = cricketRowHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = if (cricketRowHeight != null) 4.dp else 8.dp),
            )
        }
    } else {
        // Tighter below than elsewhere: Classic and Parcheesi also lay out the checkout slot
        PlayerScoresRow(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 6.dp, end = 8.dp, bottom = 4.dp),
        )
    }

    // Classic, Parcheesi and Checkout Practice reserve a checkout slot below the turn display
    val hasCheckoutSlot = state.hasCheckoutSlot

    // Current turn display
    // Roulette and Checkout Practice headers already show their own round progress. Behind the
    // win dialogs the engine is the one before the winning End Turn, so this is the leg's last round
    val hideRoundNumber = state.isRoulette || state.isCheckoutPractice
    CurrentTurnDisplay(
        roundNumber = if (hideRoundNumber) null else state.currentRoundNumber,
        currentThrows = state.currentTurnThrows,
        selectedMultiplier = state.selectedMultiplier,
        lastThrowResult = state.lastThrowResult,
        canUndo = state.canUndo,
        onUndo = onUndo,
        isCricket = state.session?.config?.gameMode == GameMode.CRICKET,
        cricketTurnSummary = state.cricketTurnSummary,
        rouletteTurnPoints = state.rouletteTurnPoints,
        compact = hasCheckoutSlot || cricketRowHeight != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    )

    // Checkout slot: always laid out at a fixed height in the modes that can have a checkout, so the
    // input below never moves when a suggestion appears, changes or disappears
    if (hasCheckoutSlot) {
        // The checkout route when the turn can finish, else (Parcheesi) a knockout suggestion
        val slotText = when (val hint = state.checkoutSlotHint(showCheckoutHints, showKnockoutHints)) {
            is CheckoutSlotHint.Checkout -> checkoutSlotText(hint.options)

            is CheckoutSlotHint.Knockout -> knockoutSlotText(hint.routes) { id ->
                state.getPlayer(id)?.name ?: "Player"
            }

            null -> null
        }
        CheckoutHint(
            text = slotText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 4.dp, end = 8.dp),
        )
    }
}

/** The score input for the mode and settings, or End Turn once the turn is complete. */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun GameInput(
    state: ActiveGameState,
    settings: AppSettingsData,
    maxHeight: Dp?,
    onMultiplierChange: (Multiplier) -> Unit,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onMiss: () -> Unit,
    onEndTurn: () -> Unit,
) {
    // Score input keypad (hidden when turn is complete)
    if (state.canThrow) {
        if (state.isRoulette) {
            // Only the current target scores in Roulette: quick Miss/Hit/Double/Triple pad,
            // regardless of the keypad settings
            state.currentRouletteTarget?.let { target ->
                RouletteInputPad(
                    target = target,
                    onScoreSelect = onScoreSelect,
                    onMiss = onMiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else if (state.isCricket) {
            // Only the Cricket numbers, Bull and Miss score in Cricket: its own pad, regardless of the radial setting
            state.cricketState?.let { cricketState ->
                CricketInputPad(
                    segments = cricketState.segments.segments,
                    selectedMultiplier = state.selectedMultiplier,
                    canThrow = state.canThrow,
                    showMultiplierButtons = settings.showMultiplierButtons,
                    onMultiplierChange = onMultiplierChange,
                    onScoreSelect = onScoreSelect,
                    onMiss = onMiss,
                    modifier = Modifier.fillMaxWidth(),
                    maxHeight = maxHeight,
                )
            }
        } else if (settings.useRadialKeypad) {
            RadialDartboard(
                selectedMultiplier = state.selectedMultiplier,
                canThrow = state.canThrow,
                showMultiplierButtons = settings.showMultiplierButtons,
                swipeMultiplierMode = settings.swipeMultiplierMode,
                onMultiplierChange = onMultiplierChange,
                onScoreSelect = onScoreSelect,
                onMiss = onMiss,
                modifier = Modifier.fillMaxWidth(),
                maxHeight = maxHeight,
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
                maxHeight = maxHeight,
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

/**
 * How the header may shrink on a short screen: its [rows] flexible rows (the Cricket scoreboard's) shrink, down to
 * [minRowHeight], so the input gets [inputHeight].
 */
private data class HeaderShrink(
    val rows: Int,
    val inputHeight: Dp,
    val minRowHeight: Dp,
)

/**
 * Cricket's [HeaderShrink]: the scoreboard's rows give way so the Cricket pad gets keys [CricketTargetKeyHeight]
 * tall. Null in the other modes, whose header never shrinks.
 */
@Composable
private fun cricketHeaderShrink(
    state: ActiveGameState,
    showMultiplierButtons: Boolean,
): HeaderShrink? {
    val cricketState = state.cricketState?.takeIf { state.isCricket } ?: return null
    val hintLine = MaterialTheme.typography.labelSmall.lineHeight
    val inputHeight = with(LocalDensity.current) {
        // The S/D/T row (MultiplierButton is 40 dp), or the swipe hint (one label line under 4 dp of padding)
        val hintHeight = (if (hintLine.isSp) hintLine.toDp() else 0.dp) + 4.dp
        cricketPadHeightPx(
            keyHeightPx = CricketTargetKeyHeight.roundToPx(),
            paddingPx = KeypadPadding.roundToPx(),
            gapPx = KeypadRowGap.roundToPx(),
            topRowPx = if (showMultiplierButtons) 40.dp.roundToPx() else 0,
            bottomRowPx = if (showMultiplierButtons) 0 else hintHeight.roundToPx(),
        ).toDp()
    }
    // Never below the rows' own text (the marks' titleSmall in a box 2 dp under the row, the labelLarge label), so a
    // larger font scale can't clip them
    val minRowHeight = with(LocalDensity.current) {
        val marksLine = MaterialTheme.typography.titleSmall.lineHeight
        val labelLine = MaterialTheme.typography.labelLarge.lineHeight
        maxOf(
            CricketMinRowHeight,
            (if (marksLine.isSp) marksLine.toDp() else 0.dp) + 2.dp,
            if (labelLine.isSp) labelLine.toDp() else 0.dp,
        )
    }
    return HeaderShrink(
        rows = cricketState.segments.segments.size,
        inputHeight = inputHeight,
        minRowHeight = minRowHeight,
    )
}

/**
 * Stacks [header] and [input] like a Column (inside the screen's vertical scroll) and passes [input] the height the
 * header leaves of [viewportHeight]. The header is measured first and on its own, so that height depends only on
 * the viewport and the header's fixed rows, never on what the input shows (board, keypad or End Turn).
 *
 * [header] gets null for its normal layout. With [shrink], if that leaves the input less than its height, the header
 * gets a row height instead: the compact layout, with its flexible rows as tall as lets the input fit (see
 * [fitScoreboardRowHeight]), between [CricketMinRowHeight] and [CricketRowHeight].
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun HeaderThenInput(
    viewportHeight: Dp,
    header: @Composable (rowHeight: Dp?) -> Unit,
    input: @Composable (heightLeft: Dp) -> Unit,
    modifier: Modifier = Modifier,
    shrink: HeaderShrink? = null,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        val viewportPx = viewportHeight.roundToPx()
        val measureHeader = { slot: HeaderSlot, rowHeight: Dp? ->
            subcompose(slot) { header(rowHeight) }.map { it.measure(childConstraints) }
        }
        val fullHeader = measureHeader(HeaderSlot.HEADER, null)
        val inputPx = shrink?.inputHeight?.roundToPx()
        val headerPlaceables = if (inputPx == null || viewportPx - fullHeader.sumOf { it.height } >= inputPx) {
            fullHeader
        } else {
            // Measure the compact header at the smallest rows, then size the rows to what the input leaves
            val minRowPx = shrink.minRowHeight.roundToPx()
            val atMinRows = measureHeader(HeaderSlot.HEADER_AT_MIN_ROWS, minRowPx.toDp())
            val rowPx = fitScoreboardRowHeight(
                availablePx = viewportPx,
                fixedPx = atMinRows.sumOf { it.height } - shrink.rows * minRowPx,
                padPx = inputPx,
                rows = shrink.rows,
                minPx = minRowPx,
                nominalPx = CricketRowHeight.roundToPx(),
            )
            if (rowPx == minRowPx) atMinRows else measureHeader(HeaderSlot.HEADER_FITTED, rowPx.toDp())
        }
        val headerHeight = headerPlaceables.sumOf { it.height }
        val heightLeft = (viewportPx - headerHeight).coerceAtLeast(0).toDp()
        val inputPlaceables = subcompose(HeaderSlot.INPUT) { input(heightLeft) }.map { it.measure(childConstraints) }
        val placeables = headerPlaceables + inputPlaceables
        val height = constraints.constrainHeight(placeables.sumOf { it.height })

        layout(constraints.maxWidth, height) {
            var y = 0
            placeables.forEach { placeable ->
                placeable.place(x = 0, y = y)
                y += placeable.height
            }
        }
    }
}

private enum class HeaderSlot { HEADER, HEADER_AT_MIN_ROWS, HEADER_FITTED, INPUT }

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
                    lastTurn = state.getLastTurn(playerId),
                    isCountUp = state.isCountUp,
                    targetSuffix = scoreCardTargetSuffix(
                        gameMode = session.config.gameMode,
                        gameType = session.config.gameType,
                        rouletteTargetScore = state.rouletteTargetScore,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun CurrentTurnDisplay(
    roundNumber: Int?,
    currentThrows: List<Throw>,
    selectedMultiplier: Multiplier,
    lastThrowResult: ThrowResult?,
    canUndo: Boolean,
    onUndo: () -> Unit,
    isCricket: Boolean = false,
    cricketTurnSummary: CricketTurnSummary? = null,
    rouletteTurnPoints: Int? = null,
    // Tighter vertical spacing, used where the checkout slot also needs room above the input
    compact: Boolean = false,
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
                .padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Current round (hidden when the mode header already shows round progress)
            roundNumber?.let { round ->
                Text(
                    text = "Round $round",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
            }

            // At least as tall as the turn total that appears with the first dart: above 1.0 font scale its line
            // is taller than the 36 dp chips, and the input below would drop on the first dart of every turn
            val turnTotalLineHeight = with(LocalDensity.current) {
                MaterialTheme.typography.headlineMedium.lineHeight
                    .takeIf { it.isSp }
                    ?.toDp() ?: 36.dp
            }
            Row(
                // Cricket: two equal halves either side of the dart boxes keep them centred and still, with the
                // summary in the right half, however long it gets
                modifier = (if (isCricket) Modifier.fillMaxWidth() else Modifier).heightIn(min = turnTotalLineHeight),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isCricket) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Throw boxes
                currentThrows.forEach { throwObj ->
                    ThrowDisplay(throwObj)
                }

                // Placeholder for remaining throws
                repeat(3 - currentThrows.size) {
                    ThrowPlaceholder()
                }

                // Turn total or status (BUST/BOUNCE/OUT!/WIN!). Cricket keeps its half even before the first dart, so
                // the dart boxes don't slide sideways as the summary appears and grows
                if (isCricket || currentThrows.isNotEmpty()) {
                    if (!isCricket) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    val isBust = lastThrowResult is ThrowResult.Bust
                    val isBounce = lastThrowResult is ThrowResult.BounceBack
                    val isCheckout = lastThrowResult is ThrowResult.Checkout
                    val isCricketWin = lastThrowResult is ThrowResult.CricketWin
                    val displayText = when {
                        isBust -> "BUST"

                        isBounce -> "BOUNCE"

                        isCheckout -> "OUT!"

                        isCricketWin -> "WIN!"

                        // The whole turn's marks and points, not just the last dart's
                        cricketTurnSummary != null -> cricketTurnSummaryText(cricketTurnSummary)

                        // Roulette shows the whole turn's points, not just the last dart's
                        rouletteTurnPoints != null -> "+$rouletteTurnPoints"

                        else -> "${currentThrows.sumOf { it.score }}"
                    }
                    val displayColor = when {
                        isBust -> MaterialTheme.colorScheme.error

                        isBounce -> MaterialTheme.colorScheme.tertiary

                        isCheckout -> MaterialTheme.colorScheme.primary

                        isCricketWin -> MaterialTheme.colorScheme.primary

                        cricketTurnSummary != null && cricketTurnSummary.points > 0 ->
                            MaterialTheme.colorScheme.primary

                        rouletteTurnPoints != null && rouletteTurnPoints > 0 -> MaterialTheme.colorScheme.primary

                        rouletteTurnPoints != null -> MaterialTheme.colorScheme.error

                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    BoxWithConstraints(
                        modifier = if (isCricket) Modifier.weight(1f).padding(start = 8.dp) else Modifier,
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        val baseStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        // Cricket: one size for every state, shrunk once so the widest summary fits its half; the
                        // text then never changes size from dart to dart
                        val textStyle = if (isCricket) {
                            val textMeasurer = rememberTextMeasurer()
                            val maxWidthPx = constraints.maxWidth
                            remember(maxWidthPx, baseStyle) {
                                val widest = textMeasurer.measure(
                                    text = CRICKET_WIDEST_SUMMARY,
                                    style = baseStyle,
                                    maxLines = 1,
                                    softWrap = false,
                                ).size.width
                                baseStyle.copy(fontSize = baseStyle.fontSize * fitTextScale(widest, maxWidthPx))
                            }
                        } else {
                            baseStyle
                        }
                        if (currentThrows.isNotEmpty()) {
                            Text(
                                text = displayText,
                                style = textStyle,
                                color = displayColor,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            // Undo button
            Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
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

/** The widest Cricket turn summary (three scoring triples), which sets the summary's one font size. */
private const val CRICKET_WIDEST_SUMMARY = "9 marks · +180"

/** The Cricket turn slot text: "-", "1 mark", "4 marks" or "6 marks · +60". */
internal fun cricketTurnSummaryText(summary: CricketTurnSummary): String {
    if (summary.marks == 0) return "-"
    val marks = if (summary.marks == 1) "1 mark" else "${summary.marks} marks"
    return if (summary.points > 0) "$marks · +${summary.points}" else marks
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ThrowDisplay(throwObj: Throw) {
    val displayText = when {
        throwObj.segment == 0 -> "Miss"
        throwObj.segment == 25 && throwObj.multiplier == Multiplier.DOUBLE -> "BULL"
        else -> "${throwObj.multiplier.name.first()}${throwObj.segment}"
    }

    // Doubles/triples use the shared multiplier accents (as in GameDetail and the keypad); singles use
    // secondaryContainer because surfaceVariant would vanish on this card
    val colorScheme = MaterialTheme.colorScheme
    val multiplierColors = MaterialTheme.multiplierColors
    val (backgroundColor, textColor) = when (throwObj.multiplier) {
        Multiplier.SINGLE -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        Multiplier.DOUBLE -> multiplierColors.double to multiplierColors.onDouble
        Multiplier.TRIPLE -> multiplierColors.triple to multiplierColors.onTriple
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
            // Hidden at 0: a final checkout stays on screen behind Practice Complete, the darts row shows OUT!
            // Hidden lines stay laid out (transparent), so the header keeps its height and the input below never moves
            val remaining = state.currentPlayerScore
            val showRemaining = remaining != state.currentCheckoutTarget && remaining != 0
            Text(
                text = "Remaining: $remaining",
                style = MaterialTheme.typography.titleMedium,
                // Hidden: keep it out of TalkBack too
                modifier = if (showRemaining) Modifier else Modifier.clearAndSetSemantics { },
                color = if (showRemaining) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    Color.Transparent
                },
            )

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
    onUndo: (() -> Unit)?,
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
        dismissButton = onUndo?.let { { UndoWinButton(onClick = it) } },
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
    onUndo: (() -> Unit)?,
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
        dismissButton = onUndo?.let { { UndoWinButton(onClick = it) } },
    )
}

/** Undo action on the leg-won / game-over dialogs: reopens the winning turn with its last dart in place. */
@Suppress("ktlint:standard:function-naming")
@Composable
fun UndoWinButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("Undo")
    }
}
