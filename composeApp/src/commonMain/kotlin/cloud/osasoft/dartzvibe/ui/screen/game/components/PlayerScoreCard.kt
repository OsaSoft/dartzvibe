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
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.ui.theme.AvatarColors

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlayerScoreCard(
    player: Player,
    score: Int,
    legsWon: Int,
    legsToWin: Int,
    isCurrentPlayer: Boolean,
    lastTurnScore: Int? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .then(
                if (isCurrentPlayer) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                    )
                } else {
                    Modifier
                },
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlayer) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentPlayer) 4.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Player name with avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AvatarColors.getColor(player.avatarColor)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = player.name.first().uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Current score
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentPlayer) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            // Legs won indicator
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Legs: $legsWon/$legsToWin",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Last turn score (if available)
            lastTurnScore?.let { turnScore ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last: -$turnScore",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun CompactPlayerScoreCard(
    player: Player,
    score: Int,
    legsWon: Int,
    isCurrentPlayer: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .then(
                if (isCurrentPlayer) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                    )
                } else {
                    Modifier
                },
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlayer) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Player avatar
            Box(
                modifier = Modifier
                    .size(24.dp)
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

            Spacer(modifier = Modifier.height(2.dp))

            // Score
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentPlayer) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            // Legs indicator (dots)
            if (legsWon > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(legsWon) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}
