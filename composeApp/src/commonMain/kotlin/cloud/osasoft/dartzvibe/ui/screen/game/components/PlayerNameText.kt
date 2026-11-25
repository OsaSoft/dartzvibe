package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Smallest font a scoreboard name shrinks to; a name still too wide at this size is cut with an ellipsis. */
val MinPlayerNameFontSize = 10.sp

/**
 * Font scale for a player name measured at [textWidth] (at [fontSize]) in [maxWidth]: 1 when it fits, otherwise
 * shrunk to fit ([fitTextScale]), but never below [minFontSize]; a name that doesn't fit even then is ellipsized.
 * A [fontSize] already at or below the minimum never shrinks; without sp sizes there's no floor.
 */
fun playerNameScale(
    textWidth: Int,
    maxWidth: Int,
    fontSize: TextUnit,
    minFontSize: TextUnit = MinPlayerNameFontSize,
): Float {
    val fit = fitTextScale(textWidth = textWidth, maxWidth = maxWidth)
    if (fit >= 1f || !fontSize.isSp || !minFontSize.isSp) return fit
    val floor = (minFontSize.value / fontSize.value).coerceAtMost(1f)
    return fit.coerceAtLeast(floor)
}

/**
 * A player name on one line in the width it's given: the font shrinks to fit (down to [MinPlayerNameFontSize]) before
 * the name is cut with an ellipsis. Only the font size changes, not the line height, so the row never changes height.
 * Give it a bounded width, e.g. `Modifier.weight(1f, fill = false)` next to the avatar.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun PlayerNameText(
    name: String,
    style: TextStyle,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = constraints.maxWidth
        val textMeasurer = rememberTextMeasurer()
        val measureStyle = LocalTextStyle.current.merge(style).copy(fontWeight = fontWeight)
        val scale = remember(name, measureStyle, maxWidthPx, textMeasurer) {
            val width = textMeasurer
                .measure(text = name, style = measureStyle, maxLines = 1, softWrap = false)
                .size
                .width
            playerNameScale(textWidth = width, maxWidth = maxWidthPx, fontSize = measureStyle.fontSize)
        }
        Text(
            text = name,
            style = if (scale < 1f) style.copy(fontSize = measureStyle.fontSize * scale) else style,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
