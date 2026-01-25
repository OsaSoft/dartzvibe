package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Multiplier

@Suppress("ktlint:standard:function-naming")
@Composable
fun ScoreInputKeypad(
    selectedMultiplier: Multiplier,
    canThrow: Boolean,
    canUndo: Boolean,
    onMultiplierChange: (Multiplier) -> Unit,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onMiss: () -> Unit,
    onUndo: () -> Unit,
    onEndTurn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Multiplier selection row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MultiplierButton(
                text = "Single",
                shortText = "S",
                isSelected = selectedMultiplier == Multiplier.SINGLE,
                onClick = { onMultiplierChange(Multiplier.SINGLE) },
                modifier = Modifier.weight(1f),
            )
            MultiplierButton(
                text = "Double",
                shortText = "D",
                isSelected = selectedMultiplier == Multiplier.DOUBLE,
                onClick = { onMultiplierChange(Multiplier.DOUBLE) },
                modifier = Modifier.weight(1f),
            )
            MultiplierButton(
                text = "Triple",
                shortText = "T",
                isSelected = selectedMultiplier == Multiplier.TRIPLE,
                onClick = { onMultiplierChange(Multiplier.TRIPLE) },
                modifier = Modifier.weight(1f),
            )
        }

        // Score buttons grid: 1-20 in 4 rows of 5
        val scoreRows = listOf(
            listOf(1, 2, 3, 4, 5),
            listOf(6, 7, 8, 9, 10),
            listOf(11, 12, 13, 14, 15),
            listOf(16, 17, 18, 19, 20),
        )

        scoreRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { score ->
                    ScoreButton(
                        score = score,
                        multiplier = selectedMultiplier,
                        enabled = canThrow,
                        onClick = { onScoreSelect(score, selectedMultiplier) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Bull row: 25 (outer bull) and BULL (inner bull = D25)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Outer bull (25, always single)
            FilledTonalButton(
                onClick = { onScoreSelect(25, Multiplier.SINGLE) },
                enabled = canThrow,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(2.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "25",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Outer",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            // Inner bull (50, always double)
            Button(
                onClick = { onScoreSelect(25, Multiplier.DOUBLE) },
                enabled = canThrow,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(2.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "BULL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "50",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        // Action row: Miss, Undo, End Turn
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Miss button
            OutlinedButton(
                onClick = onMiss,
                enabled = canThrow,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Miss")
            }

            // Undo button
            OutlinedButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Undo")
            }

            // End Turn button
            FilledTonalButton(
                onClick = onEndTurn,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("End Turn")
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun MultiplierButton(
    text: String,
    shortText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            Text(text, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ScoreButton(
    score: Int,
    multiplier: Multiplier,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayScore = score * multiplier.value
    val prefix = when (multiplier) {
        Multiplier.SINGLE -> ""
        Multiplier.DOUBLE -> "D"
        Multiplier.TRIPLE -> "T"
    }

    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(2.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$prefix$score",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (multiplier != Multiplier.SINGLE) {
                Text(
                    text = "=$displayScore",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}
