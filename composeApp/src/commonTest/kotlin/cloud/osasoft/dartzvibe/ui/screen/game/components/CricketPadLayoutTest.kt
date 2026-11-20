package cloud.osasoft.dartzvibe.ui.screen.game.components

import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.ui.screen.game.components.CricketPadKey.Bull
import cloud.osasoft.dartzvibe.ui.screen.game.components.CricketPadKey.Miss
import cloud.osasoft.dartzvibe.ui.screen.game.components.CricketPadKey.Number
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

private const val THRESHOLD_PX = 78.75f // 30 dp at density 2.625 (Pixel 9)

class CricketPadLayoutTest : FreeSpec({

    data class RowsCase(
        val name: String,
        val segments: List<Int>,
        val expected: List<List<CricketPadCell>>,
    ) {
        override fun toString(): String = name
    }

    "Should lay out only the game's Cricket targets and Miss in two full rows" - {
        withData(
            RowsCase(
                name = "Regular: 20 19 18 17 / 16 15 Bull Miss",
                segments = CricketSegments.STANDARD_CRICKET_SEGMENTS,
                expected = listOf(
                    listOf(
                        CricketPadCell(Number(20)),
                        CricketPadCell(Number(19)),
                        CricketPadCell(Number(18)),
                        CricketPadCell(Number(17)),
                    ),
                    listOf(
                        CricketPadCell(Number(16)),
                        CricketPadCell(Number(15)),
                        CricketPadCell(Bull),
                        CricketPadCell(Miss),
                    ),
                ),
            ),
            RowsCase(
                name = "Random with the bull: Bull first, as the scoreboard lists it",
                segments = listOf(25, 14, 11, 9, 6, 3, 1),
                expected = listOf(
                    listOf(
                        CricketPadCell(Bull),
                        CricketPadCell(Number(14)),
                        CricketPadCell(Number(11)),
                        CricketPadCell(Number(9)),
                    ),
                    listOf(
                        CricketPadCell(Number(6)),
                        CricketPadCell(Number(3)),
                        CricketPadCell(Number(1)),
                        CricketPadCell(Miss),
                    ),
                ),
            ),
            RowsCase(
                name = "Random without the bull: 7 numbers and Miss, no Bull key: 20 13 12 8 / 7 4 2 Miss",
                segments = listOf(20, 13, 12, 8, 7, 4, 2),
                expected = listOf(
                    listOf(
                        CricketPadCell(Number(20)),
                        CricketPadCell(Number(13)),
                        CricketPadCell(Number(12)),
                        CricketPadCell(Number(8)),
                    ),
                    listOf(
                        CricketPadCell(Number(7)),
                        CricketPadCell(Number(4)),
                        CricketPadCell(Number(2)),
                        CricketPadCell(Miss),
                    ),
                ),
            ),
        ) { case ->
            // GIVEN the scoreboard's segments
            // WHEN the pad is laid out
            val rows = cricketPadRows(case.segments)

            // THEN it holds exactly those targets, in order, then Miss
            rows shouldBe case.expected
        }
    }

    "Should give both rows the same width in columns for any Random game" {
        // GIVEN many random Cricket games
        repeat(200) {
            val segments = CricketSegments.random().segments

            // WHEN the pad is laid out
            val rows = cricketPadRows(segments)

            // THEN there are two rows of equal total weight, and every number of the game has a key
            rows.size shouldBe 2
            rows[0].sumOf { it.weight } shouldBe rows[1].sumOf { it.weight }
            rows.flatten().map { it.key }.filterIsInstance<Number>().map { it.segment } shouldBe
                segments.filter { it != 25 }
            // AND a Bull key only when the bull is a target, and one Miss key
            rows.flatten().count { it.key == Bull } shouldBe if (25 in segments) 1 else 0
            rows.flatten().count { it.key == Miss } shouldBe 1
        }
    }

    "Should give a Random game without the bull 4 keys of equal width in each row, as Regular Cricket" {
        // GIVEN a Random game without the bull
        val segments = listOf(19, 17, 10, 9, 5, 3, 1)

        // WHEN the pad is laid out
        val rows = cricketPadRows(segments)

        // THEN both rows hold 4 single-column keys, like the Regular pad
        rows.map { row -> row.map { it.weight } } shouldBe
            cricketPadRows(CricketSegments.STANDARD_CRICKET_SEGMENTS).map { row -> row.map { it.weight } }
        rows.map { row -> row.map { it.weight } } shouldBe listOf(listOf(1, 1, 1, 1), listOf(1, 1, 1, 1))
    }

    "Should enter Double for Bull with Double or Triple selected (no triple bull)" - {
        withData(
            Multiplier.SINGLE to Multiplier.SINGLE,
            Multiplier.DOUBLE to Multiplier.DOUBLE,
            Multiplier.TRIPLE to Multiplier.DOUBLE,
        ) { (selected, expected) ->
            // GIVEN the selected multiplier
            // WHEN the Bull key is tapped
            // THEN it enters a single or a double
            bullMultiplier(selected) shouldBe expected
        }
    }

    data class GestureCase(
        val name: String,
        val deltaY: Float,
        val isBull: Boolean,
        val expected: Multiplier,
    ) {
        override fun toString(): String = "$name (${if (isBull) "Bull" else "number"}) -> $expected"
    }

    "Should enter the multiplier of a tap or swipe on a Cricket key as on the grid" - {
        withData(
            GestureCase(name = "tap", deltaY = 0f, isBull = false, expected = Multiplier.SINGLE),
            GestureCase(name = "swipe up", deltaY = -THRESHOLD_PX - 1f, isBull = false, expected = Multiplier.DOUBLE),
            GestureCase(name = "swipe down", deltaY = THRESHOLD_PX + 1f, isBull = false, expected = Multiplier.TRIPLE),
            GestureCase(name = "tap", deltaY = 0f, isBull = true, expected = Multiplier.SINGLE),
            GestureCase(name = "swipe up", deltaY = -THRESHOLD_PX - 1f, isBull = true, expected = Multiplier.DOUBLE),
            GestureCase(name = "swipe down", deltaY = THRESHOLD_PX + 1f, isBull = true, expected = Multiplier.DOUBLE),
        ) { case ->
            // GIVEN a gesture on a key with the S/D/T buttons hidden
            // WHEN it lifts
            val multiplier = classifyKeySwipe(deltaY = case.deltaY, thresholdPx = THRESHOLD_PX, isBull = case.isBull)

            // THEN it enters what the grid key would
            multiplier shouldBe case.expected
        }
    }

    data class PadHeightCase(
        val name: String,
        val keyHeightPx: Int,
        val topRowPx: Int,
        val bottomRowPx: Int,
        val expected: Int,
    ) {
        override fun toString(): String = "$name -> $expected"
    }

    "Should add up the Cricket pad's height" - {
        withData(
            // 8 + 40 + 4 + 44 + 4 + 44 + 8
            PadHeightCase(
                name = "S/D/T row, 44 dp keys",
                keyHeightPx = 44,
                topRowPx = 40,
                bottomRowPx = 0,
                expected = 152,
            ),
            // 8 + 44 + 4 + 44 + 4 + 20 + 8
            PadHeightCase(
                name = "swipe hint, 44 dp keys",
                keyHeightPx = 44,
                topRowPx = 0,
                bottomRowPx = 20,
                expected = 132,
            ),
            // 8 + 40 + 4 + 64 + 4 + 64 + 8
            PadHeightCase(
                name = "S/D/T row, 64 dp keys",
                keyHeightPx = 64,
                topRowPx = 40,
                bottomRowPx = 0,
                expected = 192,
            ),
        ) { case ->
            // GIVEN the pad's rows at density 1
            // WHEN its height is added up
            val height = cricketPadHeightPx(
                keyHeightPx = case.keyHeightPx,
                paddingPx = 8,
                gapPx = 4,
                topRowPx = case.topRowPx,
                bottomRowPx = case.bottomRowPx,
            )

            // THEN it is the padding, the rows and the gaps between them
            height shouldBe case.expected
        }
    }

    data class RowFitCase(
        val name: String,
        val availablePx: Int,
        val fixedPx: Int,
        val padPx: Int,
        val expected: Int,
    ) {
        override fun toString(): String = "$name -> $expected"
    }

    "Should size the scoreboard rows to what the pad leaves, between 24 and 36 dp" - {
        withData(
            // 360×640 dp phone: about 538 dp below the bars, about 194 dp of compact scoreboard and turn card besides
            // the rows, and a 152 dp pad: (538 − 194 − 152) / 7 = 27
            RowFitCase(name = "small phone, S/D/T row", availablePx = 538, fixedPx = 194, padPx = 152, expected = 27),
            RowFitCase(name = "small phone, swipe hint", availablePx = 538, fixedPx = 194, padPx = 132, expected = 30),
            // The same phone with a 48 dp 3-button navigation bar: (490 − 194 − 144) / 7 = 21, held at the 24 dp floor
            RowFitCase(
                name = "small phone, 3-button nav",
                availablePx = 490,
                fixedPx = 194,
                padPx = 144,
                expected = 24,
            ),
            RowFitCase(
                name = "plenty of room: as tall as on tall phones",
                availablePx = 700,
                fixedPx = 194,
                padPx = 152,
                expected = 36,
            ),
            RowFitCase(
                name = "too short: the floor, the screen scrolls",
                availablePx = 450,
                fixedPx = 194,
                padPx = 152,
                expected = CricketMinRowHeight.value.toInt(),
            ),
        ) { case ->
            // GIVEN the viewport, the header's other parts and the pad, at density 1
            // WHEN the rows are fitted
            val rowHeight = fitScoreboardRowHeight(
                availablePx = case.availablePx,
                fixedPx = case.fixedPx,
                padPx = case.padPx,
                rows = 7,
                minPx = CricketMinRowHeight.value.toInt(),
                nominalPx = CricketRowHeight.value.toInt(),
            )

            // THEN they share what's left, within the limits
            rowHeight shouldBe case.expected
        }
    }
})
