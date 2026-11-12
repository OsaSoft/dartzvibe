package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
    isCountUp: Boolean = false,
    targetScore: Int? = null,
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            // Every line stays one line, shrunk if too wide (many players on a narrow phone, 3-digit scores): the
            // card's height, and with it the board below, never changes mid-game
            val maxWidthPx = constraints.maxWidth
            val scoreStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            val targetStyle = MaterialTheme.typography.titleMedium
            val targetText = " / $targetScore"
            val showTarget = isCountUp && targetScore != null
            val scoreParts = listOfNotNull("$score" to scoreStyle, (targetText to targetStyle).takeIf { showTarget })
            val scoreScale = oneLineScale(maxWidthPx, *scoreParts.toTypedArray())
            val legsText = "Legs: $legsWon/$legsToWin"
            val legsStyle = MaterialTheme.typography.labelMedium.scaled(
                oneLineScale(maxWidthPx, legsText to MaterialTheme.typography.labelMedium),
            )
            val lastTurnPrefix = if (isCountUp) "+" else "-"
            val lastText = lastTurnScore?.let { "Last: $lastTurnPrefix$it" } ?: "Last: -"
            val lastStyle = MaterialTheme.typography.labelSmall.scaled(
                oneLineScale(maxWidthPx, lastText to MaterialTheme.typography.labelSmall),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
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
                if (showTarget) {
                    // Parcheesi mode: show "125 / 301"
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.headlineLarge.scaled(scoreScale),
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentPlayer) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            text = targetText,
                            style = targetStyle.scaled(scoreScale),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                } else {
                    // Classic mode: show just the score
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.headlineLarge.scaled(scoreScale),
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentPlayer) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        softWrap = false,
                    )
                }

                // Legs won indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = legsText,
                        style = legsStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                    )
                }

                // Last turn score (always reserve space for consistent card height)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastText,
                    style = lastStyle,
                    maxLines = 1,
                    softWrap = false,
                    color = if (lastTurnScore != null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        Color.Transparent
                    },
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

/**
 * Font scale (1 when it fits) at which the [parts], laid out side by side on one line, fit [maxWidthPx]; see
 * [fitTextScale]. Only the font size shrinks, the line height stays, so the card's height never changes.
 */
@Composable
private fun oneLineScale(
    maxWidthPx: Int,
    vararg parts: Pair<String, TextStyle>,
): Float {
    val textMeasurer = rememberTextMeasurer()
    val partList = parts.toList()
    return remember(maxWidthPx, partList) {
        val width = partList.sumOf { (text, style) ->
            textMeasurer.measure(text = text, style = style, maxLines = 1, softWrap = false).size.width
        }
        fitTextScale(textWidth = width, maxWidth = maxWidthPx)
    }
}

/** This style with its font size times [scale], or unchanged at 1. */
private fun TextStyle.scaled(scale: Float): TextStyle = if (scale < 1f) copy(fontSize = fontSize * scale) else this
