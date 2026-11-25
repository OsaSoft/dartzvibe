package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * The chip of a Parcheesi dart that didn't count (it hit the target exactly without the double-out double). Apply it
 * after the chip's clip and fill; the label takes [nonCountingDartLabel].
 *
 * The chip keeps the dart's own fill, so it still reads as the dart that was thrown and never as an empty slot. It
 * gets a 2 dp ring in [ringColor] (the theme's error colour) inside its edge, a 1 dp line in [gapColor] (the surface
 * colour) inside the ring, and a ✕ badge in the top-right corner: [ringColor] with a [markColor] (onError) cross,
 * cased in [gapColor]. Only singles and trebles can be void, and the dark theme's error colour on its amber treble chip
 * is ~1.1:1, so every red mark is cased in [gapColor] to stand apart from the fill. The ring frames the whole chip as
 * void (a badge alone reads like a "remove" button) and the badge says what the ring means (a ring alone reads like a
 * selection). TalkBack reads the chip as one node: "T1, didn't count".
 */
fun Modifier.nonCountingDartChip(
    shape: Shape,
    ringColor: Color,
    markColor: Color,
    gapColor: Color,
): Modifier = this
    .border(width = 2.dp, color = ringColor, shape = shape)
    .drawWithContent {
        drawContent()
        // 12 dp across plus a 1.5 dp casing, centred 7 dp in from the top and right edges, inside the rounded corner
        val radius = 6.dp.toPx()
        val center = Offset(x = size.width - 7.dp.toPx(), y = 7.dp.toPx())
        drawCircle(color = gapColor, radius = radius + 1.5.dp.toPx(), center = center)
        drawCircle(color = ringColor, radius = radius, center = center)
        val arm = 2.5.dp.toPx()
        listOf(
            Offset(-arm, -arm) to Offset(arm, arm),
            Offset(-arm, arm) to Offset(arm, -arm),
        ).forEach { (from, to) ->
            drawLine(
                color = markColor,
                start = center + from,
                end = center + to,
                strokeWidth = 1.75.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }.padding(2.dp)
    .border(width = 1.dp, color = gapColor, shape = shape)
    .semantics(mergeDescendants = true) { stateDescription = "didn't count" }

/** The label of a [nonCountingDartChip]: 3 dp lower, clear of the corner badge even for "T20". */
fun Modifier.nonCountingDartLabel(): Modifier = this.offset(y = 3.dp)
