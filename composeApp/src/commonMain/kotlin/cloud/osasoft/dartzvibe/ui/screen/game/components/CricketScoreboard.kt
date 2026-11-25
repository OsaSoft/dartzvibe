package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.ui.theme.AvatarColors
import cloud.osasoft.dartzvibe.ui.theme.brandColors
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The Cricket marks and points of every player. [rowHeight] null: the layout of tall phones. Set (short phones): a
 * compact layout, with tighter header and points rows and every segment row exactly [rowHeight] tall, so the pad below
 * fits without scrolling. It is worked out from the viewport only, so the scoreboard never changes height mid-game.
 */
@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalUuidApi::class)
@Composable
fun CricketScoreboard(
    segments: CricketSegments,
    playerIds: List<Uuid>,
    players: Map<Uuid, Player>,
    cricketState: CricketState,
    currentPlayerId: Uuid?,
    modifier: Modifier = Modifier,
    rowHeight: Dp? = null,
) {
    val compact = rowHeight != null
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 6.dp else 8.dp),
        ) {
            // Header row with player names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Segment column header
                Box(
                    modifier = Modifier.width(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                // Player headers
                playerIds.forEach { playerId ->
                    val player = players[playerId]
                    val isCurrentPlayer = playerId == currentPlayerId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isCurrentPlayer) {
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                } else {
                                    Modifier
                                },
                            )
                            .padding(vertical = if (compact) 2.dp else 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (player != null) {
                            val avatar = @Composable {
                                Box(
                                    modifier = Modifier
                                        .size(if (compact) 20.dp else 24.dp)
                                        .clip(CircleShape)
                                        .background(AvatarColors.getColor(player.avatarColor)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = player.name.first().uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            // A long name shrinks to fit the column before it's cut (issue #37)
                            val name = @Composable { nameModifier: Modifier ->
                                PlayerNameText(
                                    name = player.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
                                    modifier = nameModifier,
                                )
                            }
                            if (compact) {
                                // Avatar and name side by side: one line instead of two
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    avatar()
                                    name(Modifier.weight(1f, fill = false))
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    avatar()
                                    Spacer(modifier = Modifier.height(2.dp))
                                    name(Modifier)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))

            // Segment rows
            segments.segments.forEach { segment ->
                CricketSegmentRow(
                    segment = segment,
                    playerIds = playerIds,
                    cricketState = cricketState,
                    currentPlayerId = currentPlayerId,
                    rowHeight = rowHeight,
                )
                if (!compact) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))

            // Points row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.brandColors.highlight.copy(alpha = 0.1f))
                    .padding(vertical = if (compact) 4.dp else 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.width(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "PTS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.brandColors.textOnContainer,
                    )
                }

                playerIds.forEach { playerId ->
                    val playerState = cricketState.getPlayerState(playerId)
                    val isCurrentPlayer = playerId == currentPlayerId
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${playerState.points}",
                            style = if (compact) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentPlayer) {
                                MaterialTheme.brandColors.textOnContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
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
private fun CricketSegmentRow(
    segment: Int,
    playerIds: List<Uuid>,
    cricketState: CricketState,
    currentPlayerId: Uuid?,
    // Compact layout: the row is exactly this tall (gap included), with smaller marks
    rowHeight: Dp?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (rowHeight != null) Modifier.height(rowHeight) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Segment label
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (segment == 25) "Bull" else "$segment",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Player marks
        playerIds.forEach { playerId ->
            val playerState = cricketState.getPlayerState(playerId)
            val marks = playerState.getMarks(segment)
            val isClosed = playerState.isClosed(segment)
            val isCurrentPlayer = playerId == currentPlayerId

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CricketMarksDisplay(
                    marks = marks,
                    isClosed = isClosed,
                    isCurrentPlayer = isCurrentPlayer,
                    size = rowHeight?.let { minOf(MarksSize, it - 2.dp) } ?: MarksSize,
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun CricketMarksDisplay(
    marks: Int,
    isClosed: Boolean,
    isCurrentPlayer: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = MarksSize,
) {
    val textColor = when {
        isClosed -> MaterialTheme.brandColors.text
        isCurrentPlayer -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    val displayText = when (marks) {
        0 -> "-"
        1 -> "/"
        2 -> "X"
        else -> if (isClosed) "\u2297" else "X" // ⊗ for closed (circled X)
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (isClosed) {
                    Modifier
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.brandColors.text,
                            shape = CircleShape,
                        )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            style = if (size < MarksSize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            fontWeight = if (isClosed) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** Side of a marks cell on tall phones. */
private val MarksSize = 32.dp
