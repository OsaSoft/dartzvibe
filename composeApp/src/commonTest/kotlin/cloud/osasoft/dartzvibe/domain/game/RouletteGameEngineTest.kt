package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for Roulette mode game engine logic.
 */
@OptIn(ExperimentalUuidApi::class)
class RouletteGameEngineTest : FreeSpec({

    val player1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val player2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")

    fun createRoundsEngine(
        playerIds: List<Uuid> = listOf(player1, player2),
        targets: List<Int> = listOf(20, 15, 10),
        rounds: Int = 3,
    ): GameEngine {
        val session = GameSession(
            id = sessionId,
            config = GameConfig(
                gameType = GameType.ROULETTE_ROUNDS,
                gameMode = GameMode.ROULETTE,
                doubleIn = false,
                doubleOut = false,
                playerIds = playerIds,
                legsToWin = 1,
                rouletteTargetSegments = targets,
                rouletteRounds = rounds,
            ),
            legs = listOf(Leg()),
            currentLegIndex = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = currentTimeMillis(),
        )
        return GameEngine.fromSession(session)
    }

    fun createScoreEngine(
        playerIds: List<Uuid> = listOf(player1, player2),
        targets: List<Int> = listOf(20, 15, 10, 5, 20, 15, 10, 5, 20, 15),
        targetScore: Int = 10,
    ): GameEngine {
        val session = GameSession(
            id = sessionId,
            config = GameConfig(
                gameType = GameType.ROULETTE_SCORE,
                gameMode = GameMode.ROULETTE,
                doubleIn = false,
                doubleOut = false,
                playerIds = playerIds,
                legsToWin = 1,
                rouletteTargetSegments = targets,
                rouletteTargetScore = targetScore,
            ),
            legs = listOf(Leg()),
            currentLegIndex = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = currentTimeMillis(),
        )
        return GameEngine.fromSession(session)
    }

    "Scoring" - {
        "Should score 0 for a miss (wrong segment)" {
            // GIVEN a roulette engine with target 20
            val engine = createRoundsEngine(targets = listOf(20, 15, 10))

            // WHEN player hits segment 19 (not the target)
            val (newEngine, result) = engine.addThrow(19, Multiplier.SINGLE)

            // THEN 0 points scored
            result.shouldBeInstanceOf<ThrowResult.RouletteHit>()
            (result as ThrowResult.RouletteHit).pointsScored shouldBe 0
            result.newTotalScore shouldBe 0
        }

        "Should score 1 for a single hit on target" {
            // GIVEN a roulette engine with target 20
            val engine = createRoundsEngine(targets = listOf(20, 15, 10))

            // WHEN player hits S20
            val (newEngine, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN 1 point scored
            result.shouldBeInstanceOf<ThrowResult.RouletteHit>()
            (result as ThrowResult.RouletteHit).pointsScored shouldBe 1
            result.newTotalScore shouldBe 1
        }

        "Should score 2 for a double hit on target" {
            // GIVEN a roulette engine with target 20
            val engine = createRoundsEngine(targets = listOf(20, 15, 10))

            // WHEN player hits D20
            val (newEngine, result) = engine.addThrow(20, Multiplier.DOUBLE)

            // THEN 2 points scored
            result.shouldBeInstanceOf<ThrowResult.RouletteHit>()
            (result as ThrowResult.RouletteHit).pointsScored shouldBe 2
            result.newTotalScore shouldBe 2
        }

        "Should score 3 for a triple hit on target" {
            // GIVEN a roulette engine with target 20
            val engine = createRoundsEngine(targets = listOf(20, 15, 10))

            // WHEN player hits T20
            val (newEngine, result) = engine.addThrow(20, Multiplier.TRIPLE)

            // THEN 3 points scored
            result.shouldBeInstanceOf<ThrowResult.RouletteHit>()
            (result as ThrowResult.RouletteHit).pointsScored shouldBe 3
            result.newTotalScore shouldBe 3
        }

        "Should handle bull (25) as target" {
            // GIVEN a roulette engine with target 25 (bull)
            val engine = createRoundsEngine(targets = listOf(25, 15, 10))

            // WHEN player hits S25
            val (newEngine, result) = engine.addThrow(25, Multiplier.SINGLE)

            // THEN 1 point scored
            result.shouldBeInstanceOf<ThrowResult.RouletteHit>()
            (result as ThrowResult.RouletteHit).pointsScored shouldBe 1

            // AND hitting D25 (bullseye) scores 2
            val (e2, result2) = newEngine.addThrow(25, Multiplier.DOUBLE)
            result2.shouldBeInstanceOf<ThrowResult.RouletteHit>()
            (result2 as ThrowResult.RouletteHit).pointsScored shouldBe 2
            result2.newTotalScore shouldBe 3
        }

        "Should accumulate points within a turn" {
            // GIVEN a roulette engine with target 20
            val engine = createRoundsEngine(targets = listOf(20, 15, 10))

            // WHEN player hits S20, then D20, then T20
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)
            val (e2, _) = e1.addThrow(20, Multiplier.DOUBLE)
            val (e3, result) = e2.addThrow(20, Multiplier.TRIPLE)

            // THEN total is 6 (1+2+3)
            result.shouldBeInstanceOf<ThrowResult.RouletteHit>()
            (result as ThrowResult.RouletteHit).newTotalScore shouldBe 6
        }
    }

    "Round advancement" - {
        "Should advance round after all players have thrown" {
            // GIVEN a 2-player roulette engine with targets [20, 15, 10]
            val engine = createRoundsEngine(targets = listOf(20, 15, 10), rounds = 3)

            // WHEN player 1 throws 3 darts and ends turn
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)
            val (e2, _) = e1.addThrow(1, Multiplier.SINGLE)
            val (e3, _) = e2.addThrow(1, Multiplier.SINGLE)
            val (e4, r1) = e3.endTurn()

            // THEN next player is player 2, still round 1
            r1.shouldBeInstanceOf<TurnResult.NextPlayer>()
            e4.getRouletteState()!!.currentRoundIndex shouldBe 0

            // WHEN player 2 throws 3 darts and ends turn
            val (e5, _) = e4.addThrow(20, Multiplier.DOUBLE)
            val (e6, _) = e5.addThrow(1, Multiplier.SINGLE)
            val (e7, _) = e6.addThrow(1, Multiplier.SINGLE)
            val (e8, r2) = e7.endTurn()

            // THEN round advances to 1 (index)
            r2.shouldBeInstanceOf<TurnResult.NextPlayer>()
            e8.getRouletteState()!!.currentRoundIndex shouldBe 1
        }
    }

    "Rounds mode" - {
        "Should end game after all rounds with winner" {
            // GIVEN a 2-player roulette with 1 round, target segment 20
            val engine = createRoundsEngine(
                targets = listOf(20),
                rounds = 1,
            )

            // WHEN player 1 hits T20 (3 points), misses rest
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
            val (e2, _) = e1.addThrow(1, Multiplier.SINGLE)
            val (e3, _) = e2.addThrow(1, Multiplier.SINGLE)
            val (e4, _) = e3.endTurn()

            // AND player 2 hits S20 (1 point), misses rest
            val (e5, _) = e4.addThrow(20, Multiplier.SINGLE)
            val (e6, _) = e5.addThrow(1, Multiplier.SINGLE)
            val (e7, _) = e6.addThrow(1, Multiplier.SINGLE)
            val (e8, result) = e7.endTurn()

            // THEN player 1 wins (3 > 1)
            result.shouldBeInstanceOf<TurnResult.MatchWon>()
            (result as TurnResult.MatchWon).winnerId shouldBe player1
            e8.toGameSession().status shouldBe GameStatus.COMPLETED
        }

        "Should continue on tie (sudden death)" {
            // GIVEN a 2-player roulette with 1 round, target segment 20
            val engine = createRoundsEngine(
                targets = listOf(20, 15),
                rounds = 1,
            )

            // WHEN both players score 1 point each (tie)
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)
            val (e2, _) = e1.addThrow(1, Multiplier.SINGLE)
            val (e3, _) = e2.addThrow(1, Multiplier.SINGLE)
            val (e4, _) = e3.endTurn()

            val (e5, _) = e4.addThrow(20, Multiplier.SINGLE)
            val (e6, _) = e5.addThrow(1, Multiplier.SINGLE)
            val (e7, _) = e6.addThrow(1, Multiplier.SINGLE)
            val (e8, result) = e7.endTurn()

            // THEN game continues (sudden death)
            result.shouldBeInstanceOf<TurnResult.NextPlayer>()
            e8.toGameSession().status shouldBe GameStatus.IN_PROGRESS
        }
    }

    "Score mode" - {
        "Should end game when player reaches target score at round boundary" {
            // GIVEN a 2-player roulette score mode with target 3, target segment 20
            val engine = createScoreEngine(
                targets = listOf(20, 15),
                targetScore = 3,
            )

            // WHEN player 1 hits T20 (3 points - reaches target)
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
            val (e2, _) = e1.addThrow(1, Multiplier.SINGLE)
            val (e3, _) = e2.addThrow(1, Multiplier.SINGLE)
            val (e4, _) = e3.endTurn()

            // AND player 2 throws (round must complete for fairness)
            val (e5, _) = e4.addThrow(1, Multiplier.SINGLE)
            val (e6, _) = e5.addThrow(1, Multiplier.SINGLE)
            val (e7, _) = e6.addThrow(1, Multiplier.SINGLE)
            val (e8, result) = e7.endTurn()

            // THEN player 1 wins (3 points, reached target, highest score)
            result.shouldBeInstanceOf<TurnResult.MatchWon>()
            (result as TurnResult.MatchWon).winnerId shouldBe player1
        }

        "Should pick highest scorer when multiple reach target" {
            // GIVEN a 2-player roulette score mode with target 3, target segment 20
            val engine = createScoreEngine(
                targets = listOf(20, 15),
                targetScore = 3,
            )

            // WHEN player 1 scores 3 (T20)
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
            val (e2, _) = e1.addThrow(1, Multiplier.SINGLE)
            val (e3, _) = e2.addThrow(1, Multiplier.SINGLE)
            val (e4, _) = e3.endTurn()

            // AND player 2 scores 6 (T20, T20) - but target is 20 for this round
            // Both players now play. Player 2 also hits target 20
            val (e5, _) = e4.addThrow(20, Multiplier.TRIPLE)
            val (e6, _) = e5.addThrow(20, Multiplier.TRIPLE)
            val (e7, _) = e6.addThrow(20, Multiplier.TRIPLE)
            val (e8, result) = e7.endTurn()

            // THEN player 2 wins (9 > 3, both >= target)
            result.shouldBeInstanceOf<TurnResult.MatchWon>()
            (result as TurnResult.MatchWon).winnerId shouldBe player2
        }

        "Should continue if scores tied at target" {
            // GIVEN a 2-player roulette score mode with target 3, target segment 20
            val engine = createScoreEngine(
                targets = listOf(20, 15),
                targetScore = 3,
            )

            // WHEN both players score exactly 3
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
            val (e2, _) = e1.addThrow(1, Multiplier.SINGLE)
            val (e3, _) = e2.addThrow(1, Multiplier.SINGLE)
            val (e4, _) = e3.endTurn()

            val (e5, _) = e4.addThrow(20, Multiplier.TRIPLE)
            val (e6, _) = e5.addThrow(1, Multiplier.SINGLE)
            val (e7, _) = e6.addThrow(1, Multiplier.SINGLE)
            val (e8, result) = e7.endTurn()

            // THEN game continues (tied)
            result.shouldBeInstanceOf<TurnResult.NextPlayer>()
            e8.toGameSession().status shouldBe GameStatus.IN_PROGRESS
        }
    }

    "Undo" - {
        "Should restore score correctly on undo" {
            // GIVEN a roulette engine with target 20
            val engine = createRoundsEngine(targets = listOf(20, 15, 10))

            // WHEN player hits T20 (3 points)
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
            e1.getCurrentPlayerScore() shouldBe 3

            // AND undoes the throw
            val e2 = e1.undoLastThrow()

            // THEN score should revert to 0
            e2 shouldNotBe null
            e2!!.getCurrentPlayerScore() shouldBe 0
        }

        "Should restore after multiple throws" {
            // GIVEN a roulette engine with target 20
            val engine = createRoundsEngine(targets = listOf(20, 15, 10))

            // WHEN player hits S20 (+1), D20 (+2)
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)
            val (e2, _) = e1.addThrow(20, Multiplier.DOUBLE)
            e2.getCurrentPlayerScore() shouldBe 3

            // AND undoes last throw
            val e3 = e2.undoLastThrow()
            e3 shouldNotBe null
            e3!!.getCurrentPlayerScore() shouldBe 1

            // AND undoes again
            val e4 = e3.undoLastThrow()
            e4 shouldNotBe null
            e4!!.getCurrentPlayerScore() shouldBe 0
        }

        "Should return null when no throws to undo" {
            // GIVEN a roulette engine with no throws
            val engine = createRoundsEngine()

            // WHEN trying to undo
            val result = engine.undoLastThrow()

            // THEN should return null
            result shouldBe null
        }
    }

    "Turn creation" - {
        "Should record correct scoreBeforeTurn and scoreAfterTurn" {
            // GIVEN a 2-player roulette with target 20
            val engine = createRoundsEngine(targets = listOf(20, 15), rounds = 2)

            // WHEN player 1 scores 3 points
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
            val (e2, _) = e1.addThrow(1, Multiplier.SINGLE)
            val (e3, _) = e2.addThrow(1, Multiplier.SINGLE)
            val (e4, _) = e3.endTurn()

            // THEN the turn should have correct scores
            val turn = e4.toGameSession().currentLeg.turns.last()
            turn.scoreBeforeTurn shouldBe 0
            turn.scoreAfterTurn shouldBe 3
            turn.playerId shouldBe player1
        }
    }

    "Single player" - {
        "Should work with single player in rounds mode" {
            // GIVEN a 1-player roulette with 1 round, target 20
            val engine = createRoundsEngine(
                playerIds = listOf(player1),
                targets = listOf(20),
                rounds = 1,
            )

            // WHEN player hits T20 (3 points) and ends turn
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
            val (e2, _) = e1.addThrow(1, Multiplier.SINGLE)
            val (e3, _) = e2.addThrow(1, Multiplier.SINGLE)
            val (e4, result) = e3.endTurn()

            // THEN game completes with player as winner
            result.shouldBeInstanceOf<TurnResult.MatchWon>()
            (result as TurnResult.MatchWon).winnerId shouldBe player1
            e4.toGameSession().status shouldBe GameStatus.COMPLETED
        }
    }
})
