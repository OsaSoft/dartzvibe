package cloud.osasoft.dartzvibe.ui.screen.game.components

import androidx.compose.ui.unit.dp

/*
 * Layout of the Cricket pad and the sizing of the Cricket screen to the height left, so the pad fits short phones
 * without scrolling. Pure, so it can be tested without Compose.
 */

/** Segment number of the bull, as stored in [cloud.osasoft.dartzvibe.data.model.CricketSegments]. */
private const val BULL_SEGMENT = 25

/** Number of key rows of the Cricket pad. */
internal const val CRICKET_PAD_ROWS = 2

/** Height of a Cricket pad key when the screen has room for it (tall phones). */
internal val CricketKeyHeight = 64.dp

/** Height of a Cricket pad key the header is sized to leave room for on short phones. */
internal val CricketTargetKeyHeight = 44.dp

/** Smallest height of a Cricket pad key before the screen scrolls instead. */
internal val CricketMinKeyHeight = 40.dp

/** Height of a Cricket scoreboard row (marks and gap) on tall phones, as on `main`. */
internal val CricketRowHeight = 36.dp

/** Smallest height of a Cricket scoreboard row on short phones. */
internal val CricketMinRowHeight = 24.dp

/** A key of the Cricket pad. */
internal sealed interface CricketPadKey {
    /** A Cricket number (1–20) of this game. */
    data class Number(
        val segment: Int,
    ) : CricketPadKey

    /** 25 single, Bull double; shown in every game, a target or not. */
    data object Bull : CricketPadKey

    data object Miss : CricketPadKey
}

/** A key in a row of the Cricket pad, [weight] columns wide. */
internal data class CricketPadCell(
    val key: CricketPadKey,
    val weight: Int = 1,
)

/**
 * The Cricket pad's [CRICKET_PAD_ROWS] rows for a game whose scoreboard lists [segments]: its keys in the
 * scoreboard's order (Bull where the scoreboard lists 25, or after the numbers when the bull isn't a target), then
 * Miss, split evenly over two rows. With an odd number of keys (7 numbers in a Random game without the bull) Miss takes
 * the spare column, so both rows are full. Standard Cricket: 20 19 18 17 / 16 15 Bull Miss.
 */
internal fun cricketPadRows(segments: List<Int>): List<List<CricketPadCell>> {
    val scoreboardKeys = segments.map { if (it == BULL_SEGMENT) CricketPadKey.Bull else CricketPadKey.Number(it) }
    val keys = scoreboardKeys +
        listOfNotNull(CricketPadKey.Bull.takeUnless { BULL_SEGMENT in segments }) +
        CricketPadKey.Miss
    val columns = (keys.size + CRICKET_PAD_ROWS - 1) / CRICKET_PAD_ROWS
    return keys.chunked(columns).map { row ->
        row.map { key ->
            CricketPadCell(key = key, weight = if (key == CricketPadKey.Miss) 1 + columns - row.size else 1)
        }
    }
}

/**
 * Height in pixels the Cricket pad needs with [keyHeightPx] keys: [paddingPx] above and below, the S/D/T row
 * ([topRowPx], 0 when hidden) and the swipe hint ([bottomRowPx], 0 when hidden), the [CRICKET_PAD_ROWS] key rows, and
 * a [gapPx] between every two rows.
 */
internal fun cricketPadHeightPx(
    keyHeightPx: Int,
    paddingPx: Int,
    gapPx: Int,
    topRowPx: Int,
    bottomRowPx: Int,
): Int = columnChromePx(
    paddingPx = paddingPx,
    gapPx = gapPx,
    rowHeightsPx = listOf(topRowPx, bottomRowPx).filter { it > 0 },
    flexibleRows = CRICKET_PAD_ROWS,
) + keyHeightPx * CRICKET_PAD_ROWS

/**
 * Height of each of the Cricket scoreboard's [rows] rows on a short screen: what's left of [availablePx] (the
 * viewport) after the compact header's other parts ([fixedPx]) and the pad ([padPx]), shared out evenly, between
 * [minPx] and [nominalPx]. Below [minPx] the screen scrolls instead.
 */
internal fun fitScoreboardRowHeight(
    availablePx: Int,
    fixedPx: Int,
    padPx: Int,
    rows: Int,
    minPx: Int,
    nominalPx: Int,
): Int = ((availablePx - fixedPx - padPx) / rows.coerceAtLeast(1)).coerceIn(minPx, nominalPx)
