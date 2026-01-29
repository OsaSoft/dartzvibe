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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.ui.theme.AvatarColors
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class NewGameScreen : Screen {

    @Composable
    override fun Content() {
        val playerRepository = LocalPlayerRepository.current
        val gameRepository = LocalGameRepository.current
        val screenModel = rememberNewGameScreenModel(playerRepository, gameRepository)
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        // Navigate to active game when session is created
        LaunchedEffect(state.createdSession) {
            state.createdSession?.let { session ->
                navigator.replace(ActiveGameScreen(session.id))
            }
        }

        NewGameContent(
            state = state,
            onGameTypeChange = screenModel::setGameType,
            onGameModeChange = screenModel::setGameMode,
            onDoubleInChange = screenModel::setDoubleIn,
            onDoubleOutChange = screenModel::setDoubleOut,
            onLegsToWinChange = screenModel::setLegsToWin,
            onPlayerToggle = screenModel::togglePlayerSelection,
            onMoveUp = screenModel::movePlayerUp,
            onMoveDown = screenModel::movePlayerDown,
            onStartGame = screenModel::startGame,
            onBack = { navigator.pop() },
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun rememberNewGameScreenModel(
    playerRepository: PlayerRepository,
    gameRepository: GameRepository,
): NewGameScreenModel = remember { NewGameScreenModel(playerRepository, gameRepository) }

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun NewGameContent(
    state: NewGameState,
    onGameTypeChange: (GameType) -> Unit,
    onGameModeChange: (GameMode) -> Unit,
    onDoubleInChange: (Boolean) -> Unit,
    onDoubleOutChange: (Boolean) -> Unit,
    onLegsToWinChange: (Int) -> Unit,
    onPlayerToggle: (Uuid) -> Unit,
    onMoveUp: (Uuid) -> Unit,
    onMoveDown: (Uuid) -> Unit,
    onStartGame: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Game") },
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
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Game Mode Selection
                GameModeSection(
                    selectedMode = state.gameMode,
                    targetScore = state.gameType.startingScore,
                    onModeChange = onGameModeChange,
                )

                // Game Type Selection
                GameTypeSection(
                    selectedType = state.gameType,
                    selectedMode = state.gameMode,
                    onTypeChange = onGameTypeChange,
                )

                // Game Options
                GameOptionsSection(
                    doubleIn = state.doubleIn,
                    doubleOut = state.doubleOut,
                    legsToWin = state.legsToWin,
                    legsOptions = state.legsOptions,
                    showDoubleOptions = state.gameMode != GameMode.CRICKET,
                    onDoubleInChange = onDoubleInChange,
                    onDoubleOutChange = onDoubleOutChange,
                    onLegsToWinChange = onLegsToWinChange,
                )

                // Player Selection
                PlayerSelectionSection(
                    availablePlayers = state.availablePlayers,
                    selectedPlayerIds = state.selectedPlayerIds,
                    onPlayerToggle = onPlayerToggle,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                )

                // Error message
                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Start Game Button
                Button(
                    onClick = onStartGame,
                    enabled = state.isValid && !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Game", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTypeSection(
    selectedType: GameType,
    selectedMode: GameMode,
    onTypeChange: (GameType) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Game Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (selectedMode == GameMode.CRICKET) {
                    // Cricket mode shows "Regular" chip only
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("Regular") },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                } else {
                    // Classic/Parcheesi modes show 501/301 options
                    GameType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { onTypeChange(type) },
                            label = { Text(type.displayName) },
                            leadingIcon = if (selectedType == type) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModeSection(
    selectedMode: GameMode,
    targetScore: Int,
    onModeChange: (GameMode) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Game Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilterChip(
                    selected = selectedMode == GameMode.CLASSIC,
                    onClick = { onModeChange(GameMode.CLASSIC) },
                    label = { Text("Classic") },
                    leadingIcon = if (selectedMode == GameMode.CLASSIC) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                FilterChip(
                    selected = selectedMode == GameMode.PARCHEESI,
                    onClick = { onModeChange(GameMode.PARCHEESI) },
                    label = { Text("Parcheesi") },
                    leadingIcon = if (selectedMode == GameMode.PARCHEESI) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                FilterChip(
                    selected = selectedMode == GameMode.CRICKET,
                    onClick = { onModeChange(GameMode.CRICKET) },
                    label = { Text("Cricket") },
                    leadingIcon = if (selectedMode == GameMode.CRICKET) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val modeDescription = when (selectedMode) {
                GameMode.CLASSIC -> "Count down from $targetScore to 0"

                GameMode.PARCHEESI ->
                    "Count up to $targetScore. Match another player's score to knock them back to 0!"

                GameMode.CRICKET ->
                    "Close 15-20 and Bull. Score points on closed numbers!"
            }
            Text(
                text = modeDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun GameOptionsSection(
    doubleIn: Boolean,
    doubleOut: Boolean,
    legsToWin: Int,
    legsOptions: List<Int>,
    showDoubleOptions: Boolean,
    onDoubleInChange: (Boolean) -> Unit,
    onDoubleOutChange: (Boolean) -> Unit,
    onLegsToWinChange: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Game Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (showDoubleOptions) {
                // Double In
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Double In", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Must start with a double",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = doubleIn, onCheckedChange = onDoubleInChange)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Double Out
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Double Out", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Must finish with a double",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = doubleOut, onCheckedChange = onDoubleOutChange)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Legs to Win
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Legs to Win", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "First to $legsToWin leg${if (legsToWin > 1) "s" else ""} wins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LegsDropdown(
                    selectedLegs = legsToWin,
                    options = legsOptions,
                    onLegsChange = onLegsToWinChange,
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun LegsDropdown(
    selectedLegs: Int,
    options: List<Int>,
    onLegsChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedCard(
            onClick = { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$selectedLegs")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { legs ->
                DropdownMenuItem(
                    text = { Text("$legs") },
                    onClick = {
                        onLegsChange(legs)
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
fun PlayerSelectionSection(
    availablePlayers: List<Player>,
    selectedPlayerIds: List<Uuid>,
    onPlayerToggle: (Uuid) -> Unit,
    onMoveUp: (Uuid) -> Unit,
    onMoveDown: (Uuid) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Players (${selectedPlayerIds.size}/4)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Select 2-4 players. Order determines throwing order.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (availablePlayers.isEmpty()) {
                Text(
                    text = "No players available. Add some players first!",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                // Selected players (reorderable)
                if (selectedPlayerIds.isNotEmpty()) {
                    Text(
                        text = "Throwing Order:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    selectedPlayerIds.forEachIndexed { index, playerId ->
                        val player = availablePlayers.find { it.id == playerId }
                        if (player != null) {
                            SelectedPlayerRow(
                                player = player,
                                position = index + 1,
                                canMoveUp = index > 0,
                                canMoveDown = index < selectedPlayerIds.size - 1,
                                onMoveUp = { onMoveUp(playerId) },
                                onMoveDown = { onMoveDown(playerId) },
                                onRemove = { onPlayerToggle(playerId) },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Available players (not selected)
                val unselectedPlayers = availablePlayers.filter { it.id !in selectedPlayerIds }
                if (unselectedPlayers.isNotEmpty()) {
                    Text(
                        text = "Available Players:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    unselectedPlayers.forEach { player ->
                        AvailablePlayerRow(
                            player = player,
                            isEnabled = selectedPlayerIds.size < 4,
                            onSelect = { onPlayerToggle(player.id) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SelectedPlayerRow(
    player: Player,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Position number
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$position",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Player avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AvatarColors.getColor(player.avatarColor)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = player.name.first().uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Player name
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            // Reorder buttons
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
            }

            // Remove checkbox
            Checkbox(
                checked = true,
                onCheckedChange = { onRemove() },
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun AvailablePlayerRow(
    player: Player,
    isEnabled: Boolean,
    onSelect: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (isEnabled) onSelect else ({}),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Player avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AvatarColors.getColor(player.avatarColor)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = player.name.first().uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Player name
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
            )

            // Add checkbox
            Checkbox(
                checked = false,
                enabled = isEnabled,
                onCheckedChange = { onSelect() },
            )
        }
    }
}
