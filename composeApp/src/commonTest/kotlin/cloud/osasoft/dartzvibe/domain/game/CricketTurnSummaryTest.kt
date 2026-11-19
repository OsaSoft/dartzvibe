package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.CricketPlayerState
import cloud.osasoft.dartzvibe.data.model.CricketState
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Throw
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for [CricketRules.turnSummary]: the marks and points of a whole Cricket turn.
 */
@OptIn(ExperimentalUuidApi::class)
class CricketTurnSummaryTest : FreeSpec({

    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val playerIds = listOf(playerId1, playerId2)

    fun single(segment: Int) = Throw(segment, Multiplier.SINGLE)
    fun double(segment: Int) = Throw(segment, Multiplier.DOUBLE)
    fun triple(segment: Int) = Throw(segment, Multiplier.TRIPLE)

    data class SummaryCase(
        val name: String,
        val turnStartState: CricketState,
        val throws: List<Throw>,
        val expected: CricketTurnSummary,
    ) {
        override fun toString(): String = name
    }

    val empty = CricketState()
    val player1Has20Closed = CricketState().withPlayerState(playerId1, CricketPlayerState(marks = mapOf(20 to 3)))
    val both20Closed = player1Has20Closed.withPlayerState(playerId2, CricketPlayerState(marks = mapOf(20 to 3)))
    val player1Has19Twice = CricketState().withPlayerState(playerId1, CricketPlayerState(marks = mapOf(19 to 2)))

    "Turn summary" - {
        withData(
            SummaryCase(
                name = "Should count T20 then S19 as 4 marks from an empty board",
                turnStartState = empty,
                throws = listOf(triple(20), single(19)),
                expected = CricketTurnSummary(marks = 4, points = 0),
            ),
            SummaryCase(
                name = "Should count T20 then T20 as 6 marks and 60 points",
                turnStartState = empty,
                throws = listOf(triple(20), triple(20)),
                expected = CricketTurnSummary(marks = 6, points = 60),
            ),
            SummaryCase(
                name = "Should count darts on non-Cricket numbers as nothing",
                turnStartState = empty,
                throws = listOf(single(5), single(2)),
                expected = CricketTurnSummary(marks = 0, points = 0),
            ),
            SummaryCase(
                name = "Should count a miss as nothing",
                turnStartState = empty,
                throws = listOf(Throw(0, Multiplier.SINGLE)),
                expected = CricketTurnSummary(marks = 0, points = 0),
            ),
            SummaryCase(
                name = "Should count a single S20 on an open number as 1 mark",
                turnStartState = empty,
                throws = listOf(single(20)),
                expected = CricketTurnSummary(marks = 1, points = 0),
            ),
            SummaryCase(
                name = "Should keep an earlier dart's points when a later dart only adds marks",
                turnStartState = player1Has20Closed,
                throws = listOf(triple(20), single(19)),
                expected = CricketTurnSummary(marks = 4, points = 60),
            ),
            SummaryCase(
                name = "Should count a dart on a number closed for everyone as nothing",
                turnStartState = both20Closed,
                throws = listOf(triple(20), single(19)),
                expected = CricketTurnSummary(marks = 1, points = 0),
            ),
            SummaryCase(
                name = "Should count both the closing mark and the scoring overflow of one dart",
                turnStartState = player1Has19Twice,
                throws = listOf(triple(19)),
                expected = CricketTurnSummary(marks = 3, points = 38),
            ),
            SummaryCase(
                name = "Should count a bull's overflow by its 25 segment",
                turnStartState = empty,
                throws = listOf(double(25), double(25)),
                expected = CricketTurnSummary(marks = 4, points = 25),
            ),
            SummaryCase(
                name = "Should count the worst case of three trebles on an already closed number",
                turnStartState = player1Has20Closed,
                throws = listOf(triple(20), triple(20), triple(20)),
                expected = CricketTurnSummary(marks = 9, points = 180),
            ),
            SummaryCase(
                name = "Should count no darts as nothing",
                turnStartState = empty,
                throws = emptyList(),
                expected = CricketTurnSummary(marks = 0, points = 0),
            ),
        ) { case ->
            // GIVEN the Cricket state at the start of player 1's turn and the turn's darts

            // WHEN the turn is summed up
            val summary = CricketRules.turnSummary(
                turnStartState = case.turnStartState,
                throws = case.throws,
                playerId = playerId1,
                playerIds = playerIds,
            )

            // THEN marks and points cover every dart of the turn
            summary shouldBe case.expected
        }
    }
})
