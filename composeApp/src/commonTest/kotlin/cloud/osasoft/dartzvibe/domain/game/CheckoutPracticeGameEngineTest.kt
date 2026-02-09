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
 * Tests for Checkout Practice mode game engine logic.
 */
@OptIn(ExperimentalUuidApi::class)
class CheckoutPracticeGameEngineTest : FreeSpec({

    val playerId = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")

    fun createEngine(
        targets: List<Int> = listOf(40, 32, 16),
    ): GameEngine {
        val session = GameSession(
            id = sessionId,
            config = GameConfig(
                gameType = GameType.CHECKOUT_EASY,
                gameMode = GameMode.CHECKOUT_PRACTICE,
                doubleIn = false,
                doubleOut = true,
                playerIds = listOf(playerId),
                legsToWin = 1,
                checkoutPracticeTargets = targets,
            ),
            legs = listOf(Leg()),
            currentLegIndex = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = currentTimeMillis(),
        )
        return GameEngine.fromSession(session)
    }

    "Checkout Practice initialization" - {
        "Should start with first target as score" {
            // GIVEN a checkout practice engine with targets [40, 32, 16]
            val engine = createEngine(targets = listOf(40, 32, 16))

            // THEN current score should be the first target (40)
            engine.getCurrentPlayerScore() shouldBe 40
        }

        "Should start with round 1" {
            // GIVEN a checkout practice engine
            val engine = createEngine()

            // THEN checkout practice state should show round index 0
            val state = engine.getCheckoutPracticeState()
            state shouldNotBe null
            state!!.currentRoundIndex shouldBe 0
            state.totalRounds shouldBe 3
        }
    }

    "addThrow" - {
        "Should detect checkout with double" {
            // GIVEN a checkout practice engine with target 40
            val engine = createEngine(targets = listOf(40, 32))

            // WHEN player hits D20 (= 40)
            val (newEngine, result) = engine.addThrow(20, Multiplier.DOUBLE)

            // THEN checkout is detected
            result.shouldBeInstanceOf<ThrowResult.Checkout>()
            newEngine.getCurrentPlayerScore() shouldBe 0
        }

        "Should bust when going below zero" {
            // GIVEN a checkout practice engine with target 16
            val engine = createEngine(targets = listOf(16, 32))

            // WHEN player hits T20 (= 60, way over 16)
            val (newEngine, result) = engine.addThrow(20, Multiplier.TRIPLE)

            // THEN should bust
            result.shouldBeInstanceOf<ThrowResult.Bust>()
            // AND score resets to target
            newEngine.getCurrentPlayerScore() shouldBe 16
        }

        "Should bust on score 0 without double" {
            // GIVEN a checkout practice engine with target 16
            val engine = createEngine(targets = listOf(16, 32))

            // WHEN player hits S16 (reaches 0 but not with double)
            val (newEngine, result) = engine.addThrow(16, Multiplier.SINGLE)

            // THEN should bust (double-out required)
            result.shouldBeInstanceOf<ThrowResult.Bust>()
            newEngine.getCurrentPlayerScore() shouldBe 16
        }

        "Should reduce score on hit" {
            // GIVEN a checkout practice engine with target 40
            val engine = createEngine(targets = listOf(40, 32))

            // WHEN player hits S20 (= 20)
            val (newEngine, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN score should reduce to 20
            result.shouldBeInstanceOf<ThrowResult.Success>()
            (result as ThrowResult.Success).newScore shouldBe 20
            newEngine.getCurrentPlayerScore() shouldBe 20
        }
    }

    "endTurn" - {
        "Should advance to next target after bust" {
            // GIVEN a checkout practice engine with targets [40, 32, 16]
            val engine = createEngine(targets = listOf(40, 32, 16))

            // WHEN player busts (hits T20 on target 40)
            val (bustedEngine, _) = engine.addThrow(20, Multiplier.TRIPLE)

            // AND ends the turn
            val (nextEngine, turnResult) = bustedEngine.endTurn()

            // THEN next round starts with target 32
            turnResult.shouldBeInstanceOf<TurnResult.NextPlayer>()
            nextEngine.getCurrentPlayerScore() shouldBe 32
            nextEngine.getCheckoutPracticeState()!!.currentRoundIndex shouldBe 1
        }

        "Should advance to next target after successful checkout" {
            // GIVEN a checkout practice engine with targets [40, 32, 16]
            val engine = createEngine(targets = listOf(40, 32, 16))

            // WHEN player checks out (hits D20 on target 40)
            val (checkoutEngine, _) = engine.addThrow(20, Multiplier.DOUBLE)

            // AND ends the turn
            val (nextEngine, turnResult) = checkoutEngine.endTurn()

            // THEN next round starts with target 32
            turnResult.shouldBeInstanceOf<TurnResult.NextPlayer>()
            nextEngine.getCurrentPlayerScore() shouldBe 32
        }

        "Should complete practice after all rounds" {
            // GIVEN a checkout practice engine with 2 targets [40, 32]
            val engine = createEngine(targets = listOf(40, 32))

            // WHEN round 1: checkout D20 -> end turn
            val (e1, _) = engine.addThrow(20, Multiplier.DOUBLE)
            val (e2, _) = e1.endTurn()

            // AND round 2: checkout D16 -> end turn
            val (e3, _) = e2.addThrow(16, Multiplier.DOUBLE)
            val (e4, result) = e3.endTurn()

            // THEN match is won (practice complete)
            result.shouldBeInstanceOf<TurnResult.MatchWon>()
            e4.toGameSession().status shouldBe GameStatus.COMPLETED
        }

        "Should track success count and darts correctly" {
            // GIVEN a checkout practice engine with 3 targets [40, 32, 16]
            val engine = createEngine(targets = listOf(40, 32, 16))

            // WHEN round 1: checkout with D20 (1 dart, success)
            val (e1, _) = engine.addThrow(20, Multiplier.DOUBLE)
            val (e2, _) = e1.endTurn()

            // AND round 2: bust with T20 (1 dart, failure)
            val (e3, _) = e2.addThrow(20, Multiplier.TRIPLE)
            val (e4, _) = e3.endTurn()

            // AND round 3: checkout with D8 (1 dart, success)
            val (e5, _) = e4.addThrow(8, Multiplier.DOUBLE)
            val (e6, _) = e5.endTurn()

            // THEN practice state should have 2 successes out of 3
            val state = e6.getCheckoutPracticeState()!!
            state.isComplete shouldBe true
            state.successCount shouldBe 2
            state.roundResults[0].success shouldBe true
            state.roundResults[0].dartsUsed shouldBe 1
            state.roundResults[1].success shouldBe false
            state.roundResults[1].dartsUsed shouldBe 1
            state.roundResults[2].success shouldBe true
            state.roundResults[2].dartsUsed shouldBe 1
        }

        "Should record multiple darts in a round" {
            // GIVEN a checkout practice engine with target [40]
            val engine = createEngine(targets = listOf(40))

            // WHEN player hits S20 then D10 (2 darts to check out)
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)
            val (e2, _) = e1.addThrow(10, Multiplier.DOUBLE)
            val (e3, _) = e2.endTurn()

            // THEN round result should show 2 darts used
            val state = e3.getCheckoutPracticeState()!!
            state.roundResults[0].dartsUsed shouldBe 2
            state.roundResults[0].success shouldBe true
        }
    }

    "undoLastThrow" - {
        "Should allow undo within a round" {
            // GIVEN a checkout practice engine with target 40
            val engine = createEngine(targets = listOf(40, 32))

            // WHEN player hits S20
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)
            e1.getCurrentPlayerScore() shouldBe 20

            // AND undoes the throw
            val e2 = e1.undoLastThrow()

            // THEN score should revert to target
            e2 shouldNotBe null
            e2!!.getCurrentPlayerScore() shouldBe 40
        }

        "Should allow undo after bust" {
            // GIVEN a checkout practice engine with target 16
            val engine = createEngine(targets = listOf(16, 32))

            // WHEN player busts
            val (bustedEngine, _) = engine.addThrow(20, Multiplier.TRIPLE)

            // AND undoes the throw
            val undoneEngine = bustedEngine.undoLastThrow()

            // THEN score should revert and bust flag should be cleared
            undoneEngine shouldNotBe null
            undoneEngine!!.getCurrentPlayerScore() shouldBe 16
            undoneEngine.isTurnEnded() shouldBe false
        }

        "Should return null when no throws to undo" {
            // GIVEN a checkout practice engine with no throws
            val engine = createEngine()

            // WHEN trying to undo
            val result = engine.undoLastThrow()

            // THEN should return null
            result shouldBe null
        }
    }
})
