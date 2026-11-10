package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Multiplier

private val ButtonMinHeight = 88.dp
private val ButtonShape = RoundedCornerShape(12.dp)
private val ButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)

/**
 * Roulette input: only the current target scores, so instead of the full keypad/dartboard the player
 * gets four big buttons aimed at [target], laid out as a 2x2 grid (Miss | Hit, Double | Triple).
 *
 * Miss calls [onMiss]; the other buttons call [onScoreSelect] with the target and their multiplier.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun RouletteInputPad(
    target: Int,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onMiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember(target) { rouletteInputOptions(target) }

    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.chunked(2).forEach { rowOptions ->
            // IntrinsicSize.Min + fillMaxHeight keeps both buttons of a row equally tall when a label wraps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowOptions.forEach { option ->
                    RouletteInputButton(
                        option = option,
                        onClick = {
                            when (option.kind) {
                                RouletteInputKind.MISS -> onMiss()

                                RouletteInputKind.HIT,
                                RouletteInputKind.DOUBLE,
                                RouletteInputKind.TRIPLE,
                                -> onScoreSelect(option.segment, option.multiplier)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = ButtonMinHeight),
                    )
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun RouletteInputButton(
    option: RouletteInputOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (option.kind) {
        RouletteInputKind.MISS -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = option.enabled,
            shape = ButtonShape,
            contentPadding = ButtonPadding,
        ) {
            RouletteInputButtonText(option)
        }

        RouletteInputKind.HIT,
        RouletteInputKind.DOUBLE,
        RouletteInputKind.TRIPLE,
        -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = option.enabled,
            shape = ButtonShape,
            contentPadding = ButtonPadding,
            colors = multiplierButtonColors(option.multiplier),
        ) {
            RouletteInputButtonText(option)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun RouletteInputButtonText(option: RouletteInputOption) {
    // Read as one phrase ("Double 7 · +2") instead of two separate texts; the button keeps its click action
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = option.fullLabel },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // No maxLines/ellipsis: long labels wrap and the row grows, so text never clips at large font scales
        Text(
            text = option.label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = option.pointsLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}
