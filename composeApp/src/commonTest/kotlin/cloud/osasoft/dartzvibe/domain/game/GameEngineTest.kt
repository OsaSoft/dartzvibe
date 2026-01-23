package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
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
 * Tests for GameEngine - the core game logic.
 */
@OptIn(ExperimentalUuidApi::class)
class GameEngineTest : FreeSpec({

    val playerId1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val playerId2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")

    fun createEngine(
        playerIds: List<Uuid> = listOf(playerId1, playerId2),
        gameType: GameType = GameType.CLASSIC_501,
        doubleIn: Boolean = false,
        doubleOut: Boolean = true,
        legsToWin: Int = 1
    ): GameEngine {
        val session = GameSession(
            id = sessionId,
            config = GameConfig(
                gameType = gameType,
                doubleIn = doubleIn,
                doubleOut = doubleOut,
                playerIds = playerIds,
                legsToWin = legsToWin
            ),
            legs = listOf(Leg()),
            currentLegIndex = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = currentTimeMillis()
        )
        return GameEngine.fromSession(session)
    }

    "GameEngine initialization" - {
        "should start with first player" {
            val engine = createEngine()
            engine.getCurrentPlayerId() shouldBe playerId1
        }

        "should start with full score" {
            val engine = createEngine(gameType = GameType.CLASSIC_501)
            engine.getPlayerScore(playerId1) shouldBe 501
            engine.getPlayerScore(playerId2) shouldBe 501
        }

        "should start with empty throws" {
            val engine = createEngine()
            engine.getCurrentTurnThrows().shouldBeEmpty()
        }

        "should start with 0 legs won" {
            val engine = createEngine()
            engine.getLegsWon(playerId1) shouldBe 0
            engine.getLegsWon(playerId2) shouldBe 0
        }
    }

    "addThrow" - {
        "should add throw and update score" {
            val engine = createEngine()

            val (newEngine, result) = engine.addThrow(20, Multiplier.SINGLE)

            result.shouldBeInstanceOf<ThrowResult.Success>()
            (result as ThrowResult.Success).newScore shouldBe 481
            newEngine.getCurrentTurnThrows() shouldHaveSize 1
            newEngine.getCurrentPlayerScore() shouldBe 481
        }

        "should handle triple 20" {
            val engine = createEngine()

            val (newEngine, result) = engine.addThrow(20, Multiplier.TRIPLE)

            result.shouldBeInstanceOf<ThrowResult.Success>()
            (result as ThrowResult.Success).newScore shouldBe 441 // 501 - 60
        }

        "should handle double 20" {
            val engine = createEngine()

            val (newEngine, result) = engine.addThrow(20, Multiplier.DOUBLE)

            result.shouldBeInstanceOf<ThrowResult.Success>()
            (result as ThrowResult.Success).newScore shouldBe 461 // 501 - 40
        }

        "should handle bullseye (25 double)" {
            val engine = createEngine()

            val (newEngine, result) = engine.addThrow(25, Multiplier.DOUBLE)

            result.shouldBeInstanceOf<ThrowResult.Success>()
            (result as ThrowResult.Success).newScore shouldBe 451 // 501 - 50
        }

        "should handle outer bull (25 single)" {
            val engine = createEngine()

            val (newEngine, result) = engine.addThrow(25, Multiplier.SINGLE)

            result.shouldBeInstanceOf<ThrowResult.Success>()
            (result as ThrowResult.Success).newScore shouldBe 476 // 501 - 25
        }

        "should accumulate multiple throws" {
            var engine = createEngine()

            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // -60 = 441
            val (e2, _) = e1.addThrow(20, Multiplier.TRIPLE)     // -60 = 381
            val (e3, result) = e2.addThrow(20, Multiplier.TRIPLE) // -60 = 321

            result.shouldBeInstanceOf<ThrowResult.Success>()
            (result as ThrowResult.Success).newScore shouldBe 321
            e3.getCurrentTurnThrows() shouldHaveSize 3
        }
    }

    "Bust detection" - {
        "should bust when score goes below 0" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleOut = true,
                    playerIds = listOf(playerId1, playerId2)
                ),
                legs = listOf(Leg()),
                startedAt = currentTimeMillis()
            )
            // Create engine with low score by simulating turns
            var engine = GameEngine.fromSession(session)

            // Throw enough to get score close to 0
            // Start: 501, throw T20 x 8 = 480, remaining = 21
            repeat(8) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE) // Miss for player 2
                val (e4, _) = e3.endTurn()
                engine = e4
            }

            // Player 1 now has 21, throw T20 (60) would go to -39
            val (_, result) = engine.addThrow(20, Multiplier.TRIPLE)

            result.shouldBeInstanceOf<ThrowResult.Bust>()
            (result as ThrowResult.Bust).reason shouldBe "Score below zero"
        }

        "should bust when score equals 1 with double-out" {
            // Create a session where player has score of 21
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleOut = true,
                    playerIds = listOf(playerId1, playerId2)
                ),
                legs = listOf(Leg()),
                startedAt = currentTimeMillis()
            )
            var engine = GameEngine.fromSession(session)

            // Get to score of 21
            repeat(8) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }

            // Player 1 has 21, throw single 20 would leave 1
            val (_, result) = engine.addThrow(20, Multiplier.SINGLE)

            result.shouldBeInstanceOf<ThrowResult.Bust>()
            (result as ThrowResult.Bust).reason shouldBe "Score at 1 with double-out required"
        }

        "should bust when hitting exactly 0 without double (with double-out)" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleOut = true,
                    playerIds = listOf(playerId1, playerId2)
                ),
                legs = listOf(Leg()),
                startedAt = currentTimeMillis()
            )
            var engine = GameEngine.fromSession(session)

            // Get to score of 20
            // 501 - (8 * 60) = 501 - 480 = 21, then need 1 more point
            repeat(8) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // Score is 21, throw single 1 to get to 20
            val (e5, _) = engine.addThrow(1, Multiplier.SINGLE)
            val (e6, _) = e5.endTurn()
            val (e7, _) = e6.addThrow(0, Multiplier.SINGLE)
            val (e8, _) = e7.endTurn()
            engine = e8
            // Score is now 20

            // Try to finish with single 20 (should bust because not a double)
            val (_, result) = engine.addThrow(20, Multiplier.SINGLE)

            result.shouldBeInstanceOf<ThrowResult.Bust>()
            (result as ThrowResult.Bust).reason shouldBe "Must finish on a double"
        }

        "should NOT bust when hitting 0 without double (no double-out required)" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleOut = false, // No double-out required
                    playerIds = listOf(playerId1, playerId2)
                ),
                legs = listOf(Leg()),
                startedAt = currentTimeMillis()
            )
            var engine = GameEngine.fromSession(session)

            // Get to score of 20
            repeat(8) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            val (e5, _) = engine.addThrow(1, Multiplier.SINGLE)
            val (e6, _) = e5.endTurn()
            val (e7, _) = e6.addThrow(0, Multiplier.SINGLE)
            val (e8, _) = e7.endTurn()
            engine = e8

            // Finish with single 20 (should succeed - no double required)
            val (_, result) = engine.addThrow(20, Multiplier.SINGLE)

            result.shouldBeInstanceOf<ThrowResult.Checkout>()
        }
    }

    "Checkout" - {
        "should detect checkout with double" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleOut = true,
                    playerIds = listOf(playerId1, playerId2)
                ),
                legs = listOf(Leg()),
                startedAt = currentTimeMillis()
            )
            var engine = GameEngine.fromSession(session)

            // Get to score of 40
            repeat(7) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // 501 - 420 = 81
            val (e5, _) = engine.addThrow(1, Multiplier.SINGLE) // 80
            val (e6, _) = e5.addThrow(20, Multiplier.DOUBLE)    // 40
            val (e7, _) = e6.endTurn()
            val (e8, _) = e7.addThrow(0, Multiplier.SINGLE)
            val (e9, _) = e8.endTurn()
            engine = e9

            // Now at 40, checkout with D20
            val (_, result) = engine.addThrow(20, Multiplier.DOUBLE)

            result.shouldBeInstanceOf<ThrowResult.Checkout>()
            (result as ThrowResult.Checkout).winnerId shouldBe playerId1
        }

        "should detect checkout with bullseye (D25)" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleOut = true,
                    playerIds = listOf(playerId1, playerId2)
                ),
                legs = listOf(Leg()),
                startedAt = currentTimeMillis()
            )
            var engine = GameEngine.fromSession(session)

            // Get to score of 50
            repeat(7) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // 501 - 420 = 81
            val (e5, _) = engine.addThrow(1, Multiplier.SINGLE) // 80
            val (e6, _) = e5.addThrow(10, Multiplier.SINGLE)    // 70
            val (e7, _) = e6.addThrow(20, Multiplier.SINGLE)    // 50
            val (e8, _) = e7.endTurn()
            val (e9, _) = e8.addThrow(0, Multiplier.SINGLE)
            val (e10, _) = e9.endTurn()
            engine = e10

            // Now at 50, checkout with bullseye
            val (_, result) = engine.addThrow(25, Multiplier.DOUBLE)

            result.shouldBeInstanceOf<ThrowResult.Checkout>()
        }
    }

    "undoLastThrow" - {
        "should return null when no throws" {
            val engine = createEngine()

            val result = engine.undoLastThrow()

            result shouldBe null
        }

        "should remove last throw and restore score" {
            val engine = createEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // Score: 441

            val undone = e1.undoLastThrow()

            undone?.getCurrentTurnThrows()?.shouldBeEmpty()
            undone?.getCurrentPlayerScore() shouldBe 501
        }

        "should remove only last throw when multiple throws" {
            val engine = createEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)  // 441
            val (e2, _) = e1.addThrow(19, Multiplier.TRIPLE)      // 384

            val undone = e2.undoLastThrow()

            undone shouldNotBe null
            undone!!.getCurrentTurnThrows() shouldHaveSize 1
            undone.getCurrentPlayerScore() shouldBe 441
        }

        "should allow undo after bust" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleOut = true,
                    playerIds = listOf(playerId1, playerId2)
                ),
                legs = listOf(Leg()),
                startedAt = currentTimeMillis()
            )
            var engine = GameEngine.fromSession(session)

            // Get to low score
            repeat(8) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            // Score is 21

            // Bust by going below 0
            val (busted, result) = engine.addThrow(20, Multiplier.TRIPLE)
            result.shouldBeInstanceOf<ThrowResult.Bust>()

            // Undo should work
            val undone = busted.undoLastThrow()
            undone?.getCurrentPlayerScore() shouldBe 21
        }
    }

    "endTurn" - {
        "should switch to next player" {
            val engine = createEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)

            val (e2, result) = e1.endTurn()

            result.shouldBeInstanceOf<TurnResult.NextPlayer>()
            (result as TurnResult.NextPlayer).playerId shouldBe playerId2
            e2.getCurrentPlayerId() shouldBe playerId2
        }

        "should cycle back to first player" {
            val engine = createEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)
            val (e2, _) = e1.endTurn()
            val (e3, _) = e2.addThrow(20, Multiplier.SINGLE)

            val (e4, result) = e3.endTurn()

            result.shouldBeInstanceOf<TurnResult.NextPlayer>()
            (result as TurnResult.NextPlayer).playerId shouldBe playerId1
            e4.getCurrentPlayerId() shouldBe playerId1
        }

        "should preserve score after turn" {
            val engine = createEngine()
            val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE) // 441
            val (e2, _) = e1.endTurn()

            e2.getPlayerScore(playerId1) shouldBe 441
        }

        "should reset score on bust" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleOut = true,
                    playerIds = listOf(playerId1, playerId2)
                ),
                legs = listOf(Leg()),
                startedAt = currentTimeMillis()
            )
            var engine = GameEngine.fromSession(session)

            // Get to score of 21
            repeat(8) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }

            // Bust
            val (busted, _) = engine.addThrow(20, Multiplier.TRIPLE)
            val (afterTurn, _) = busted.endTurn()

            // Score should be reset to 21 (before the bust)
            afterTurn.getPlayerScore(playerId1) shouldBe 21
        }
    }

    "Leg winning" - {
        "should detect leg win" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleOut = true,
                    playerIds = listOf(playerId1, playerId2),
                    legsToWin = 3
                ),
                legs = listOf(Leg()),
                startedAt = currentTimeMillis()
            )
            var engine = GameEngine.fromSession(session)

            // Get to score of 40
            repeat(7) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            val (e5, _) = engine.addThrow(1, Multiplier.SINGLE)
            val (e6, _) = e5.addThrow(20, Multiplier.DOUBLE)
            val (e7, _) = e6.endTurn()
            val (e8, _) = e7.addThrow(0, Multiplier.SINGLE)
            val (e9, _) = e8.endTurn()
            engine = e9

            // Checkout
            val (e10, checkoutResult) = engine.addThrow(20, Multiplier.DOUBLE)
            checkoutResult.shouldBeInstanceOf<ThrowResult.Checkout>()

            val (e11, turnResult) = e10.endTurn()

            turnResult.shouldBeInstanceOf<TurnResult.LegWon>()
            val legWon = turnResult as TurnResult.LegWon
            legWon.winnerId shouldBe playerId1
            legWon.matchContinues shouldBe true

            e11.getLegsWon(playerId1) shouldBe 1
        }
    }

    "Match winning" - {
        "should detect match win when reaching legs to win" {
            val session = GameSession(
                id = sessionId,
                config = GameConfig(
                    gameType = GameType.CLASSIC_501,
                    doubleOut = true,
                    playerIds = listOf(playerId1, playerId2),
                    legsToWin = 1 // Single leg match
                ),
                legs = listOf(Leg()),
                startedAt = currentTimeMillis()
            )
            var engine = GameEngine.fromSession(session)

            // Get to score of 40
            repeat(7) {
                val (e1, _) = engine.addThrow(20, Multiplier.TRIPLE)
                val (e2, _) = e1.endTurn()
                val (e3, _) = e2.addThrow(0, Multiplier.SINGLE)
                val (e4, _) = e3.endTurn()
                engine = e4
            }
            val (e5, _) = engine.addThrow(1, Multiplier.SINGLE)
            val (e6, _) = e5.addThrow(20, Multiplier.DOUBLE)
            val (e7, _) = e6.endTurn()
            val (e8, _) = e7.addThrow(0, Multiplier.SINGLE)
            val (e9, _) = e8.endTurn()
            engine = e9

            // Checkout
            val (e10, _) = engine.addThrow(20, Multiplier.DOUBLE)
            val (e11, turnResult) = e10.endTurn()

            turnResult.shouldBeInstanceOf<TurnResult.MatchWon>()
            val matchWon = turnResult as TurnResult.MatchWon
            matchWon.winnerId shouldBe playerId1

            e11.toGameSession().status shouldBe GameStatus.COMPLETED
            e11.toGameSession().winnerId shouldBe playerId1
        }
    }

    "301 game" - {
        "should start with 301" {
            val engine = createEngine(gameType = GameType.CLASSIC_301)

            engine.getPlayerScore(playerId1) shouldBe 301
            engine.getPlayerScore(playerId2) shouldBe 301
        }
    }

    "Three players" - {
        "should cycle through three players" {
            val playerId3 = Uuid.parse("00000000-0000-0000-0000-000000000003")
            val engine = createEngine(playerIds = listOf(playerId1, playerId2, playerId3))

            engine.getCurrentPlayerId() shouldBe playerId1

            val (e1, _) = engine.addThrow(20, Multiplier.SINGLE)
            val (e2, _) = e1.endTurn()
            e2.getCurrentPlayerId() shouldBe playerId2

            val (e3, _) = e2.addThrow(20, Multiplier.SINGLE)
            val (e4, _) = e3.endTurn()
            e4.getCurrentPlayerId() shouldBe playerId3

            val (e5, _) = e4.addThrow(20, Multiplier.SINGLE)
            val (e6, _) = e5.endTurn()
            e6.getCurrentPlayerId() shouldBe playerId1
        }
    }
})
