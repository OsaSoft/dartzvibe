package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import cloud.osasoft.dartzvibe.ui.theme.dialogTextButtonColors

/*
 * The leg-won, game-over and results dialogs, drawn inside the game screen instead of in a dialog window. A window
 * is centred on the screen, where it covered the darts row (the Cricket scoreboard and the Practice header push the
 * row down to the middle), and Android tears it down before the screen slides away, so Finish showed the bare game
 * for a few frames. In-screen, the card sits below the header and leaves with the screen.
 */

/** Gap between the card and the darts row above it, and the screen's edges. */
private val WinDialogMargin = 8.dp

/**
 * Space below the header that the card keeps to: with less, it may be as tall as the screen allows and rises over the
 * header only as far as its own height needs.
 */
private val WinDialogMinSpaceBelow = 200.dp

/** The dimmed backdrop's alpha, as Material's dialog scrim. */
private const val SCRIM_ALPHA = 0.32f

/**
 * Largest height of the win card: the [spaceBelowPx] left below the header, so long results scroll there, unless
 * that's under [minSpaceBelowPx]; then the whole [viewportPx], and [winDialogTop] raises the card only as far as it
 * needs.
 */
internal fun winDialogMaxHeight(
    spaceBelowPx: Int,
    minSpaceBelowPx: Int,
    viewportPx: Int,
): Int = (if (spaceBelowPx >= minSpaceBelowPx) spaceBelowPx else viewportPx).coerceIn(0, viewportPx.coerceAtLeast(0))

/**
 * Top of a win card [cardPx] tall: right under the header ([anchorPx]), unless that would push its bottom past
 * [bottomPx]; then as low as it fits, but never above [topPx].
 */
internal fun winDialogTop(
    anchorPx: Int,
    cardPx: Int,
    topPx: Int,
    bottomPx: Int,
): Int = anchorPx.coerceAtMost(bottomPx - cardPx).coerceAtLeast(topPx)

/** Dims everything below it and swallows its touches, as a dialog window's scrim did. */
@Suppress("ktlint:standard:function-naming")
@Composable
fun WinDialogScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .clearAndSetSemantics { }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            },
    )
}

/**
 * Lays [card] out below the darts row: [anchorPx] is the header's bottom in this layout's coordinates (read during
 * layout, so the card follows the same frame's header), and [topInsetPx]/[bottomInsetPx] the bars it must stay
 * clear of. The layout fills its constraints; put it over the screen's content, above [WinDialogScrim].
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun WinDialogLayer(
    anchorPx: () -> Int,
    topInsetPx: Int,
    bottomInsetPx: Int,
    modifier: Modifier = Modifier,
    card: @Composable () -> Unit,
) {
    Layout(content = card, modifier = modifier) { measurables, constraints ->
        val marginPx = WinDialogMargin.roundToPx()
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val topPx = topInsetPx + marginPx
        val bottomPx = height - bottomInsetPx - marginPx
        val anchor = anchorPx() + marginPx
        val maxCardPx = winDialogMaxHeight(
            spaceBelowPx = bottomPx - anchor,
            minSpaceBelowPx = WinDialogMinSpaceBelow.roundToPx(),
            viewportPx = bottomPx - topPx,
        )
        val sidePx = 3 * marginPx
        val cardConstraints = Constraints(maxWidth = (width - 2 * sidePx).coerceAtLeast(0), maxHeight = maxCardPx)
        val placeables = measurables.map { it.measure(cardConstraints) }
        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.place(
                    x = (width - placeable.width) / 2,
                    y = winDialogTop(anchorPx = anchor, cardPx = placeable.height, topPx = topPx, bottomPx = bottomPx),
                )
            }
        }
    }
}

/**
 * A win dialog's card, styled as Material's AlertDialog: [title], the [body] (scrolls when the card is limited in
 * height), then Undo (when [onUndo] is set) and [confirmLabel].
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun WinDialogCard(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onUndo: (() -> Unit)?,
    modifier: Modifier = Modifier,
    body: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .widthIn(min = 280.dp, max = 560.dp)
            .semantics { paneTitle = title },
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = AlertDialogDefaults.titleContentColor,
            )
            Spacer(modifier = Modifier.height(12.dp))
            CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.textContentColor) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    body()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                onUndo?.let { undo ->
                    TextButton(onClick = undo, colors = dialogTextButtonColors()) {
                        Text("Undo")
                    }
                }
                TextButton(onClick = onConfirm, colors = dialogTextButtonColors()) {
                    Text(confirmLabel)
                }
            }
        }
    }
}
