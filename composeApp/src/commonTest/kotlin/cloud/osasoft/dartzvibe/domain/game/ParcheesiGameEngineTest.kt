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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for GameEngine with Parcheesi mode - count-up with knockout mechanics.
 */
@OptIn(ExperimentalUuidApi::class)
class ParcheesiGameEngineTest : FreeSpec({

    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val playerId3 = Uuid.parse("00000000-0000-0000-0000-000000000003")
    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")

    fun createParcheesiEngine(
        playerIds: List<Uuid> = listOf(playerId1, playerId2),
        gameType: GameType = GameType.CLASSIC_301,
        doubleOut: Boolean = false,
        legsToWin: Int = 1,
    ): GameEngine {
        val session = GameSession(
            id = sessionId,
            config = GameConfig(
                gameType = gameType,
                gameMode = GameMode.PARCHEESI,
                doubleIn = false,
                doubleOut = doubleOut,
                playerIds = playerIds,
                legsToWin = legsToWin,
            ),
            legs = listOf(Leg()),
            currentLegIndex = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = currentTimeMillis(),
        )
        return GameEngine.fromSession(session)
    }

    "Parcheesi initialization" - {
        "Should start with score 0" {
            // GIVEN a Parcheesi game

            // WHEN engine is created
            val engine = createParcheesiEngine()

            // THEN all players start at 0
            engine.getPlayerScore(playerId1) shouldBe 0
            engine.getPlayerScore(playerId2) shouldBe 0
        }

        "Should have target score from game type" {
            // GIVEN a Parcheesi 301 game
            val engine301 = createParcheesiEngine(gameType = GameType.CLASSIC_301)

            // THEN target score is 301
            engine301.config.targetScore shouldBe 301

            // GIVEN a Parcheesi 501 game
            val engine501 = createParcheesiEngine(gameType = GameType.CLASSIC_501)

            // THEN target score is 501
            engine501.config.targetScore shouldBe 501
        }

        "Should be marked as count-up mode" {
            // GIVEN a Parcheesi game

            // WHEN engine is created
            val engine = createParcheesiEngine()

            // THEN isCountUp is true
            engine.config.isCountUp shouldBe true
        }
    }

    "Count-up scoring" - {
        "Should add throw score to current score" {
            // GIVEN a Parcheesi game with player at score 0
            val engine = createParcheesiEngine()

            // WHEN player throws single 20
            val (newEngine, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN score increases by 20
            result.shouldBeInstanceOf<ThrowResult.Success>()
            (result as ThrowResult.Success).newScore shouldBe 20
            newEngine.getCurrentPlayerScore() shouldBe 20
        }

        "Should accumulate multiple throws in a turn" {
            // GIVEN a Parcheesi game
            var engine = createParcheesiEngine()

            // WHEN player throws T20, T20, T20
            val (e1, r1) = engine.addThrow(20, Multiplier.TRIPLE) // +60 = 60
            val (e2, r2) = e1.addThrow(20, Multiplier.TRIPLE) // +60 = 120
            val (e3, r3) = e2.addThrow(20, Multiplier.TRIPLE) // +60 = 180

            // THEN score accumulates correctly
            (r1 as ThrowResult.Success).newScore shouldBe 60
            (r2 as ThrowResult.Success).newScore shouldBe 120
            (r3 as ThrowResult.Success).newScore shouldBe 180
            e3.getCurrentTurnThrows() shouldHaveSize 3
        }

        "Should handle bullseye correctly" {
            // GIVEN a Parcheesi game
            val engine = createParcheesiEngine()

            // WHEN player throws bullseye (D25 = 50)
            val (newEngine, result) = engine.addThrow(25, Multiplier.DOUBLE)

            // THEN score is 50
            result.shouldBeInstanceOf<ThrowResult.Success>()
            (result as ThrowResult.Success).newScore shouldBe 50
        }
    }

    "Bounce-back" - {
        "Should bounce back when overshooting target" {
            // GIVEN a Parcheesi 301 game with player at 280
            var engine = createParcheesiEngine()
            // Get player 1 to score 280
            repeat(4) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // +60
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE) // Miss for player 2
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // Now at 240, throw T20 to get to 300
            val (e5, _) = engine.addThrow(20, Multiplier.TRIPLE)
            val (e6, _) = e5.endTurn()
            val (e7, _) = e6.addThrow(0, Multiplier.SINGLE)
            val (e8, _) = e7.endTurn()
            engine = e8
            // Player 1 now at 300

            // WHEN player throws single 20 (300 + 20 = 320, overshoot by 19)
            val (newEngine, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN bounce back to 301 - 19 = 282
            result.shouldBeInstanceOf<ThrowResult.BounceBack>()
            val bounceBack = result as ThrowResult.BounceBack
            bounceBack.newScore shouldBe 282
            bounceBack.overshoot shouldBe 19
        }

        "Should calculate correct bounce-back score" {
            // GIVEN a Parcheesi 301 game with player at 281
            var engine = createParcheesiEngine()
            // Get player 1 to score 281
            repeat(4) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // +60
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // At 240, throw T20 + 1 = 301
            val (e5, _) = engine.addThrow(20, Multiplier.TRIPLE) // 300
            val (e6, _) = e5.addThrow(1, Multiplier.SINGLE) // 301
            val (e7, _) = e6.endTurn()
            val (e8, _) = e7.addThrow(0, Multiplier.SINGLE)
            val (e9, _) = e8.endTurn()
            engine = e9
            // Hmm, wait - 301 is checkout not overshoot. Let me get to 281
            // Actually, with doubleOut = false, hitting exactly 301 should be checkout
            // Let me restructure this test

            // GIVEN a Parcheesi 301 game with player at 281
            engine = createParcheesiEngine()
            // Get player 1 to score 281
            repeat(4) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // +60
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // At 240
            val (e10, _) = engine.addThrow(20, Multiplier.DOUBLE) // +40 = 280
            val (e11, _) = e10.addThrow(1, Multiplier.SINGLE) // +1 = 281
            val (e12, _) = e11.endTurn()
            val (e13, _) = e12.addThrow(0, Multiplier.SINGLE)
            val (e14, _) = e13.endTurn()
            engine = e14
            // Player 1 now at 281

            // WHEN player throws 25 (281 + 25 = 306, overshoot by 5)
            val (newEngine, result) = engine.addThrow(25, Multiplier.SINGLE)

            // THEN bounce back to 301 - 5 = 296
            result.shouldBeInstanceOf<ThrowResult.BounceBack>()
            val bounceBack = result as ThrowResult.BounceBack
            bounceBack.newScore shouldBe 296
            bounceBack.overshoot shouldBe 5
        }

        "Should end turn immediately on bounce-back" {
            // GIVEN a Parcheesi game with player close to target
            var engine = createParcheesiEngine()
            repeat(4) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // At 240
            val (e5, _) = engine.addThrow(20, Multiplier.TRIPLE) // 300
            val (e6, _) = e5.endTurn()
            val (e7, _) = e6.addThrow(0, Multiplier.SINGLE)
            val (e8, _) = e7.endTurn()
            engine = e8
            // Player 1 at 300

            // WHEN player throws overshoot
            val (bounced, result) = engine.addThrow(20, Multiplier.SINGLE)
            result.shouldBeInstanceOf<ThrowResult.BounceBack>()

            // THEN turn is ended
            bounced.isTurnEnded() shouldBe true
        }

        "Should not allow more throws after bounce-back" {
            // GIVEN a Parcheesi game where player just bounced
            var engine = createParcheesiEngine()
            repeat(5) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // Player 1 at 300
            val (bounced, _) = engine.addThrow(20, Multiplier.SINGLE) // Bounce

            // WHEN trying to throw again
            val (_, result) = bounced.addThrow(1, Multiplier.SINGLE)

            // THEN it's treated as turn ended
            result.shouldBeInstanceOf<ThrowResult.Bust>()
            (result as ThrowResult.Bust).reason shouldBe "Turn already ended"
        }
    }

    "Knockout mechanic" - {
        "Should detect knockout when matching another player's score" {
            // GIVEN a Parcheesi game with player 2 at score 60
            var engine = createParcheesiEngine()
            // Player 1 turn - throw 20
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)
            val (e2, _) = e1.endTurn()
            // Player 2 turn - throw T20 to get to 60
            val (e3, _) = e2.addThrow(20, Multiplier.TRIPLE) // 60
            val (e4, _) = e3.endTurn()
            engine = e4
            // Player 1 at 20, Player 2 at 60

            // WHEN player 1 throws D20 to also reach 60
            val (e5, result) = engine.addThrow(20, Multiplier.DOUBLE) // 20 + 40 = 60

            // THEN player 2 is knocked out immediately
            result.shouldBeInstanceOf<ThrowResult.SuccessWithKnockout>()
            val knockout = result as ThrowResult.SuccessWithKnockout
            knockout.knockedOutPlayerIds shouldBe listOf(playerId2)

            // AND player 2's score is reset to 0
            e5.getPlayerScore(playerId2) shouldBe 0
        }

        "Should reset knocked-out player to 0" {
            // GIVEN a setup where player 1 will match player 2's score
            var engine = createParcheesiEngine()
            // Player 1: 0, Player 2: 20
            val (e1, _) = engine.addThrow(0, Multiplier.SINGLE) // Miss
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(20, Multiplier.SINGLE) // Player 2 at 20
            val (e4, _) = e3.endTurn()
            engine = e4

            // WHEN player 1 throws 20 to match
            val (e5, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN player 2 is at 0 immediately
            result.shouldBeInstanceOf<ThrowResult.SuccessWithKnockout>()
            e5.getPlayerScore(playerId2) shouldBe 0
        }

        "Should not knock out at score 0" {
            // GIVEN a Parcheesi game at start (both players at 0)
            val engine = createParcheesiEngine()

            // WHEN player 1 misses (stays at 0)
            val (e1, _) = engine.addThrow(0, Multiplier.SINGLE)
            val (e2, result) = e1.endTurn()

            // THEN no knockout (can't knock out at 0)
            result.shouldBeInstanceOf<TurnResult.NextPlayer>()
        }

        "Should handle multiple knockouts in one throw" {
            // GIVEN a Parcheesi game with 3 players, 2 at same score
            var engine = createParcheesiEngine(playerIds = listOf(playerId1, playerId2, playerId3))
            // Get players 2 and 3 to score 20
            val (e1, _) = engine.addThrow(0, Multiplier.SINGLE) // P1 stays at 0
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(20, Multiplier.SINGLE) // P2 at 20
            val (e4, _) = e3.endTurn()
            val (e5, _) = e4.addThrow(20, Multiplier.SINGLE) // P3 at 20
            val (e6, _) = e5.endTurn()
            engine = e6
            // P1: 0, P2: 20, P3: 20

            // WHEN player 1 throws 20 to match both
            val (e7, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN both players 2 and 3 are knocked out immediately
            result.shouldBeInstanceOf<ThrowResult.SuccessWithKnockout>()
            val knockout = result as ThrowResult.SuccessWithKnockout
            knockout.knockedOutPlayerIds shouldHaveSize 2
            knockout.knockedOutPlayerIds shouldBe listOf(playerId2, playerId3)

            // AND both are reset to 0
            e7.getPlayerScore(playerId2) shouldBe 0
            e7.getPlayerScore(playerId3) shouldBe 0
        }

        "Should trigger knockout immediately on any throw" {
            // GIVEN a Parcheesi game with player 2 at 20
            var engine = createParcheesiEngine()
            val (e1, _) = engine.addThrow(0, Multiplier.SINGLE)
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(20, Multiplier.SINGLE) // P2 at 20
            val (e4, _) = e3.endTurn()
            engine = e4

            // WHEN player 1 throws 20 (matching P2)
            val (e5, r5) = engine.addThrow(20, Multiplier.SINGLE) // P1 at 20 (matches!)

            // THEN knockout is triggered immediately
            r5.shouldBeInstanceOf<ThrowResult.SuccessWithKnockout>()
            val knockout = r5 as ThrowResult.SuccessWithKnockout
            knockout.knockedOutPlayerIds shouldBe listOf(playerId2)
            knockout.newScore shouldBe 20

            // AND P2's score is reset to 0 immediately
            e5.getPlayerScore(playerId2) shouldBe 0
        }

        "Should allow continued throwing after knockout" {
            // GIVEN a Parcheesi game with player 2 at 20
            var engine = createParcheesiEngine()
            val (e1, _) = engine.addThrow(0, Multiplier.SINGLE)
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(20, Multiplier.SINGLE) // P2 at 20
            val (e4, _) = e3.endTurn()
            engine = e4

            // WHEN player 1 throws 20 (knockout), then continues throwing
            val (e5, r5) = engine.addThrow(20, Multiplier.SINGLE) // Knockout!
            r5.shouldBeInstanceOf<ThrowResult.SuccessWithKnockout>()

            // Player 1 can still throw
            val (e6, r6) = e5.addThrow(20, Multiplier.SINGLE) // P1 at 40
            r6.shouldBeInstanceOf<ThrowResult.Success>()
            (r6 as ThrowResult.Success).newScore shouldBe 40

            // THEN turn ends normally
            val (e7, result) = e6.endTurn()
            result.shouldBeInstanceOf<TurnResult.NextPlayer>()
            e7.getPlayerScore(playerId1) shouldBe 40
            e7.getPlayerScore(playerId2) shouldBe 0 // Still knocked out
        }

        "Should handle multiple knockouts from different throws in same turn" {
            // GIVEN a Parcheesi game with 3 players
            // P2 at 20, P3 at 40
            var engine = createParcheesiEngine(playerIds = listOf(playerId1, playerId2, playerId3))
            // P1 misses
            val (e1, _) = engine.addThrow(0, Multiplier.SINGLE)
            val (e2, _) = e1.endTurn()
            // P2 throws 20
            val (e3, _) = e2.addThrow(20, Multiplier.SINGLE)
            val (e4, _) = e3.endTurn()
            // P3 throws 40
            val (e5, _) = e4.addThrow(20, Multiplier.DOUBLE)
            val (e6, _) = e5.endTurn()
            engine = e6
            // P1: 0, P2: 20, P3: 40

            // WHEN player 1 throws 20 (knocking out P2)
            val (e7, r7) = engine.addThrow(20, Multiplier.SINGLE)
            r7.shouldBeInstanceOf<ThrowResult.SuccessWithKnockout>()
            val ko1 = r7 as ThrowResult.SuccessWithKnockout
            ko1.knockedOutPlayerIds shouldBe listOf(playerId2)
            e7.getPlayerScore(playerId2) shouldBe 0

            // THEN throws again to reach 40 (knocking out P3)
            val (e8, r8) = e7.addThrow(20, Multiplier.SINGLE) // P1 at 40
            r8.shouldBeInstanceOf<ThrowResult.SuccessWithKnockout>()
            val ko2 = r8 as ThrowResult.SuccessWithKnockout
            ko2.knockedOutPlayerIds shouldBe listOf(playerId3)
            e8.getPlayerScore(playerId3) shouldBe 0

            // Both players should be at 0 after the turn
            val (e9, _) = e8.endTurn()
            e9.getPlayerScore(playerId1) shouldBe 40
            e9.getPlayerScore(playerId2) shouldBe 0
            e9.getPlayerScore(playerId3) shouldBe 0
        }

        "Should not change current player after mid-turn knockout" {
            // GIVEN a Parcheesi game with player 2 at 20
            var engine = createParcheesiEngine()
            // Player 1 throws 20 and ends turn
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)
            val (e2, _) = e1.endTurn()
            engine = e2
            // Now it's player 2's turn, P1: 20, P2: 0

            // WHEN player 2's first throw lands on 20 (matching P1)
            val (e3, r3) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN knockout is triggered
            r3.shouldBeInstanceOf<ThrowResult.SuccessWithKnockout>()
            val knockout = r3 as ThrowResult.SuccessWithKnockout
            knockout.knockedOutPlayerIds shouldBe listOf(playerId1)

            // AND player 2 is STILL the current player (can continue throwing)
            e3.getCurrentPlayerId() shouldBe playerId2

            // AND player 2 can continue to throw
            val (e4, r4) = e3.addThrow(5, Multiplier.SINGLE)
            r4.shouldBeInstanceOf<ThrowResult.Success>()
            (r4 as ThrowResult.Success).newScore shouldBe 25

            // AND player 2 is still the current player
            e4.getCurrentPlayerId() shouldBe playerId2
        }
    }

    "Checkout" - {
        "Should detect checkout at exact target score" {
            // GIVEN a Parcheesi 301 game with player at 281 (needs 20)
            var engine = createParcheesiEngine()
            repeat(4) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // At 240
            val (e5, _) = engine.addThrow(20, Multiplier.DOUBLE) // 280
            val (e6, _) = e5.addThrow(1, Multiplier.SINGLE) // 281
            val (e7, _) = e6.endTurn()
            val (e8, _) = e7.addThrow(0, Multiplier.SINGLE)
            val (e9, _) = e8.endTurn()
            engine = e9
            // Player 1 at 281

            // WHEN player throws 20 to reach exactly 301
            val (_, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN checkout is detected
            result.shouldBeInstanceOf<ThrowResult.Checkout>()
            (result as ThrowResult.Checkout).winnerId shouldBe playerId1
        }

        "Should require double for checkout when doubleOut enabled" {
            // GIVEN a Parcheesi 301 game with doubleOut at 281
            var engine = createParcheesiEngine(doubleOut = true)
            repeat(4) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // At 240
            val (e5, _) = engine.addThrow(20, Multiplier.DOUBLE) // 280
            val (e6, _) = e5.addThrow(1, Multiplier.SINGLE) // 281
            val (e7, _) = e6.endTurn()
            val (e8, _) = e7.addThrow(0, Multiplier.SINGLE)
            val (e9, _) = e8.endTurn()
            engine = e9
            // Player 1 at 281

            // WHEN player throws S20 (not double) to reach exactly 301
            val (_, result) = engine.addThrow(20, Multiplier.SINGLE)

            // THEN it bounces back (not checkout without double)
            result.shouldBeInstanceOf<ThrowResult.BounceBack>()
        }

        "Should checkout with double when doubleOut enabled" {
            // GIVEN a Parcheesi 301 game with doubleOut at 261 (needs 40)
            var engine = createParcheesiEngine(doubleOut = true)
            repeat(4) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // At 240
            val (e5, _) = engine.addThrow(20, Multiplier.SINGLE) // 260
            val (e6, _) = e5.addThrow(1, Multiplier.SINGLE) // 261
            val (e7, _) = e6.endTurn()
            val (e8, _) = e7.addThrow(0, Multiplier.SINGLE)
            val (e9, _) = e8.endTurn()
            engine = e9
            // Player 1 at 261

            // WHEN player throws D20 (40) to reach exactly 301
            val (_, result) = engine.addThrow(20, Multiplier.DOUBLE)

            // THEN checkout is detected
            result.shouldBeInstanceOf<ThrowResult.Checkout>()
        }
    }

    "Undo" - {
        "Should correctly undo throws with count-up math" {
            // GIVEN a Parcheesi game with some throws
            val engine = createParcheesiEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // 60
            val (e2, _) = e1.addThrow(19, Multiplier.TRIPLE) // 117

            // WHEN undo is called
            val undone = e2.undoLastThrow()

            // THEN score is correctly restored
            undone shouldNotBe null
            undone!!.getCurrentTurnThrows() shouldHaveSize 1
            undone.getCurrentPlayerScore() shouldBe 60
        }

        "Should clear bounce-back state on undo" {
            // GIVEN a Parcheesi game where player bounced
            var engine = createParcheesiEngine()
            repeat(5) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // Player 1 at 300
            val (bounced, result) = engine.addThrow(20, Multiplier.SINGLE) // Bounce
            result.shouldBeInstanceOf<ThrowResult.BounceBack>()
            bounced.isTurnEnded() shouldBe true

            // WHEN undo is called
            val undone = bounced.undoLastThrow()

            // THEN bounce state is cleared
            undone shouldNotBe null
            undone!!.isTurnEnded() shouldBe false
            undone.getCurrentPlayerScore() shouldBe 300
        }

        "Should restore score to 0 when undoing first throw" {
            // GIVEN a Parcheesi game with one throw
            val engine = createParcheesiEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)

            // WHEN undo is called
            val undone = e1.undoLastThrow()

            // THEN score is back to 0
            undone shouldNotBe null
            undone!!.getCurrentTurnThrows().shouldBeEmpty()
            undone.getCurrentPlayerScore() shouldBe 0
        }
    }

    "Match winning" - {
        "Should win match when reaching target score" {
            // GIVEN a Parcheesi 301 game with player at 281
            var engine = createParcheesiEngine(legsToWin = 1)
            repeat(4) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            val (e5, _) = engine.addThrow(20, Multiplier.DOUBLE) // 280
            val (e6, _) = e5.addThrow(1, Multiplier.SINGLE) // 281
            val (e7, _) = e6.endTurn()
            val (e8, _) = e7.addThrow(0, Multiplier.SINGLE)
            val (e9, _) = e8.endTurn()
            engine = e9

            // WHEN player throws 20 to checkout
            val (e10, checkoutResult) = engine.addThrow(20, Multiplier.SINGLE)
            checkoutResult.shouldBeInstanceOf<ThrowResult.Checkout>()

            val (e11, turnResult) = e10.endTurn()

            // THEN match is won
            turnResult.shouldBeInstanceOf<TurnResult.MatchWon>()
            val matchWon = turnResult as TurnResult.MatchWon
            matchWon.winnerId shouldBe playerId1

            e11.toGameSession().status shouldBe GameStatus.COMPLETED
            e11.toGameSession().winnerId shouldBe playerId1
        }
    }
})
