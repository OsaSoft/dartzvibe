package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.data.model.Multiplier

/** Key the Bull button reports to [updateSwipePill] (the number keys report their number). */
private const val BULL_PILL_KEY = 25

/**
 * Cricket input: only this game's Cricket numbers ([segments], in the scoreboard's order, Bull only when it is one)
 * and Miss, in two rows across the width (see [cricketPadRows]). A dart on any other number is entered as Miss, which
 * marks and scores the same. Used instead of the grid and the radial board, whatever the keypad settings.
 *
 * The keys are the grid keypad's ([ScoreButton], [BullButton]): the same S/D/T buttons, or without them the same
 * tap/swipe handling, live preview, pill and TalkBack actions. They are [CricketKeyHeight] tall, or share [maxHeight]
 * (the height left for the pad) down to [CricketMinKeyHeight].
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun CricketInputPad(
    segments: List<Int>,
    selectedMultiplier: Multiplier,
    canThrow: Boolean,
    showMultiplierButtons: Boolean,
    onMultiplierChange: (Multiplier) -> Unit,
    onScoreSelect: (segment: Int, multiplier: Multiplier) -> Unit,
    onMiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp? = null,
) {
    val rows = remember(segments) { cricketPadRows(segments) }
    var swipePill by remember { mutableStateOf<SwipePill?>(null) }
    // Only the key that set the pill clears it, so a second finger on another key can't hide it
    val swipeChangeFor = { key: Int ->
        { multiplier: Multiplier? ->
            swipePill = updateSwipePill(current = swipePill, key = key, multiplier = multiplier)
        }
    }
    val typography = MaterialTheme.typography
    val (minKeyHeight, twoLineKeyHeight) = with(LocalDensity.current) {
        val title = typography.titleMedium.lineHeight.takeIf { it.isSp }?.toDp() ?: 0.dp
        val label = typography.labelSmall.lineHeight.takeIf { it.isSp }?.toDp() ?: 0.dp
        // A shrunk key must still fit its one-line label at the user's font scale
        maxOf(CricketMinKeyHeight, title + KeyContentPadding * 2) to (title + label + KeyContentPadding * 2)
    }

    Box(modifier = modifier) {
        KeypadColumn(
            heightLeft = maxHeight,
            minKeyHeight = minKeyHeight,
            keyRows = CRICKET_PAD_ROWS,
            nominalKeyHeight = CricketKeyHeight,
            twoLineKeyHeight = twoLineKeyHeight,
            modifier = Modifier.padding(KeypadPadding),
            top = {
                if (showMultiplierButtons) {
                    KeypadMultiplierRow(
                        selectedMultiplier = selectedMultiplier,
                        onMultiplierChange = onMultiplierChange,
                    )
                }
            },
            keys = { oneLineLabels ->
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        row.forEach { cell ->
                            val cellModifier = Modifier.weight(cell.weight.toFloat()).fillMaxHeight()
                            when (val key = cell.key) {
                                is CricketPadKey.Number -> ScoreButton(
                                    score = key.segment,
                                    selectedMultiplier = selectedMultiplier,
                                    showMultiplierButtons = showMultiplierButtons,
                                    enabled = canThrow,
                                    oneLineLabel = oneLineLabels,
                                    onScoreSelect = onScoreSelect,
                                    onSwipeChange = swipeChangeFor(key.segment),
                                    modifier = cellModifier,
                                )

                                CricketPadKey.Bull -> BullButton(
                                    showMultiplierButtons = showMultiplierButtons,
                                    selectedMultiplier = selectedMultiplier,
                                    enabled = canThrow,
                                    onScoreSelect = onScoreSelect,
                                    onSwipeChange = swipeChangeFor(BULL_PILL_KEY),
                                    oneLineLabel = oneLineLabels,
                                    modifier = cellModifier,
                                )

                                CricketPadKey.Miss -> OutlinedButton(
                                    onClick = onMiss,
                                    enabled = canThrow,
                                    modifier = cellModifier,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(KeyContentPadding),
                                ) {
                                    Text(
                                        text = "Miss",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottom = {
                if (!showMultiplierButtons) {
                    SwipeHint()
                }
            },
        )

        MultiplierIndicator(
            multiplier = swipePill?.multiplier,
            // Above the keys, not over them (issue #33); the pill only shows while the S/D/T row is hidden
            modifier = Modifier.align(Alignment.TopCenter).aboveKeys(keysTop = KeypadPadding),
            compact = true,
        )
    }
}
