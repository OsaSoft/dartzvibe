package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/*
 * Sizing of the score input to the height left below the fixed rows of the game screen, so it fits short phones
 * without scrolling. All in integer pixels, rounded down, so the result never overflows by a pixel.
 */

/**
 * Smallest side of the radial board's box before the screen scrolls instead of shrinking it further. At 220 dp the
 * Miss button (see [missButtonSize]) still gets 48 dp, the minimum touch target, without overlapping the wheel, and
 * the outer wedges are still about 28 dp wide. It is also what a 731 dp tall phone leaves in Checkout Practice, the
 * mode with the tallest header.
 */
internal val MinBoardSide = 220.dp

/** Default height of a grid keypad key (1–20). */
internal val KeyHeight = 40.dp

/** Smallest height of a grid keypad key at the default font scale; see [minKeyHeight]. */
internal val MinKeyHeight = 28.dp

/**
 * Smallest height of a grid keypad key before the screen scrolls instead: [MinKeyHeight], or more when the user's
 * font scale makes the one-line label ([labelLine] tall, with [contentPadding] above and below) taller.
 */
internal fun minKeyHeight(
    labelLine: Dp,
    contentPadding: Dp,
): Dp = maxOf(MinKeyHeight, labelLine + contentPadding * 2)

/** Largest Miss button on the radial board, as on tall phones. */
internal val MaxMissButtonSize = 65.dp

/**
 * Miss button diameter as a fraction of half the board canvas. The button sits in the canvas's top-left corner and
 * the wheel's radius is 0.85 of half the canvas: a button wider than (√2 − 0.85) / (√2 / 2 + 1 / 2) ≈ 0.467 of half
 * the canvas would overlap the wheel.
 */
private const val MISS_BUTTON_TO_HALF_CANVAS = 0.46f

/**
 * Side of the radial board's (square) box: the full [widthPx] when there is no height limit or it fits
 * [heightLeftPx] (what's left for the box after the input's other rows), otherwise as tall as that height allows,
 * but never below [minSidePx] (or the width, if narrower): there the screen scrolls instead.
 */
internal fun fitBoardSide(
    widthPx: Int,
    heightLeftPx: Int?,
    minSidePx: Int,
): Int = if (heightLeftPx == null) widthPx else max(min(minSidePx, widthPx), min(widthPx, heightLeftPx))

/**
 * Height of each of the grid keypad's [rows] key rows: the default [nominalPx] when there is no height limit or the
 * keys fit [heightLeftPx] (what's left after the keypad's other rows), otherwise shared out evenly, but never below
 * [minPx]: there the screen scrolls instead.
 */
internal fun fitKeyHeight(
    heightLeftPx: Int?,
    rows: Int,
    nominalPx: Int,
    minPx: Int,
): Int = if (heightLeftPx == null) nominalPx else (heightLeftPx / rows).coerceIn(minPx, nominalPx)

/**
 * Height in pixels a padded column of fixed rows takes besides its [flexibleRows] (the board, or the key rows):
 * [paddingPx] above and below, the fixed [rowHeightsPx], and a [gapPx] between every two children. What's left of
 * the available height goes to the flexible rows.
 */
internal fun columnChromePx(
    paddingPx: Int,
    gapPx: Int,
    rowHeightsPx: List<Int>,
    flexibleRows: Int,
): Int = paddingPx * 2 + rowHeightsPx.sum() + gapPx * (rowHeightsPx.size + flexibleRows - 1).coerceAtLeast(0)

/** Miss button diameter on a board canvas of [canvasSide]: as large as [maxSize] while it stays clear of the wheel. */
internal fun missButtonSize(
    canvasSide: Float,
    maxSize: Float,
): Float = min(maxSize, canvasSide / 2 * MISS_BUTTON_TO_HALF_CANVAS)
