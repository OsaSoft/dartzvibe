package cloud.osasoft.dartzvibe.ui.screen.gamedetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cloud.osasoft.dartzvibe.LocalGameRepository
import cloud.osasoft.dartzvibe.LocalPlayerRepository
import cloud.osasoft.dartzvibe.data.model.CheckoutRoundResult
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.domain.history.CricketTurnEffect
import cloud.osasoft.dartzvibe.domain.history.DartOutcome
import cloud.osasoft.dartzvibe.domain.history.ParcheesiKnockout
import cloud.osasoft.dartzvibe.domain.history.cricketTurnEffects
import cloud.osasoft.dartzvibe.domain.history.historyDartDescription
import cloud.osasoft.dartzvibe.domain.history.historyDartLabel
import cloud.osasoft.dartzvibe.domain.history.parcheesiLegHistory
import cloud.osasoft.dartzvibe.domain.history.rouletteTurnContexts
import cloud.osasoft.dartzvibe.ui.theme.multiplierColors
import cloud.osasoft.dartzvibe.util.formatDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class GameDetailScreen(
    private val gameId: Uuid,
) : Screen {

    @Composable
    override fun Content() {
        val playerRepository = LocalPlayerRepository.current
        val gameRepository = LocalGameRepository.current
        val screenModel = rememberGameDetailScreenModel(playerRepository, gameRepository, gameId)
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        GameDetailContent(
            state = state,
            onBack = { navigator.pop() },
            onToggleLeg = screenModel::toggleLegExpansion,
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun rememberGameDetailScreenModel(
    playerRepository: PlayerRepository,
    gameRepository: GameRepository,
    gameId: Uuid,
): GameDetailScreenModel = remember {
    GameDetailScreenModel(playerRepository, gameRepository, gameId)
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun GameDetailContent(
    state: GameDetailState,
    onBack: () -> Unit,
    onToggleLeg: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Details") },
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
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                state.session != null -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            GameSummaryHeader(
                                session = state.session,
                                players = state.players,
                            )
                        }

                        itemsIndexed(
                            state.session.legs,
                            key = { index, _ -> index },
                        ) { index, leg ->
                            LegSection(
                                legIndex = index,
                                leg = leg,
                                session = state.session,
                                players = state.players,
                                isExpanded = index in state.expandedLegs,
                                onToggle = { onToggleLeg(index) },
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
private fun GameSummaryHeader(
    session: GameSession,
    players: Map<Uuid, Player>,
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
                .padding(16.dp),
        ) {
            // Players row with winner trophy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                session.config.playerIds.forEachIndexed { index, playerId ->
                    val player = players[playerId]
                    val isWinner = playerId == session.winnerId

                    if (index > 0) {
                        Text(
                            text = " vs ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isWinner) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = "Winner",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = player?.name ?: "Unknown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legs won display
            if (session.config.legsToWin > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    session.config.playerIds.forEachIndexed { index, playerId ->
                        val legsWon = session.legs.count { it.winnerId == playerId }
                        if (index > 0) {
                            Text(
                                text = " - ",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            text = legsWon.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Game info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = session.config.displayDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = when (session.status) {
                            GameStatus.COMPLETED -> "Completed"
                            GameStatus.ABANDONED -> "Abandoned"
                            GameStatus.IN_PROGRESS -> "In Progress"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = formatDate(session.startedAt),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Settings row
            val settings = buildList {
                if (session.config.doubleIn) add("Double In")
                if (session.config.doubleOut) add("Double Out")
            }
            if (settings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = settings.joinToString(" | "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun LegSection(
    legIndex: Int,
    leg: Leg,
    session: GameSession,
    players: Map<Uuid, Player>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // Header row (always visible, clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Leg ${legIndex + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    leg.winnerId?.let { winnerId ->
                        val winner = players[winnerId]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = "Winner",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = winner?.name ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${leg.playerTurns.size} turns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

            // Expanded content (turns)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (session.config.gameMode) {
                        GameMode.CRICKET -> CricketLegContent(leg, session, players)

                        GameMode.CHECKOUT_PRACTICE -> CheckoutPracticeLegContent(leg)

                        GameMode.ROULETTE -> RouletteLegContent(leg, session, players)

                        GameMode.CLASSIC ->
                            leg.playerTurns.forEach { turn ->
                                TurnRow(
                                    turn = turn,
                                    playerName = players[turn.playerId]?.name ?: "Unknown",
                                )
                            }

                        GameMode.PARCHEESI -> ParcheesiLegContent(leg, players)
                    }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun TurnRow(
    turn: Turn,
    playerName: String,
    showCheckout: Boolean = true,
    showBounce: Boolean = false,
    knockouts: List<KnockoutDisplay> = emptyList(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (turn.isBust) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
    ) {
        TurnRowMain(
            turn = turn,
            playerName = playerName,
            showCheckout = showCheckout,
            showBounce = showBounce,
        )
        if (knockouts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            KnockoutLine(knockouts = knockouts)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun TurnRowMain(
    turn: Turn,
    playerName: String,
    showCheckout: Boolean,
    showBounce: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerNameCell(playerName = playerName, modifier = Modifier.weight(1f))

        ThrowBadges(darts = turn.throws, outcomes = null, modifier = Modifier.weight(1.5f))

        // Score column
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(0.8f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (turn.isBust) {
                    Text(
                        text = "BUST",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                } else if (showBounce && turn.isBounce) {
                    Text(
                        text = "BOUNCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                    )
                } else if (showCheckout && turn.scoreAfterTurn == 0) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Checkout",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = turn.totalScore.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                } else {
                    Text(
                        text = turn.totalScore.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = "${turn.scoreBeforeTurn} -> ${turn.scoreAfterTurn}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One dart in a history row.
 *
 * Without an [outcome] the badge is colored by multiplier (Classic, Checkout Practice). With one,
 * it is emphasized by what the dart achieved instead: [DartOutcome.HIT] gets a solid green fill,
 * anything else an outlined, faded look. The multiplier then lives in the label ("T7", "D7", "7").
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun ThrowBadge(dart: Throw?, outcome: DartOutcome? = null) {
    val colorScheme = MaterialTheme.colorScheme
    val multiplierColors = MaterialTheme.multiplierColors
    val shape = RoundedCornerShape(4.dp)

    val style = when {
        dart == null -> BadgeStyle(
            background = colorScheme.surfaceVariant,
            text = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )

        outcome != null -> when (outcome) {
            DartOutcome.HIT -> BadgeStyle(background = colorScheme.secondary, text = colorScheme.onSecondary)

            DartOutcome.NO_EFFECT, DartOutcome.MISS -> BadgeStyle(
                background = Color.Transparent,
                text = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                // ~3.4:1 against the row in light and ~3.5:1 in dark, so the chip edge stays visible
                border = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }

        else -> when (dart.multiplier) {
            Multiplier.SINGLE -> BadgeStyle(
                background = colorScheme.surfaceVariant,
                text = colorScheme.onSurfaceVariant,
            )

            Multiplier.DOUBLE -> BadgeStyle(background = multiplierColors.double, text = multiplierColors.onDouble)

            Multiplier.TRIPLE -> BadgeStyle(background = multiplierColors.triple, text = multiplierColors.onTriple)
        }
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(style.background)
            .then(
                if (style.border != null) Modifier.border(1.dp, style.border, shape) else Modifier,
            )
            .then(
                if (dart != null && outcome != null) {
                    Modifier.clearAndSetSemantics { contentDescription = historyDartDescription(dart, outcome) }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (dart != null && outcome != null) historyDartLabel(dart) else formatThrow(dart),
            style = MaterialTheme.typography.labelMedium,
            color = style.text,
            fontWeight = if (outcome == DartOutcome.HIT || outcome == null) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private data class BadgeStyle(
    val background: Color,
    val text: Color,
    val border: Color? = null,
)

/** Three dart slots, padded with empty badges for turns with fewer than 3 darts. */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun ThrowBadges(
    darts: List<Throw>,
    outcomes: List<DartOutcome>?,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        darts.forEachIndexed { index, dart -> ThrowBadge(dart, outcomes?.getOrNull(index)) }
        repeat(3 - darts.size) { ThrowBadge(null) }
    }
}

/** A player's initial in a filled circle, as used by every turn row. */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun PlayerNameCell(playerName: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = playerName.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = playerName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Names of the players a knockout reset, plus the score each one lost. */
private data class KnockoutDisplay(
    val playerName: String,
    val scoreBefore: Int,
)

@OptIn(ExperimentalUuidApi::class)
private fun ParcheesiKnockout.toDisplay(players: Map<Uuid, Player>): KnockoutDisplay = KnockoutDisplay(
    playerName = players[playerId]?.name ?: "Unknown",
    scoreBefore = scoreBefore,
)

/** "KO Bob" chip followed by each reset, e.g. "Bob 60 -> 0". */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun KnockoutLine(knockouts: List<KnockoutDisplay>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "KO ${knockouts.joinToString(", ") { it.playerName }}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = knockouts.joinToString(" · ") { "${it.playerName} ${it.scoreBefore} -> 0" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatThrow(dart: Throw?): String = when {
    dart == null -> "-"
    dart.segment == 50 -> "Bull"
    dart.segment == 25 -> "25"
    dart.multiplier == Multiplier.TRIPLE -> "T${dart.segment}"
    dart.multiplier == Multiplier.DOUBLE -> "D${dart.segment}"
    else -> dart.segment.toString()
}

private fun segmentLabel(segment: Int): String = when (segment) {
    50 -> "Bull"
    25 -> "25"
    else -> segment.toString()
}

private fun marksSymbol(marks: Int): String = when (marks) {
    0 -> "·"
    1 -> "/"
    2 -> "X"
    else -> "✓"
}

// ---- Cricket: per-leg mark grid (segments x players) + turn-by-turn list ----

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun CricketLegContent(
    leg: Leg,
    session: GameSession,
    players: Map<Uuid, Player>,
) {
    CricketMarksGrid(leg, session, players)
    CricketTurnList(leg, session, players)
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun CricketMarksGrid(
    leg: Leg,
    session: GameSession,
    players: Map<Uuid, Player>,
) {
    val state = leg.cricketState
    val segments = state?.segments?.segments ?: session.config.cricketSegments?.segments.orEmpty()
    val playerIds = session.config.playerIds

    if (state == null || segments.isEmpty()) {
        Text(
            text = "No marks recorded",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    // Header row: segment column + player columns
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.weight(0.8f))
        playerIds.forEach { pid ->
            Text(
                text = players[pid]?.name?.take(6) ?: "?",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }

    segments.forEach { seg ->
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = segmentLabel(seg),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.8f),
            )
            playerIds.forEach { pid ->
                val ps = state.getPlayerState(pid)
                val closed = ps.isClosed(seg)
                Text(
                    text = marksSymbol(ps.getMarks(seg)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (closed) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    color = if (closed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Points row
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Points",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.8f),
        )
        playerIds.forEach { pid ->
            Text(
                text = state.getPlayerState(pid).points.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun CricketTurnList(
    leg: Leg,
    session: GameSession,
    players: Map<Uuid, Player>,
) {
    val effects = remember(leg, session.config) { cricketTurnEffects(leg, session.config) }
    if (effects.isEmpty()) return

    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Turns",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    effects.forEach { effect ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerNameCell(
                playerName = players[effect.turn.playerId]?.name ?: "Unknown",
                modifier = Modifier.weight(1f),
            )
            ThrowBadges(
                darts = effect.darts.map { it.dart },
                outcomes = effect.darts.map { it.outcome },
                modifier = Modifier.weight(1.5f),
            )
            CricketTurnEffectCell(effect = effect, modifier = Modifier.weight(0.8f))
        }
    }
}

/** Right-hand summary of a Cricket turn: "+40 pts", "+3 marks" and any numbers it closed. */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun CricketTurnEffectCell(effect: CricketTurnEffect, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        if (effect.marksAdded == 0 && effect.pointsScored == 0) {
            Text(
                text = "no effect",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        if (effect.pointsScored > 0) {
            Text(
                text = "+${effect.pointsScored} pts",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (effect.marksAdded > 0) {
            Text(
                text = if (effect.marksAdded == 1) "+1 mark" else "+${effect.marksAdded} marks",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (effect.closedSegments.isNotEmpty()) {
            Text(
                text = "closed ${effect.closedSegments.joinToString(", ") { segmentLabel(it) }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

// ---- Checkout practice: per-round pass/fail list ----

@Suppress("ktlint:standard:function-naming")
@Composable
private fun CheckoutPracticeLegContent(leg: Leg) {
    val results = leg.checkoutPracticeState?.roundResults.orEmpty()
    if (results.isEmpty()) {
        Text(
            text = "No rounds recorded",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    results.forEach { result ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
        ) {
            CheckoutRoundSummary(result)
            if (result.throws.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ThrowBadges(darts = result.throws, outcomes = null)
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun CheckoutRoundSummary(result: CheckoutRoundResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Target ${result.target}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (result.success) "PASS" else "FAIL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (result.success) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${result.dartsUsed} darts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Roulette: round-by-round target + hit/miss per dart ----

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun RouletteLegContent(
    leg: Leg,
    session: GameSession,
    players: Map<Uuid, Player>,
) {
    val contexts = remember(leg, session.config) { rouletteTurnContexts(leg, session.config) }

    contexts.forEach { context ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = players[context.turn.playerId]?.name ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "R${context.round} • target ${segmentLabel(context.target)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ThrowBadges(
                darts = context.turn.throws,
                outcomes = context.dartOutcomes,
                modifier = Modifier.weight(1.5f),
            )
            Text(
                text = "+${context.points}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (context.points > 0) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.End,
            )
        }
    }
}

// ---- Parcheesi: turns with bounces and knockouts ----

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ParcheesiLegContent(
    leg: Leg,
    players: Map<Uuid, Player>,
) {
    val history = remember(leg) { parcheesiLegHistory(leg) }

    history.turns.forEach { context ->
        TurnRow(
            turn = context.turn,
            playerName = players[context.turn.playerId]?.name ?: "Unknown",
            showCheckout = false,
            showBounce = true,
            knockouts = context.knockouts.map { it.toDisplay(players) },
        )
    }

    if (history.unfinishedTurnKnockouts.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
        ) {
            Text(
                text = "Unfinished turn",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            KnockoutLine(knockouts = history.unfinishedTurnKnockouts.map { it.toDisplay(players) })
        }
    }
}
