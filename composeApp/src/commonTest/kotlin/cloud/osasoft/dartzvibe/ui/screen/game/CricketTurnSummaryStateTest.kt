package cloud.osasoft.dartzvibe.ui.screen.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.domain.game.CricketTurnSummary
import cloud.osasoft.dartzvibe.domain.game.GameEngine
import cloud.osasoft.dartzvibe.domain.game.TurnRewind
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for the Cricket turn slot: [ActiveGameState.cricketTurnSummary] follows the whole turn (and undo), and
 * [cricketTurnSummaryText] formats it.
 */
@OptIn(ExperimentalUuidApi::class)
class CricketTurnSummaryStateTest : FreeSpec({

    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")

    fun engine(mode: GameMode): GameEngine = GameEngine.fromSession(
        GameSession(
            id = Uuid.parse("00000000-0000-0000-0000-000000000100"),
            config = GameConfig(
                gameType = if (mode == GameMode.CRICKET) GameType.CRICKET_REGULAR else GameType.CLASSIC_301,
                gameMode = mode,
                doubleIn = false,
                doubleOut = false,
                playerIds = listOf(playerId1, playerId2),
                legsToWin = 1,
            ),
            legs = listOf(Leg()),
            currentLegIndex = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = 0L,
        ),
    )

    fun GameEngine.throwing(vararg darts: Pair<Int, Multiplier>): GameEngine =
        darts.fold(this) { engine, (segment, multiplier) -> engine.addThrow(segment, multiplier).first }

    "Turn summary state" - {
        "Should sum the whole turn, not only the last dart" {
            // GIVEN a Cricket turn of T20 then S19
            val engine = engine(GameMode.CRICKET).throwing(20 to Multiplier.TRIPLE, 19 to Multiplier.SINGLE)

            // WHEN the screen state is built
            val state = ActiveGameState(engine = engine)

            // THEN the slot covers both darts
            state.cricketTurnSummary shouldBe CricketTurnSummary(marks = 4, points = 0)
        }

        "Should show the remaining darts' totals after undoing the last dart" {
            // GIVEN a turn of T20, T20, S19
            val engine = engine(GameMode.CRICKET)
                .throwing(20 to Multiplier.TRIPLE, 20 to Multiplier.TRIPLE, 19 to Multiplier.SINGLE)

            // WHEN the last dart is undone
            val undone = TurnRewind.undo(engine).shouldNotBeNull().engine

            // THEN the slot shows T20, T20
            ActiveGameState(engine = undone).cricketTurnSummary shouldBe CricketTurnSummary(marks = 6, points = 60)
        }

        "Should show nothing once the only dart is undone" {
            // GIVEN a turn with a single T20
            val engine = engine(GameMode.CRICKET).throwing(20 to Multiplier.TRIPLE)

            // WHEN the dart is undone
            val undone = TurnRewind.undo(engine).shouldNotBeNull().engine

            // THEN the turn is empty and sums to nothing
            undone.getCurrentTurnThrows() shouldBe emptyList()
            ActiveGameState(engine = undone).cricketTurnSummary shouldBe CricketTurnSummary(marks = 0, points = 0)
        }

        "Should show the reopened previous turn's totals after undoing into it" {
            // GIVEN player 1 closed 20 last turn, then T20, T20 this turn and ended it
            val engine = engine(GameMode.CRICKET)
                .throwing(20 to Multiplier.TRIPLE).endTurn().first
                .throwing(19 to Multiplier.SINGLE).endTurn().first
                .throwing(20 to Multiplier.TRIPLE, 20 to Multiplier.TRIPLE).endTurn().first

            // WHEN undo reopens player 1's last turn (player 2 has no darts yet)
            val reopened = TurnRewind.undo(engine).shouldNotBeNull().engine

            // THEN the slot shows that turn's replayed darts, scored against the earlier closed 20
            reopened.getCurrentPlayerId() shouldBe playerId1
            ActiveGameState(engine = reopened).cricketTurnSummary shouldBe
                CricketTurnSummary(marks = 6, points = 120)
        }

        "Should be null outside Cricket" {
            // GIVEN a Classic turn
            val engine = engine(GameMode.CLASSIC).throwing(20 to Multiplier.TRIPLE)

            // WHEN the screen state is built
            val state = ActiveGameState(engine = engine)

            // THEN there is no Cricket summary
            state.cricketTurnSummary.shouldBeNull()
        }
    }

    "Turn summary text" - {
        withData(
            nameFn = { (summary, text) -> "Should show \"$text\" for $summary" },
            CricketTurnSummary(marks = 0, points = 0) to "-",
            CricketTurnSummary(marks = 1, points = 0) to "1 mark",
            CricketTurnSummary(marks = 4, points = 0) to "4 marks",
            CricketTurnSummary(marks = 6, points = 60) to "6 marks · +60",
            CricketTurnSummary(marks = 9, points = 180) to "9 marks · +180",
        ) { (summary, expected) ->
            // GIVEN a turn summary

            // WHEN it is formatted for the turn slot
            val text = cricketTurnSummaryText(summary)

            // THEN it reads as marks plus any points
            text shouldBe expected
        }
    }
})
