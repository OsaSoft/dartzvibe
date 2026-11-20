package cloud.osasoft.dartzvibe.domain.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Multiplier.DOUBLE
import cloud.osasoft.dartzvibe.data.model.Multiplier.SINGLE
import cloud.osasoft.dartzvibe.data.model.Multiplier.TRIPLE
import cloud.osasoft.dartzvibe.data.model.Throw
import cloud.osasoft.dartzvibe.data.model.Turn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TurnRewindTest : FreeSpec({

    val p1 = Uuid.parse("00000000-0000-0000-0000-000000000001")
    val p2 = Uuid.parse("00000000-0000-0000-0000-000000000002")
    val p3 = Uuid.parse("00000000-0000-0000-0000-000000000003")
    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000100")

    fun t(segment: Int, multiplier: Multiplier = SINGLE) = Throw(segment, multiplier)

    fun session(config: GameConfig) = GameSession(
        id = sessionId,
        config = config,
        legs = listOf(Leg()),
        currentLegIndex = 0,
        status = GameStatus.IN_PROGRESS,
        startedAt = 0L,
    )

    fun GameEngine.play(throws: List<Throw>): GameEngine =
        throws.fold(this) { engine, throwObj -> engine.addThrow(throwObj.segment, throwObj.multiplier).first }

    fun GameEngine.playWithResult(throws: List<Throw>): Pair<GameEngine, ThrowResult?> =
        throws.fold(this to null as ThrowResult?) { (engine, _), throwObj ->
            engine.addThrow(throwObj.segment, throwObj.multiplier)
        }

    fun GameEngine.playTurns(turns: List<List<Throw>>): GameEngine =
        turns.fold(this) { engine, throws -> engine.play(throws).endTurn().first }

    fun assertSameState(actual: GameEngine, expected: GameEngine) {
        actual.toGameSession() shouldBe expected.toGameSession()
        actual.getCurrentPlayerId() shouldBe expected.getCurrentPlayerId()
        actual.getCurrentTurnThrows() shouldBe expected.getCurrentTurnThrows()
        actual.getCurrentPlayerScore() shouldBe expected.getCurrentPlayerScore()
        actual.isTurnEnded() shouldBe expected.isTurnEnded()
        actual.getCricketState() shouldBe expected.getCricketState()
        actual.getRouletteState() shouldBe expected.getRouletteState()
        actual.getCheckoutPracticeState() shouldBe expected.getCheckoutPracticeState()
        expected.config.playerIds.forEach { actual.getPlayerScore(it) shouldBe expected.getPlayerScore(it) }
    }

    data class Script(
        val name: String,
        val config: GameConfig,
        val turns: List<List<Throw>>,
        val expectedLastResult: (TurnResult) -> Boolean,
    )

    val classicConfig = GameConfig(
        gameType = GameType.CLASSIC_301,
        gameMode = GameMode.CLASSIC,
        doubleOut = true,
        playerIds = listOf(p1, p2),
        legsToWin = 2,
    )
    val parcheesiConfig = GameConfig(
        gameType = GameType.CLASSIC_301,
        gameMode = GameMode.PARCHEESI,
        doubleOut = false,
        playerIds = listOf(p1, p2),
    )

    val scripts = listOf(
        Script(
            name = "Classic 301 with a bust, a leg win and a match win",
            config = classicConfig,
            turns = listOf(
                listOf(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE)), // P1 121
                listOf(t(1), t(1), t(1)), // P2 298
                listOf(t(20, TRIPLE), t(19, TRIPLE), t(4)), // P1 bust (0 without a double)
                listOf(t(20, TRIPLE)), // P2 238
                listOf(t(20, TRIPLE), t(19, TRIPLE), t(2, DOUBLE)), // P1 wins leg 1
                listOf(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE)), // leg 2: P1 121
                listOf(t(1)), // P2 300
                listOf(t(20, TRIPLE), t(19, TRIPLE), t(2, DOUBLE)), // P1 wins the match
            ),
            expectedLastResult = { it is TurnResult.MatchWon },
        ),
        Script(
            name = "Classic 301 double-in, doubling in mid-turn",
            config = classicConfig.copy(doubleIn = true, legsToWin = 1),
            turns = listOf(
                listOf(t(20, TRIPLE), t(20), t(5)), // P1 not in yet: 301
                listOf(t(20), t(10, DOUBLE), t(20, TRIPLE)), // P2 doubles in with the second dart: 221
                listOf(t(20, DOUBLE), t(20, TRIPLE)), // P1 in: 201
                listOf(t(20)), // P2 already in: 201
            ),
            expectedLastResult = { it is TurnResult.NextPlayer },
        ),
        Script(
            name = "Cricket with points, closed numbers and a win",
            config = GameConfig(
                gameType = GameType.CRICKET_REGULAR,
                gameMode = GameMode.CRICKET,
                doubleOut = false,
                playerIds = listOf(p1, p2),
            ),
            turns = listOf(
                listOf(t(20, TRIPLE), t(20, TRIPLE), t(19, DOUBLE)), // P1 closes 20, 60 points
                listOf(t(19, TRIPLE), t(19, SINGLE), t(5)), // P2 closes 19, 19 points
                listOf(t(19), t(18, TRIPLE), t(17, TRIPLE)), // P1 closes 19, 18, 17
                listOf(t(0), t(0), t(0)),
                listOf(t(16, TRIPLE), t(15, TRIPLE), t(25, DOUBLE)),
                listOf(t(20)), // P2 a mark on 20
                listOf(t(25)), // P1 closes the bull and wins
            ),
            expectedLastResult = { it is TurnResult.MatchWon },
        ),
        Script(
            name = "Roulette Rounds with a tie into sudden death",
            config = GameConfig(
                gameType = GameType.ROULETTE_ROUNDS,
                gameMode = GameMode.ROULETTE,
                doubleOut = false,
                playerIds = listOf(p1, p2),
                rouletteTargetSegments = listOf(7, 12, 3),
                rouletteRounds = 2,
            ),
            turns = listOf(
                listOf(t(7, TRIPLE)), // P1 3
                listOf(t(7), t(1)), // P2 1, round 2
                listOf(t(0)), // P1 3
                listOf(t(12, DOUBLE)), // P2 3: tie after the last round, sudden death on 3
                listOf(t(3)), // P1 4
                listOf(t(0), t(0), t(0)), // P2 3: P1 wins
            ),
            expectedLastResult = { it is TurnResult.MatchWon },
        ),
        Script(
            name = "Roulette Score reaching the target",
            config = GameConfig(
                gameType = GameType.ROULETTE_SCORE,
                gameMode = GameMode.ROULETTE,
                doubleOut = false,
                playerIds = listOf(p1, p2),
                rouletteTargetSegments = listOf(7, 12),
                rouletteTargetScore = 5,
            ),
            turns = listOf(
                listOf(t(7, TRIPLE)),
                listOf(t(7)),
                listOf(t(12, TRIPLE)), // P1 6
                listOf(t(0)), // round ends: P1 wins
            ),
            expectedLastResult = { it is TurnResult.MatchWon },
        ),
        Script(
            name = "Checkout Practice with a success, a miss, a bust and completion",
            config = GameConfig(
                gameType = GameType.CHECKOUT_EASY,
                gameMode = GameMode.CHECKOUT_PRACTICE,
                doubleOut = true,
                playerIds = listOf(p1),
                checkoutPracticeTargets = listOf(40, 32, 5, 3),
            ),
            turns = listOf(
                listOf(t(20, DOUBLE)), // 40 checked out
                listOf(t(16), t(10), t(2)), // 32 missed
                listOf(t(1), t(3)), // 5 busted at 1
                listOf(t(1), t(1, DOUBLE)), // 3 checked out: practice complete
            ),
            expectedLastResult = { it is TurnResult.MatchWon },
        ),
        Script(
            name = "Parcheesi with knockouts, a bounce and a win",
            config = parcheesiConfig,
            turns = listOf(
                listOf(t(20)), // P1 20
                listOf(t(20), t(1)), // P2 knocks P1 back to 0 with the first dart, ends on 21
                listOf(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE)), // P1 180
                listOf(t(1)), // P2 22
                listOf(t(20, TRIPLE), t(20, TRIPLE)), // P1 300
                listOf(t(1)), // P2 23
                listOf(t(20)), // P1 bounces to 282
                listOf(t(1)), // P2 24
                listOf(t(19)), // P1 301: wins
            ),
            expectedLastResult = { it is TurnResult.MatchWon },
        ),
        Script(
            name = "Parcheesi with three players and a double knockout",
            config = parcheesiConfig.copy(playerIds = listOf(p1, p2, p3), legsToWin = 2),
            turns = listOf(
                listOf(t(20)), // P1 20
                listOf(t(20)), // P2 20 knocks P1
                listOf(t(20), t(20)), // P3 20 knocks P2, 40
                listOf(t(20), t(20)), // P1 40 knocks P3
                listOf(t(1)),
            ),
            expectedLastResult = { it is TurnResult.NextPlayer },
        ),
    )

    "Rewinding right after End Turn" - {
        "Should restore exactly the state before End Turn" - {
            withData(nameFn = { it.name }, scripts) { script ->
                // GIVEN a scripted game played turn by turn
                val start = GameEngine.fromSession(session(script.config)) to null as TurnResult?
                val (_, lastResult) = script.turns.fold(start) { (engine, _), throws ->
                    val (beforeEndTurn, lastDartResult) = engine.playWithResult(throws)
                    val (afterEndTurn, turnResult) = beforeEndTurn.endTurn()
                    val crossesLeg = turnResult is TurnResult.LegWon || turnResult is TurnResult.MatchWon

                    // AND the same state once saved and resumed
                    val resumed = GameEngine.fromSession(afterEndTurn.toGameSession())
                    listOf(afterEndTurn, resumed).forEach { candidate ->
                        // WHEN the turn is rewound (from the dialog when it won the leg or match)
                        val rewound = if (crossesLeg) {
                            TurnRewind.rewindTurn(candidate.toGameSession(), crossLegBoundary = true)
                        } else {
                            TurnRewind.undo(candidate)
                        }

                        // THEN the player is back mid-turn exactly as before End Turn, except that a Checkout
                        // Practice round comes back without its checkout dart (live again, as before that dart)
                        rewound.shouldNotBeNull()
                        val isPracticeCheckout = script.config.isCheckoutPractice &&
                            lastDartResult is ThrowResult.Checkout
                        val expected = if (isPracticeCheckout) {
                            TurnRewind.undoLastThrow(beforeEndTurn).shouldNotBeNull()
                        } else {
                            RewindResult(beforeEndTurn, lastDartResult)
                        }
                        assertSameState(rewound.engine, expected.engine)
                        // AND the last dart's result (bust, bounce, checkout...) drives the UI as before
                        rewound.lastThrowResult shouldBe expected.lastThrowResult
                        // AND the normal Undo never reopens a finished leg
                        if (crossesLeg) TurnRewind.undo(candidate).shouldBeNull()
                    }
                    afterEndTurn to turnResult
                }

                // AND the script ended the way it was meant to
                script.expectedLastResult(lastResult.shouldNotBeNull()) shouldBe true
            }
        }

        "Should undo the reopened turn's darts one at a time and end it again" {
            // GIVEN a Classic game where P2 just ended a turn of three darts
            val engine = GameEngine.fromSession(session(classicConfig))
                .playTurns(listOf(listOf(t(20, TRIPLE)), listOf(t(1), t(2), t(3))))

            // WHEN Undo is pressed with no darts thrown
            val reopened = TurnRewind.undo(engine).shouldNotBeNull()

            // THEN P2's turn reopens with all three darts and their score before ending it
            reopened.engine.getCurrentPlayerId() shouldBe p2
            reopened.engine.getCurrentTurnThrows() shouldBe listOf(t(1), t(2), t(3))
            reopened.engine.getCurrentPlayerScore() shouldBe 295

            // WHEN Undo is pressed again
            val oneDartLess = TurnRewind.undo(reopened.engine).shouldNotBeNull()

            // THEN the last dart is removed
            oneDartLess.engine.getCurrentTurnThrows() shouldBe listOf(t(1), t(2))
            oneDartLess.engine.getCurrentPlayerScore() shouldBe 298

            // WHEN another dart is thrown and the turn ended
            val (ended, result) = oneDartLess.engine.play(listOf(t(20))).endTurn()

            // THEN it ends normally
            result shouldBe TurnResult.NextPlayer(p1)
            ended.getPlayerScore(p2) shouldBe 278
        }

        "Should go back through every turn of the leg to the empty leg" - {
            withData(nameFn = { it.name }, scripts) { script ->
                // GIVEN a scripted game stopped before its final turn, then saved and resumed
                val initial = session(script.config)
                val played = GameEngine.fromSession(initial).playTurns(script.turns.dropLast(1))
                val resumed = GameEngine.fromSession(played.toGameSession())
                val legStart = played.toGameSession().let { s ->
                    s.copy(legs = s.legs.dropLast(1) + Leg())
                }

                // WHEN Undo is pressed until it does nothing (a Practice checkout round reopens without its
                // checkout dart, so it takes one press fewer)
                val darts = script.turns.dropLast(1).let { turns ->
                    val legTurns = played.toGameSession().currentLeg.playerTurns
                    val practiceCheckouts = if (script.config.isCheckoutPractice) {
                        legTurns.count { it.scoreAfterTurn == 0 && !it.isBust }
                    } else {
                        0
                    }
                    turns.takeLast(legTurns.size).sumOf { it.size } + legTurns.size - practiceCheckouts
                }
                val rewound = (1..darts).fold(resumed) { engine, _ ->
                    TurnRewind.undo(engine).shouldNotBeNull().engine
                }

                // THEN the leg is back to how it started, and Undo is disabled
                rewound.toGameSession() shouldBe legStart
                TurnRewind.canUndo(rewound) shouldBe false
                TurnRewind.undo(rewound).shouldBeNull()
            }
        }
    }

    "Leg boundary" - {
        "Should not undo into the finished leg from the new leg's first turn" {
            // GIVEN a Classic best-of-3 where P1 just won leg 1
            val engine = GameEngine.fromSession(session(classicConfig)).playTurns(
                listOf(
                    listOf(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE)),
                    listOf(t(1)),
                    listOf(t(20, TRIPLE), t(19, TRIPLE), t(2, DOUBLE)),
                ),
            )
            engine.toGameSession().currentLegIndex shouldBe 1

            // WHEN Undo is checked on the new leg's first turn
            // THEN it is disabled and does nothing
            TurnRewind.canUndo(engine) shouldBe false
            TurnRewind.undo(engine).shouldBeNull()

            // WHEN the leg-won dialog's Undo is used instead
            val rewound = TurnRewind.rewindTurn(engine.toGameSession(), crossLegBoundary = true).shouldNotBeNull()

            // THEN the leg is unwon, the empty leg dropped, and the winning dart is in place
            val session = rewound.engine.toGameSession()
            session.legs.size shouldBe 1
            session.currentLegIndex shouldBe 0
            session.currentLeg.winnerId.shouldBeNull()
            rewound.engine.getLegsWon(p1) shouldBe 0
            rewound.engine.getCurrentTurnThrows().last() shouldBe t(2, DOUBLE)
            rewound.lastThrowResult.shouldBeInstanceOf<ThrowResult.Checkout>()
        }

        "Should not undo into the finished leg once a dart is thrown in the new leg" {
            // GIVEN P1 won leg 1 and P1 ended the first turn of leg 2
            val engine = GameEngine.fromSession(session(classicConfig)).playTurns(
                listOf(
                    listOf(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE)),
                    listOf(t(1)),
                    listOf(t(20, TRIPLE), t(19, TRIPLE), t(2, DOUBLE)),
                    listOf(t(20)),
                ),
            )

            // WHEN Undo is pressed twice
            val first = TurnRewind.undo(engine).shouldNotBeNull().engine
            val second = TurnRewind.undo(first).shouldNotBeNull().engine

            // THEN leg 2 is back to empty and Undo stops there
            second.toGameSession().currentLegIndex shouldBe 1
            second.toGameSession().currentLeg.turns.shouldBeEmpty()
            TurnRewind.undo(second).shouldBeNull()
        }

        "Should revert a match win to in progress" {
            // GIVEN a completed Classic match
            val engine = GameEngine.fromSession(session(classicConfig.copy(legsToWin = 1))).playTurns(
                listOf(
                    listOf(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE)),
                    listOf(t(1)),
                    listOf(t(20, TRIPLE), t(19, TRIPLE), t(2, DOUBLE)),
                ),
            )
            engine.status shouldBe GameStatus.COMPLETED

            // WHEN the game-over dialog's Undo is used
            val rewound = TurnRewind.rewindTurn(engine.toGameSession(), crossLegBoundary = true).shouldNotBeNull()

            // THEN the match is in progress again, with no winner or finish time
            val session = TurnRewind.turnStartSession(rewound.engine.toGameSession())
            session.status shouldBe GameStatus.IN_PROGRESS
            session.finishedAt.shouldBeNull()
            session.winnerId.shouldBeNull()
            session.currentLeg.winnerId.shouldBeNull()
            // AND the saved turn-start state no longer holds the winning turn
            session.currentLeg.playerTurns.size shouldBe 2
        }

        "Should never revive an abandoned game" {
            // GIVEN an abandoned game with turns
            val engine = GameEngine.fromSession(session(classicConfig)).playTurns(listOf(listOf(t(20))))
            val abandoned = engine.toGameSession().copy(status = GameStatus.ABANDONED)

            // WHEN a rewind is attempted
            // THEN nothing happens
            TurnRewind.rewindTurn(abandoned, crossLegBoundary = true).shouldBeNull()
        }
    }

    "Parcheesi knockouts" - {
        "Should restore the victim's score when the knocking dart is undone" {
            // GIVEN P1 on 20 and P2 knocking P1 back to 0 with a S20
            val start = GameEngine.fromSession(session(parcheesiConfig)).playTurns(listOf(listOf(t(20))))
            val (knocked, result) = start.addThrow(20, SINGLE)
            result.shouldBeInstanceOf<ThrowResult.SuccessWithKnockout>()
            knocked.getPlayerScore(p1) shouldBe 0

            // WHEN that dart is undone
            val undone = knocked.undoLastThrow().shouldNotBeNull()

            // THEN P1 is back on 20 and the phantom knockout turn is gone
            undone.getPlayerScore(p1) shouldBe 20
            undone.toGameSession().currentLeg.turns.none { it.isPhantom } shouldBe true
            undone.toGameSession() shouldBe start.toGameSession()
        }

        "Should keep a knockout caused by an earlier dart when a later dart is undone" {
            // GIVEN P2 knocks P1 with the first dart and throws another
            val start = GameEngine.fromSession(session(parcheesiConfig)).playTurns(listOf(listOf(t(20))))
            val engine = start.play(listOf(t(20), t(5)))

            // WHEN the last dart is undone
            val undone = engine.undoLastThrow().shouldNotBeNull()

            // THEN the knockout stays
            undone.getPlayerScore(p1) shouldBe 0
            undone.getCurrentPlayerScore() shouldBe 20
            undone.toGameSession().currentLeg.turns.count { it.isPhantom } shouldBe 1
        }

        "Should drop the knockout from the session start of a turn" {
            // GIVEN P2 knocked P1 mid-turn
            val start = GameEngine.fromSession(session(parcheesiConfig)).playTurns(listOf(listOf(t(20))))
            val engine = start.play(listOf(t(20)))

            // WHEN the turn-start session is taken
            // THEN it has no phantom: that is what gets saved
            TurnRewind.turnStartSession(engine.toGameSession()) shouldBe start.toGameSession()
        }

        "Should restore the victim when undoing back into the knocking turn and removing its dart" {
            // GIVEN P2 knocked P1 and ended the turn
            val engine = GameEngine.fromSession(session(parcheesiConfig))
                .playTurns(listOf(listOf(t(20)), listOf(t(20))))
            engine.getPlayerScore(p1) shouldBe 0

            // WHEN Undo reopens P2's turn
            val reopened = TurnRewind.undo(engine).shouldNotBeNull().engine

            // THEN the knockout is still shown (the dart is still there)
            reopened.getPlayerScore(p1) shouldBe 0
            // AND the turn-start session to save has no phantom
            TurnRewind.turnStartSession(reopened.toGameSession()).currentLeg.turns.none { it.isPhantom } shouldBe true

            // WHEN Undo removes the knocking dart
            val undone = TurnRewind.undo(reopened).shouldNotBeNull().engine

            // THEN P1 is back on 20
            undone.getPlayerScore(p1) shouldBe 20
            undone.toGameSession().currentLeg.turns.none { it.isPhantom } shouldBe true
        }
    }

    "Replaying" - {
        "Should reopen a bust turn with its darts and the score from before the bust" {
            // GIVEN P1 on 121 busting with T20 T19 S4 (0 without a double)
            val engine = GameEngine.fromSession(session(classicConfig)).playTurns(
                listOf(
                    listOf(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE)),
                    listOf(t(1)),
                    listOf(t(20, TRIPLE), t(19, TRIPLE), t(4)),
                ),
            )

            // WHEN Undo reopens the bust turn
            val reopened = TurnRewind.undo(engine).shouldNotBeNull()

            // THEN its darts are back, it is still a bust, and P1's score is the one before the bust
            reopened.engine.getCurrentPlayerId() shouldBe p1
            reopened.engine.getCurrentTurnThrows() shouldBe listOf(t(20, TRIPLE), t(19, TRIPLE), t(4))
            reopened.lastThrowResult.shouldBeInstanceOf<ThrowResult.Bust>()
            reopened.engine.isTurnEnded() shouldBe true
            reopened.engine.getCurrentPlayerScore() shouldBe 121

            // WHEN Undo removes the busting dart
            val undone = TurnRewind.undo(reopened.engine).shouldNotBeNull()

            // THEN the turn is open again on 4
            undone.lastThrowResult.shouldBeInstanceOf<ThrowResult.Success>()
            undone.engine.isTurnEnded() shouldBe false
            undone.engine.getCurrentPlayerScore() shouldBe 4
        }

        "Should refuse to rewind a leg that doesn't replay to itself" {
            // GIVEN a stored leg whose turn records a score the engine wouldn't produce
            val played = GameEngine.fromSession(session(classicConfig))
                .playTurns(listOf(listOf(t(20)), listOf(t(1)), listOf(t(5))))
            val stored = played.toGameSession()
            val corruptedTurns = stored.currentLeg.turns.mapIndexed { index, turn ->
                if (index == 0) turn.copy(scoreAfterTurn = 100) else turn
            }
            val corrupted = stored.copy(legs = listOf(stored.currentLeg.copy(turns = corruptedTurns)))

            // WHEN a rewind is attempted
            // THEN it refuses rather than rewrite history
            TurnRewind.rewindTurn(corrupted, crossLegBoundary = false).shouldBeNull()
            // AND Undo is reported as unavailable, so the button isn't enabled to do nothing
            TurnRewind.canUndo(GameEngine.fromSession(corrupted)) shouldBe false

            // WHEN the same leg is the winning leg of a completed match
            val completed = corrupted.copy(
                status = GameStatus.COMPLETED,
                winnerId = p1,
                legs = listOf(corrupted.currentLeg.copy(winnerId = p1)),
            )

            // THEN the dialog Undo is unavailable too
            TurnRewind.canUndoWin(GameEngine.fromSession(completed)) shouldBe false
        }
    }

    "Parcheesi double-out and bounces" - {
        val doubleOutConfig = parcheesiConfig.copy(doubleOut = true)

        // P1 to 281, P2 missing in between; P1 to throw
        val to281 = listOf(
            listOf(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE)),
            listOf(t(0)),
            listOf(t(20, TRIPLE), t(20, DOUBLE), t(1)),
            listOf(t(0)),
        )

        "Should undo a dart that didn't count, back to the same score" {
            // GIVEN P1 on 281 under double-out has thrown S20 (doesn't count) and S1 (282)
            val engine = GameEngine.fromSession(session(doubleOutConfig)).playTurns(to281)
                .play(listOf(t(20), t(1)))
            engine.getCurrentPlayerScore() shouldBe 282

            // WHEN Undo removes S1, then the non-counting S20
            val first = TurnRewind.undo(engine).shouldNotBeNull()
            val second = TurnRewind.undo(first.engine).shouldNotBeNull()

            // THEN the score is 281 after each, and the turn stays open
            first.engine.getCurrentPlayerScore() shouldBe 281
            first.lastThrowResult shouldBe ThrowResult.Success(281)
            first.engine.isTurnEnded() shouldBe false
            second.engine.getCurrentTurnThrows().shouldBeEmpty()
            second.engine.getCurrentPlayerScore() shouldBe 281
        }

        "Should reopen a turn with a non-counting dart with its darts and score" {
            // GIVEN P1 on 281 ended a turn S20 (doesn't count), S1, miss, and P2 is to throw
            val engine = GameEngine.fromSession(
                session(doubleOutConfig),
            ).playTurns(to281 + listOf(listOf(t(20), t(1), t(0))))

            // WHEN Undo reopens that turn
            val reopened = TurnRewind.undo(engine).shouldNotBeNull()

            // THEN its three darts are back on 282 and the leg replays to itself
            reopened.engine.getCurrentPlayerId() shouldBe p1
            reopened.engine.getCurrentTurnThrows() shouldBe listOf(t(20), t(1), t(0))
            reopened.engine.getCurrentPlayerScore() shouldBe 282
            reopened.engine.getPlayerScore(p1) shouldBe 281
        }

        "Should reopen a bounce turn still bounced" {
            // GIVEN P1 on 300 bounced with S20 (to 282)
            val engine = GameEngine.fromSession(session(parcheesiConfig)).playTurns(
                listOf(
                    listOf(t(20, TRIPLE), t(20, TRIPLE), t(20, TRIPLE)),
                    listOf(t(0)),
                    listOf(t(20, TRIPLE), t(20, TRIPLE)),
                    listOf(t(0)),
                    listOf(t(20)),
                ),
            )

            // WHEN Undo reopens the bounce turn
            val reopened = TurnRewind.undo(engine).shouldNotBeNull()

            // THEN it is a bounce again: no more darts, on the bounced score
            reopened.lastThrowResult shouldBe ThrowResult.BounceBack(282, 19)
            reopened.engine.isTurnEnded() shouldBe true
            reopened.engine.getCurrentPlayerScore() shouldBe 282

            // WHEN Undo removes the bouncing dart
            val undone = TurnRewind.undo(reopened.engine).shouldNotBeNull()

            // THEN P1 is back on 300 with the turn open
            undone.engine.isTurnEnded() shouldBe false
            undone.engine.getCurrentPlayerScore() shouldBe 300
        }

        "Should load an old save's exact-without-double bounce, and reopen it under the current rule" {
            // GIVEN a leg saved by the old engine: P1 on 281 threw S20 and it was stored as a bounce on 301
            val played = GameEngine.fromSession(session(doubleOutConfig)).playTurns(to281)
            val stored = played.toGameSession()
            val oldTurn = Turn(
                playerId = p1,
                throws = listOf(t(20)),
                scoreBeforeTurn = 281,
                scoreAfterTurn = 301,
                isBounce = true,
            )
            val oldSave = stored.copy(legs = listOf(stored.currentLeg.copy(turns = stored.currentLeg.turns + oldTurn)))

            // WHEN it is loaded
            val loaded = GameEngine.fromSession(oldSave)

            // THEN it loads as stored, with P2 to throw
            loaded.getCurrentPlayerId() shouldBe p2
            loaded.getPlayerScore(p1) shouldBe 301
            // WHEN Undo reopens that turn (only the turns before it have to replay to themselves)
            val reopened = TurnRewind.undo(loaded).shouldNotBeNull()

            // THEN its dart is replayed under the current rule: it doesn't count, P1 is on 281 with two darts left
            reopened.engine.getCurrentTurnThrows() shouldBe listOf(t(20))
            reopened.engine.getCurrentPlayerScore() shouldBe 281
            reopened.engine.isTurnEnded() shouldBe false
            // AND its points leave out the dart that wouldn't count under the current rules
            ParcheesiRules.turnPoints(oldTurn, doubleOutConfig) shouldBe 0
        }
    }

    "Checkout Practice reopen" - {
        val practiceConfig = GameConfig(
            gameType = GameType.CHECKOUT_EASY,
            gameMode = GameMode.CHECKOUT_PRACTICE,
            doubleOut = true,
            playerIds = listOf(p1),
            checkoutPracticeTargets = listOf(40, 32, 5),
        )

        "Should reopen a successful round live, without its checkout dart" {
            // GIVEN round 1 (40) checked out with S20 D10, as live play does (the checkout ends the round)
            val (afterS20, s20) = GameEngine.fromSession(session(practiceConfig)).playWithResult(listOf(t(20)))
            val (live, _) = afterS20.play(listOf(t(10, DOUBLE))).endTurn()
            live.getCheckoutPracticeState().shouldNotBeNull().currentTarget shouldBe 32

            // WHEN one Undo goes back into that round
            val reopened = TurnRewind.undo(live).shouldNotBeNull()

            // THEN round 1 is live again with S20 on 20 remaining, exactly as before the checkout dart
            val state = reopened.engine.getCheckoutPracticeState().shouldNotBeNull()
            state.currentRoundIndex shouldBe 0
            state.currentTarget shouldBe 40
            state.successCount shouldBe 0
            reopened.engine.getCurrentTurnThrows() shouldBe listOf(t(20))
            reopened.engine.getCurrentPlayerScore() shouldBe 20
            reopened.engine.isTurnEnded() shouldBe false
            reopened.lastThrowResult shouldBe s20
            assertSameState(reopened.engine, afterS20)
        }

        "Should reopen a one-dart checkout round with no darts and all three to throw" {
            // GIVEN round 1 (40) checked out with D20
            val live = GameEngine.fromSession(session(practiceConfig)).playTurns(listOf(listOf(t(20, DOUBLE))))

            // WHEN one Undo goes back into that round
            val reopened = TurnRewind.undo(live).shouldNotBeNull()

            // THEN round 1 is open on 40 with no darts
            reopened.engine.getCheckoutPracticeState().shouldNotBeNull().currentTarget shouldBe 40
            reopened.engine.getCurrentTurnThrows().shouldBeEmpty()
            reopened.engine.getCurrentPlayerScore() shouldBe 40
            reopened.lastThrowResult.shouldBeNull()
        }

        "Should reopen a failed round as before, still open for its End Turn" {
            // GIVEN round 1 (40) missed with three darts and ended
            val live = GameEngine.fromSession(session(practiceConfig)).playTurns(listOf(listOf(t(1), t(1), t(1))))

            // WHEN Undo goes back into that round
            val reopened = TurnRewind.undo(live).shouldNotBeNull()

            // THEN its darts are back on 37 remaining
            reopened.engine.getCurrentTurnThrows() shouldBe listOf(t(1), t(1), t(1))
            reopened.engine.getCurrentPlayerScore() shouldBe 37
            reopened.lastThrowResult shouldBe ThrowResult.Success(37)
        }
    }
})
