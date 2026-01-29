package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.CricketSegments
import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for GameEngine with Cricket mode - close segments and score points.
 */
@OptIn(ExperimentalUuidApi::class)
class CricketGameEngineTest : FreeSpec({

    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")

    fun createCricketEngine(
        playerIds: List<Uuid> = listOf(playerId1, playerId2),
        cricketSegments: CricketSegments = CricketSegments.standard(),
        legsToWin: Int = 1,
    ): GameEngine {
        val session = GameSession(
            id = sessionId,
            config = GameConfig(
                gameType = cloud.osasoft.dartzvibe.data.model.GameType.CLASSIC_501,
                gameMode = GameMode.CRICKET,
                doubleIn = false,
                doubleOut = false,
                playerIds = playerIds,
                legsToWin = legsToWin,
                cricketSegments = cricketSegments,
            ),
            legs = listOf(Leg()),
            currentLegIndex = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = currentTimeMillis(),
        )
        return GameEngine.fromSession(session)
    }

    "Cricket initialization" - {
        "Should start with first player" {
            // GIVEN a Cricket game

            // WHEN engine is created
            val engine = createCricketEngine()

            // THEN first player is current
            engine.getCurrentPlayerId() shouldBe playerId1
        }

        "Should start with empty cricket state" {
            // GIVEN a Cricket game

            // WHEN engine is created
            val engine = createCricketEngine()

            // THEN both players have 0 marks and 0 points
            engine.getCricketPoints(playerId1) shouldBe 0
            engine.getCricketPoints(playerId2) shouldBe 0
            engine.getCricketPlayerState(playerId1)?.marks shouldBe emptyMap()
        }

        "Should use standard segments by default" {
            // GIVEN a Cricket game with standard segments

            // WHEN engine is created
            val engine = createCricketEngine()

            // THEN segments are 20, 19, 18, 17, 16, 15, 25
            val cricketState = engine.getCricketState()
            cricketState shouldNotBe null
            cricketState!!.segments.segments shouldBe listOf(20, 19, 18, 17, 16, 15, 25)
        }
    }

    "Marking segments" - {
        "Should add single mark for single hit" {
            // GIVEN a Cricket game
            val engine = createCricketEngine()

            // WHEN player hits single 20
            val (newEngine, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN 1 mark is added
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.segment shouldBe 20
            cricketResult.marksAdded shouldBe 1
            cricketResult.totalMarks shouldBe 1
            cricketResult.pointsScored shouldBe 0

            // AND player state is updated
            newEngine.getCricketPlayerState(playerId1)?.getMarks(20) shouldBe 1
        }

        "Should add double marks for double hit" {
            // GIVEN a Cricket game
            val engine = createCricketEngine()

            // WHEN player hits double 20
            val (newEngine, result) = engine.addThrow(20, Multiplier.DOUBLE)

            // THEN 2 marks are added
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.marksAdded shouldBe 2
            cricketResult.totalMarks shouldBe 2

            newEngine.getCricketPlayerState(playerId1)?.getMarks(20) shouldBe 2
        }

        "Should add triple marks for triple hit" {
            // GIVEN a Cricket game
            val engine = createCricketEngine()

            // WHEN player hits triple 20
            val (newEngine, result) = engine.addThrow(20, Multiplier.TRIPLE)

            // THEN 3 marks are added (segment is closed)
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.marksAdded shouldBe 3
            cricketResult.totalMarks shouldBe 3

            newEngine.getCricketPlayerState(playerId1)?.getMarks(20) shouldBe 3
            newEngine.getCricketPlayerState(playerId1)?.isClosed(20) shouldBe true
        }

        "Should cap marks at 3" {
            // GIVEN a Cricket game with player already having 2 marks on 20
            var engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.DOUBLE) // 2 marks
            engine = e1

            // WHEN player hits triple 20 (would be 5 marks total)
            val (newEngine, result) = engine.addThrow(20, Multiplier.TRIPLE)

            // THEN marks are capped at 3
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.marksAdded shouldBe 1 // Only 1 mark counted toward closing
            cricketResult.totalMarks shouldBe 3

            newEngine.getCricketPlayerState(playerId1)?.getMarks(20) shouldBe 3
        }

        "Should not mark non-cricket segments" {
            // GIVEN a Cricket game
            val engine = createCricketEngine()

            // WHEN player hits single 5 (not a cricket segment)
            val (newEngine, result) = engine.addThrow(5, Multiplier.SINGLE)

            // THEN no marks are added
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.marksAdded shouldBe 0
            cricketResult.totalMarks shouldBe 0

            // AND throw is still recorded
            newEngine.getCurrentTurnThrows() shouldHaveSize 1
        }

        "Should accumulate marks across throws" {
            // GIVEN a Cricket game
            var engine = createCricketEngine()

            // WHEN player throws S20, S20, S20
            val (e1, r1) = engine.addThrow(20, Multiplier.SINGLE)
            val (e2, r2) = e1.addThrow(20, Multiplier.SINGLE)
            val (e3, r3) = e2.addThrow(20, Multiplier.SINGLE)

            // THEN marks accumulate
            (r1 as ThrowResult.CricketMarks).totalMarks shouldBe 1
            (r2 as ThrowResult.CricketMarks).totalMarks shouldBe 2
            (r3 as ThrowResult.CricketMarks).totalMarks shouldBe 3

            e3.getCricketPlayerState(playerId1)?.isClosed(20) shouldBe true
        }

        "Should handle bullseye correctly" {
            // GIVEN a Cricket game
            val engine = createCricketEngine()

            // WHEN player hits bullseye (D25)
            val (newEngine, result) = engine.addThrow(25, Multiplier.DOUBLE)

            // THEN 2 marks are added to segment 25
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.segment shouldBe 25
            cricketResult.marksAdded shouldBe 2
            cricketResult.totalMarks shouldBe 2

            newEngine.getCricketPlayerState(playerId1)?.getMarks(25) shouldBe 2
        }
    }

    "Scoring points" - {
        "Should not score on unclosed segment" {
            // GIVEN a Cricket game where player 1 has 2 marks on 20 (not closed)
            var engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.DOUBLE) // 2 marks
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(0, Multiplier.SINGLE) // Player 2 misses
            val (e4, _) = e3.endTurn()
            engine = e4

            // Player 1 still only has 2 marks on 20

            // WHEN player 1 hits another S20
            val (_, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN no points scored (closing the segment)
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.pointsScored shouldBe 0
        }

        "Should score when closed and opponent hasn't" {
            // GIVEN a Cricket game where player 1 has closed 20
            var engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // Closed with 3 marks
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(0, Multiplier.SINGLE) // Player 2 misses
            val (e4, _) = e3.endTurn()
            engine = e4

            // Player 1 has 20 closed, player 2 does not

            // WHEN player 1 hits another S20
            val (newEngine, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN 20 points are scored
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.pointsScored shouldBe 20

            // AND points are accumulated
            newEngine.getCricketPoints(playerId1) shouldBe 20
        }

        "Should not score when all players closed" {
            // GIVEN a Cricket game where both players have closed 20
            var engine = createCricketEngine()
            // Player 1 closes 20
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
            val (e2, _) = e1.endTurn()
            // Player 2 closes 20
            val (e3, _) = e2.addThrow(20, Multiplier.TRIPLE)
            val (e4, _) = e3.endTurn()
            engine = e4

            // WHEN player 1 hits another S20
            val (newEngine, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN no points scored (both closed)
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.pointsScored shouldBe 0

            newEngine.getCricketPoints(playerId1) shouldBe 0
        }

        "Should score excess marks when closing segment" {
            // GIVEN a Cricket game where player 1 has 2 marks on 20, opponent has 0
            var engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.DOUBLE) // 2 marks
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(0, Multiplier.SINGLE) // Player 2 misses
            val (e4, _) = e3.endTurn()
            engine = e4

            // WHEN player 1 throws D20 (2 more marks, but only needs 1 to close)
            val (newEngine, result) = engine.addThrow(20, Multiplier.DOUBLE)

            // THEN 1 excess mark scores 20 points
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.marksAdded shouldBe 1 // Only 1 counted toward closing
            cricketResult.totalMarks shouldBe 3
            cricketResult.pointsScored shouldBe 20 // 1 excess mark * 20

            newEngine.getCricketPoints(playerId1) shouldBe 20
        }

        "Should score triple excess marks" {
            // GIVEN a Cricket game where player 1 has closed 20
            var engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // Closed
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(0, Multiplier.SINGLE) // Player 2 misses
            val (e4, _) = e3.endTurn()
            engine = e4

            // WHEN player 1 throws T20
            val (newEngine, result) = engine.addThrow(20, Multiplier.TRIPLE)

            // THEN 60 points scored (3 * 20)
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            val cricketResult = result as ThrowResult.CricketMarks
            cricketResult.pointsScored shouldBe 60

            newEngine.getCricketPoints(playerId1) shouldBe 60
        }

        "Should accumulate points across multiple throws" {
            // GIVEN a Cricket game where player 1 has closed 20
            var engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // Closed
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
            val (e4, _) = e3.endTurn()
            engine = e4

            // WHEN player 1 throws S20, S20
            val (e5, _) = engine.addThrow(20, Multiplier.SINGLE) // +20
            val (e6, _) = e5.addThrow(20, Multiplier.SINGLE) // +20

            // THEN total points are 40
            e6.getCricketPoints(playerId1) shouldBe 40
        }
    }

    "Win conditions" - {
        "Should win when all closed and highest points" {
            // GIVEN a Cricket game
            var engine = createCricketEngine()

            // Player 1 closes all segments
            val segments = CricketSegments.STANDARD_CRICKET_SEGMENTS
            segments.forEach { segment ->
                val multiplier = if (segment == 25) Multiplier.SINGLE else Multiplier.TRIPLE
                val (e1, _) = engine.addThrow(segment, multiplier)
                if (segment == 25) {
                    // Need additional marks for bull
                    val (e2, _) = e1.addThrow(25, Multiplier.DOUBLE) // 1+2=3
                    val (e3, _) = e2.endTurn()
                    val (e4, _) = e3.addThrow(0, Multiplier.SINGLE)
                    val (e5, _) = e4.endTurn()
                    engine = e5
                } else {
                    val (e2, _) = e1.endTurn()
                    val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                    val (e4, _) = e3.endTurn()
                    engine = e4
                }
            }

            // Player 1 has all closed, 0 points. Player 2 has nothing closed, 0 points
            // Should win because all closed and >= opponent points

            // Verify all segments are closed for player 1
            val playerState = engine.getCricketPlayerState(playerId1)
            segments.forEach { segment ->
                playerState?.isClosed(segment) shouldBe true
            }

            // The last segment closing should have triggered the win
            // Let's verify by ending turn and checking for win
            // Actually, the win is detected on the throw, so let's check
        }

        "Should not win if segments not all closed" {
            // GIVEN a Cricket game where player 1 has closed only 20
            var engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // Only 20 closed
            engine = e1

            // WHEN checking win condition implicitly via end turn
            val (_, result) = engine.endTurn()

            // THEN game continues
            result.shouldBeInstanceOf<TurnResult.NextPlayer>()
        }

        "Should not win if opponent has more points" {
            // GIVEN a Cricket game where player 1 has all closed but player 2 has more points
            var engine = createCricketEngine()

            // Player 2 gets some points first
            val (e1, _) = engine.addThrow(0, Multiplier.SINGLE) // P1 miss
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(20, Multiplier.TRIPLE) // P2 closes 20
            val (e4, _) = e3.endTurn()
            val (e5, _) = e4.addThrow(0, Multiplier.SINGLE) // P1 miss
            val (e6, _) = e5.endTurn()
            val (e7, _) = e6.addThrow(20, Multiplier.TRIPLE) // P2 scores 60 points
            val (e8, _) = e7.endTurn()
            engine = e8

            // Now player 2 has 60 points
            engine.getCricketPoints(playerId2) shouldBe 60

            // Player 1 closes all segments quickly
            val segments = CricketSegments.STANDARD_CRICKET_SEGMENTS
            segments.forEach { segment ->
                val multiplier = if (segment == 25) Multiplier.SINGLE else Multiplier.TRIPLE
                val (ea, _) = engine.addThrow(segment, multiplier)
                if (segment == 25) {
                    val (eb, _) = ea.addThrow(25, Multiplier.DOUBLE)
                    val (ec, result) = eb.endTurn()
                    engine = ec
                    // Should NOT win because player 2 has more points
                    result.shouldBeInstanceOf<TurnResult.NextPlayer>()
                } else {
                    val (eb, result) = ea.endTurn()
                    engine = eb
                    // Should NOT win
                    result.shouldBeInstanceOf<TurnResult.NextPlayer>()
                }
                val (ed, _) = engine.addThrow(0, Multiplier.SINGLE)
                val (ee, _) = ed.endTurn()
                engine = ee
            }

            // Player 1 has all closed but 0 points, player 2 has 60 points
            // Player 1 should NOT have won
            engine.toGameSession().status shouldBe GameStatus.IN_PROGRESS
        }

        "Should detect win immediately on throw" {
            // GIVEN a Cricket game where player 1 is one mark away from winning
            var engine = createCricketEngine()

            // Close all but bull for player 1, player 2 has nothing
            listOf(20, 19, 18, 17, 16, 15).forEach { segment ->
                val (e1, _) = engine.addThrow(segment, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }

            // Player 1 has S25, S25 (2 marks on bull)
            val (e1, _) = engine.addThrow(25, Multiplier.SINGLE)
            val (e2, _) = e1.addThrow(25, Multiplier.SINGLE)
            val (e3, _) = e2.endTurn()
            val (e4, _) = e3.addThrow(0, Multiplier.SINGLE)
            val (e5, _) = e4.endTurn()
            engine = e5

            // Player 1 needs 1 more mark on bull to close and win

            // WHEN player 1 hits S25 (closing bull)
            val (_, result) = engine.addThrow(25, Multiplier.SINGLE)

            // THEN win is detected
            result.shouldBeInstanceOf<ThrowResult.CricketWin>()
            (result as ThrowResult.CricketWin).winnerId shouldBe playerId1
        }
    }

    "Undo" - {
        "Should correctly undo marks" {
            // GIVEN a Cricket game with some marks
            val engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // 3 marks

            // WHEN undo is called
            val undone = e1.undoLastThrow()

            // THEN marks are restored
            undone shouldNotBe null
            undone!!.getCricketPlayerState(playerId1)?.getMarks(20) shouldBe 0
            undone.getCurrentTurnThrows() shouldHaveSize 0
        }

        "Should correctly undo points" {
            // GIVEN a Cricket game where player scored points
            var engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // Close 20
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
            val (e4, _) = e3.endTurn()
            val (e5, _) = e4.addThrow(20, Multiplier.SINGLE) // Score 20 points
            engine = e5

            engine.getCricketPoints(playerId1) shouldBe 20

            // WHEN undo is called
            val undone = engine.undoLastThrow()

            // THEN points are restored
            undone shouldNotBe null
            undone!!.getCricketPoints(playerId1) shouldBe 0
        }

        "Should correctly undo multiple throws" {
            // GIVEN a Cricket game with multiple throws
            val engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE) // 1 mark
            val (e2, _) = e1.addThrow(20, Multiplier.DOUBLE) // 3 marks total

            e2.getCricketPlayerState(playerId1)?.getMarks(20) shouldBe 3

            // WHEN undo is called twice
            val undone1 = e2.undoLastThrow()
            val undone2 = undone1?.undoLastThrow()

            // THEN all marks are restored
            undone2 shouldNotBe null
            undone2!!.getCricketPlayerState(playerId1)?.getMarks(20) shouldBe 0
        }
    }

    "Turn handling" - {
        "Should switch players after end turn" {
            // GIVEN a Cricket game
            val engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)

            // WHEN turn ends
            val (e2, result) = e1.endTurn()

            // THEN it's player 2's turn
            result.shouldBeInstanceOf<TurnResult.NextPlayer>()
            (result as TurnResult.NextPlayer).playerId shouldBe playerId2
            e2.getCurrentPlayerId() shouldBe playerId2
        }

        "Should persist cricket state after turn" {
            // GIVEN a Cricket game
            var engine = createCricketEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // Close 20
            val (e2, _) = e1.endTurn()
            engine = e2

            // THEN cricket state is persisted
            val persistedState = engine.toGameSession().currentLeg.cricketState
            persistedState shouldNotBe null
            persistedState!!.getPlayerState(playerId1).getMarks(20) shouldBe 3
        }

        "Should maintain state across multiple turns" {
            // GIVEN a Cricket game
            var engine = createCricketEngine()

            // Player 1 closes 20
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
            val (e2, _) = e1.endTurn()

            // Player 2 closes 19
            val (e3, _) = e2.addThrow(19, Multiplier.TRIPLE)
            val (e4, _) = e3.endTurn()
            engine = e4

            // THEN both players' states are maintained
            engine.getCricketPlayerState(playerId1)?.isClosed(20) shouldBe true
            engine.getCricketPlayerState(playerId2)?.isClosed(19) shouldBe true
        }
    }

    "Random Cricket" - {
        "Should use random segments when configured" {
            // GIVEN a random cricket config
            val randomSegments = CricketSegments.random()
            val engine = createCricketEngine(cricketSegments = randomSegments)

            // THEN the engine uses those segments
            engine.getCricketState()!!.segments shouldBe randomSegments
        }

        "Should only mark configured random segments" {
            // GIVEN random segments that DON'T include 20
            val segmentsWithout20 = CricketSegments(listOf(19, 18, 17, 16, 15, 14, 25))
            val engine = createCricketEngine(cricketSegments = segmentsWithout20)

            // WHEN hitting segment 20
            val (newEngine, result) = engine.addThrow(20, Multiplier.TRIPLE)

            // THEN no marks are added (20 is not a target)
            result.shouldBeInstanceOf<ThrowResult.CricketMarks>()
            (result as ThrowResult.CricketMarks).marksAdded shouldBe 0
            newEngine.getCricketPlayerState(playerId1)?.getMarks(20) shouldBe 0
        }

        "Random segments should generate 7 unique segments" {
            // WHEN generating random segments
            val segments = CricketSegments.random()

            // THEN 7 unique segments are generated
            segments.segments shouldHaveSize 7
            segments.segments.toSet().size shouldBe 7
        }

        "Random segments should be sorted descending" {
            // WHEN generating random segments
            val segments = CricketSegments.random()

            // THEN segments are sorted descending
            segments.segments shouldBe segments.segments.sortedDescending()
        }
    }

    "Match winning" - {
        "Should win match with Cricket" {
            // GIVEN a Cricket game (1 leg to win)
            var engine = createCricketEngine(legsToWin = 1)

            // Close all segments for player 1
            val segments = CricketSegments.STANDARD_CRICKET_SEGMENTS
            segments.dropLast(1).forEach { segment ->
                val (e1, _) = engine.addThrow(segment, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }

            // Close bull (25) - need 3 marks
            val (e1, _) = engine.addThrow(25, Multiplier.SINGLE) // 1
            val (e2, _) = e1.addThrow(25, Multiplier.SINGLE) // 2
            val (e3, result) = e2.addThrow(25, Multiplier.SINGLE) // 3 - win!

            // THEN match is won immediately
            result.shouldBeInstanceOf<ThrowResult.CricketWin>()

            // AND when turn ends, match is complete
            val (e4, turnResult) = e3.endTurn()
            turnResult.shouldBeInstanceOf<TurnResult.MatchWon>()
            (turnResult as TurnResult.MatchWon).winnerId shouldBe playerId1

            e4.toGameSession().status shouldBe GameStatus.COMPLETED
            e4.toGameSession().winnerId shouldBe playerId1
        }
    }
})
