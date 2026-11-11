package cloud.osasoft.dartzvibe.ui.screen.game

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.FakeGameRepository
import cloud.osasoft.dartzvibe.data.repository.FakePlayerRepository
import cloud.osasoft.dartzvibe.domain.game.ThrowResult
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class ActiveGameScreenModelTest : FreeSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    val rouletteSessionId = Uuid.parse("00000000-0000-0000-0000-000000000098")
    val roulettePlayer1Id = Uuid.parse("00000000-0000-0000-0000-000000000011")
    val roulettePlayer2Id = Uuid.parse("00000000-0000-0000-0000-000000000012")

    // Builds a loaded ScreenModel for a two-player Roulette game whose first round targets [targets].first()
    suspend fun TestScope.rouletteScreenModel(
        targets: List<Int>?,
        gameType: GameType = GameType.ROULETTE_ROUNDS,
    ): ActiveGameScreenModel {
        val playerRepository = FakePlayerRepository()
        playerRepository.insertPlayer(Player(id = roulettePlayer1Id, name = "Player 1", createdAt = 0L))
        playerRepository.insertPlayer(Player(id = roulettePlayer2Id, name = "Player 2", createdAt = 0L))

        val session = GameSession(
            id = rouletteSessionId,
            config = GameConfig(
                gameType = gameType,
                gameMode = GameMode.ROULETTE,
                doubleIn = false,
                doubleOut = false,
                playerIds = listOf(roulettePlayer1Id, roulettePlayer2Id),
                legsToWin = 1,
                rouletteTargetSegments = targets,
                rouletteRounds = 5,
                rouletteTargetScore = 20,
            ),
            legs = listOf(Leg()),
            currentLegIndex = 0,
            status = GameStatus.IN_PROGRESS,
            startedAt = 0L,
        )
        val gameRepository = FakeGameRepository()
        gameRepository.insertSession(session)

        val screenModel = ActiveGameScreenModel(
            gameRepository = gameRepository,
            playerRepository = playerRepository,
            sessionId = session.id,
        )
        advanceUntilIdle()
        return screenModel
    }

    "Roulette quick input" - {
        "Should score the multiplier value for a Double on the target" - {
            withData(GameType.ROULETTE_ROUNDS, GameType.ROULETTE_SCORE) { gameType ->
                runTest {
                    // GIVEN a Roulette game on target 7
                    val screenModel = rouletteScreenModel(targets = listOf(7, 12), gameType = gameType)

                    // WHEN the player taps Double (D7)
                    screenModel.onScoreSelect(7, Multiplier.DOUBLE)
                    advanceUntilIdle()
                    val state = screenModel.state.first()

                    // THEN the dart scores 2 points
                    val result = state.lastThrowResult.shouldBeInstanceOf<ThrowResult.RouletteHit>()
                    result.pointsScored shouldBe 2
                    // AND the turn total and the score show it
                    state.rouletteTurnPoints shouldBe 2
                    state.currentPlayerScore shouldBe 2
                }
            }
        }

        "Should ignore a triple bull" {
            runTest {
                // GIVEN a Roulette game on the bull
                val screenModel = rouletteScreenModel(targets = listOf(25))

                // WHEN a triple bull is submitted
                screenModel.onScoreSelect(25, Multiplier.TRIPLE)
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN no dart is recorded
                state.currentTurnThrows shouldBe emptyList()
                state.lastThrowResult shouldBe null
                state.rouletteTurnPoints shouldBe 0
            }
        }

        "Should record a miss with 0 points" {
            runTest {
                // GIVEN a Roulette game on target 7
                val screenModel = rouletteScreenModel(targets = listOf(7))

                // WHEN the player taps Miss
                screenModel.onMiss()
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN a miss is recorded
                state.currentTurnThrows.single().segment shouldBe 0
                // AND it scores nothing
                val result = state.lastThrowResult.shouldBeInstanceOf<ThrowResult.RouletteHit>()
                result.pointsScored shouldBe 0
                state.rouletteTurnPoints shouldBe 0
            }
        }

        "Should show the whole turn's points, not just the last dart's" {
            runTest {
                // GIVEN a Roulette game on target 7
                val screenModel = rouletteScreenModel(targets = listOf(7))

                // WHEN the player throws T7, a miss and S7
                screenModel.onScoreSelect(7, Multiplier.TRIPLE)
                screenModel.onMiss()
                screenModel.onScoreSelect(7, Multiplier.SINGLE)
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN the last dart alone scored 1
                (state.lastThrowResult as ThrowResult.RouletteHit).pointsScored shouldBe 1
                // AND the turn total is 4, matching the score
                state.rouletteTurnPoints shouldBe 4
                state.currentPlayerScore shouldBe 4
                // AND the pad is replaced by End Turn
                state.canThrow shouldBe false
            }
        }

        "Should fall back to target 1 when the target list is missing or empty" - {
            withData<List<Int>?>(nameFn = { "targets = $it" }, null, emptyList()) { targets ->
                runTest {
                    // GIVEN a Roulette game without stored targets
                    val screenModel = rouletteScreenModel(targets = targets)

                    // WHEN the state is read
                    val state = screenModel.state.first()

                    // THEN the target falls back to 1, like RouletteModeEngine, so the input pad still renders
                    state.currentRouletteTarget shouldBe 1
                }
            }
        }
    }

    "Parcheesi BounceBack" - {
        "Should not auto-advance player after BounceBack in Parcheesi mode" {
            runTest {
                // GIVEN two players
                val player1Id = Uuid.parse("00000000-0000-0000-0000-000000000001")
                val player2Id = Uuid.parse("00000000-0000-0000-0000-000000000002")
                val player1 = Player(id = player1Id, name = "Player 1", createdAt = 0L)
                val player2 = Player(id = player2Id, name = "Player 2", createdAt = 0L)

                val playerRepository = FakePlayerRepository()
                playerRepository.insertPlayer(player1)
                playerRepository.insertPlayer(player2)

                // AND a Parcheesi 301 game session
                val config = GameConfig(
                    gameType = GameType.CLASSIC_301,
                    gameMode = GameMode.PARCHEESI,
                    doubleIn = false,
                    doubleOut = false,
                    playerIds = listOf(player1Id, player2Id),
                    legsToWin = 1,
                )
                val session = GameSession(
                    id = Uuid.parse("00000000-0000-0000-0000-000000000099"),
                    config = config,
                    legs = listOf(Leg()),
                    currentLegIndex = 0,
                    status = GameStatus.IN_PROGRESS,
                    startedAt = 0L,
                )

                val gameRepository = FakeGameRepository()
                gameRepository.insertSession(session)

                val screenModel = ActiveGameScreenModel(
                    gameRepository = gameRepository,
                    playerRepository = playerRepository,
                    sessionId = session.id,
                )
                advanceUntilIdle()

                // WHEN player1 builds up score close to 301 via multiple turns
                // Each turn: throw T20 (60 points), then end turn
                // After 5 full rounds (player1 throws 5 × T20 = 300 total)
                (1..5).forEach { _ ->
                    // Player 1 throws T20
                    screenModel.onScoreSelect(20, Multiplier.TRIPLE)
                    screenModel.endTurn()
                    advanceUntilIdle()

                    // Player 2 throws a miss to cycle back
                    screenModel.onMiss()
                    screenModel.endTurn()
                    advanceUntilIdle()
                }

                // THEN player1 should be current player with score 300
                val preState = screenModel.state.first()
                preState.currentPlayerId shouldBe player1Id
                preState.currentPlayerScore shouldBe 300

                // WHEN player1 throws S20 (20 points → total 320 > 301 target) → BounceBack
                screenModel.onScoreSelect(20, Multiplier.SINGLE)
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN lastThrowResult should be BounceBack (not cleared by auto-endTurn)
                state.lastThrowResult.shouldBeInstanceOf<ThrowResult.BounceBack>()

                // AND isBounced should be true
                state.isBounced shouldBe true

                // AND current player should still be player1 (turn not auto-ended)
                state.currentPlayerId shouldBe player1Id
            }
        }
    }

    val undoSessionId = Uuid.parse("00000000-0000-0000-0000-000000000097")
    val undoPlayer1Id = Uuid.parse("00000000-0000-0000-0000-000000000021")
    val undoPlayer2Id = Uuid.parse("00000000-0000-0000-0000-000000000022")

    // Builds a loaded ScreenModel for [config], or for a stored [session] when given, plus its repository
    suspend fun TestScope.configScreenModel(
        config: GameConfig,
        session: GameSession = GameSession(id = undoSessionId, config = config, startedAt = 0L),
    ): Pair<ActiveGameScreenModel, FakeGameRepository> {
        val playerRepository = FakePlayerRepository()
        playerRepository.insertPlayer(Player(id = undoPlayer1Id, name = "Player 1", createdAt = 0L))
        playerRepository.insertPlayer(Player(id = undoPlayer2Id, name = "Player 2", createdAt = 0L))
        val gameRepository = FakeGameRepository()
        gameRepository.insertSession(session)
        val screenModel = ActiveGameScreenModel(
            gameRepository = gameRepository,
            playerRepository = playerRepository,
            sessionId = session.id,
        )
        advanceUntilIdle()
        return screenModel to gameRepository
    }

    // Builds a loaded ScreenModel for a two-player game in [mode] (301, no double-out) plus its repository
    suspend fun TestScope.undoScreenModel(
        mode: GameMode,
        legsToWin: Int = 1,
    ): Pair<ActiveGameScreenModel, FakeGameRepository> = configScreenModel(
        GameConfig(
            gameType = if (mode == GameMode.CRICKET) GameType.CRICKET_REGULAR else GameType.CLASSIC_301,
            gameMode = mode,
            doubleIn = false,
            doubleOut = false,
            playerIds = listOf(undoPlayer1Id, undoPlayer2Id),
            legsToWin = legsToWin,
        ),
    )

    suspend fun FakeGameRepository.saved(): GameSession = getGameSessionById(undoSessionId).first()!!

    // Classic 301: P1 T20 x3 (121 left), P2 a miss, P1 T20 T20 (1 left), P2 a miss; P1 to throw
    fun ActiveGameScreenModel.playClassicToOneLeft() {
        repeat(3) { onScoreSelect(20, Multiplier.TRIPLE) }
        endTurn()
        onMiss()
        endTurn()
        onScoreSelect(20, Multiplier.TRIPLE)
        onScoreSelect(20, Multiplier.TRIPLE)
        endTurn()
        onMiss()
        endTurn()
    }

    "Undo" - {
        "Should rewind only once when the dialog Undo is tapped twice" {
            runTest {
                // GIVEN Player 1 just won a Classic match
                val (screenModel, repository) = undoScreenModel(GameMode.CLASSIC)
                screenModel.playClassicToOneLeft()
                screenModel.onScoreSelect(1, Multiplier.SINGLE)
                advanceUntilIdle()

                // WHEN the dialog's Undo gets two taps before the dialog closes
                screenModel.undoWin()
                screenModel.undoWin()
                advanceUntilIdle()

                // THEN the winning turn is open with its winning dart
                val state = screenModel.state.value
                state.currentPlayerId shouldBe undoPlayer1Id
                state.currentTurnThrows.map { it.segment } shouldBe listOf(1)
                state.lastThrowResult.shouldBeInstanceOf<ThrowResult.Checkout>()
                // AND the saved game still holds the four turns before it
                repository.saved().currentLeg.playerTurns.size shouldBe 4
            }
        }

        "Should block more darts in a reopened winning turn and win again on End Turn" {
            runTest {
                // GIVEN Player 1 won leg 1 of a best-of-2-legs game and the Leg Won Undo was used
                val (screenModel, _) = undoScreenModel(GameMode.CLASSIC, legsToWin = 2)
                screenModel.playClassicToOneLeft()
                screenModel.onScoreSelect(1, Multiplier.SINGLE)
                advanceUntilIdle()
                screenModel.undoWin()

                // THEN no more darts can be thrown
                screenModel.state.value.canThrow shouldBe false

                // WHEN a dart is submitted anyway
                screenModel.onScoreSelect(20, Multiplier.SINGLE)
                // THEN it is not recorded
                screenModel.state.value.currentTurnThrows.size shouldBe 1

                // WHEN End Turn is pressed
                screenModel.endTurn()
                advanceUntilIdle()

                // THEN the Leg Won dialog shows again
                screenModel.state.value.showLegWonDialog shouldBe true
                screenModel.state.value.getLegsWon(undoPlayer1Id) shouldBe 1
            }
        }

        "Should close the results dialog from its Undo" - {
            withData(
                nameFn = { it.first },
                "Roulette Complete" to GameConfig(
                    gameType = GameType.ROULETTE_SCORE,
                    gameMode = GameMode.ROULETTE,
                    doubleOut = false,
                    playerIds = listOf(undoPlayer1Id, undoPlayer2Id),
                    rouletteTargetSegments = listOf(7),
                    rouletteTargetScore = 1,
                ),
                "Practice Complete" to GameConfig(
                    gameType = GameType.CHECKOUT_EASY,
                    gameMode = GameMode.CHECKOUT_PRACTICE,
                    doubleOut = true,
                    playerIds = listOf(undoPlayer1Id),
                    checkoutPracticeTargets = listOf(2),
                ),
            ) { (_, config) ->
                runTest {
                    // GIVEN a game finished in one round
                    val (screenModel, repository) = configScreenModel(config)
                    if (config.isRoulette) {
                        screenModel.onScoreSelect(7, Multiplier.SINGLE)
                        screenModel.endTurn()
                        screenModel.onMiss()
                        screenModel.endTurn()
                    } else {
                        screenModel.onScoreSelect(1, Multiplier.DOUBLE)
                    }
                    advanceUntilIdle()
                    screenModel.state.value.showGameCompleteDialog shouldBe true
                    screenModel.state.value.canUndoWin shouldBe true

                    // WHEN the dialog's Undo is pressed
                    screenModel.undoWin()
                    advanceUntilIdle()

                    // THEN the dialog closes and the game is in progress again
                    screenModel.state.value.showGameCompleteDialog shouldBe false
                    screenModel.state.value.isGameComplete shouldBe false
                    screenModel.state.value.currentTurnThrows.isNotEmpty() shouldBe true
                    repository.saved().status shouldBe GameStatus.IN_PROGRESS
                }
            }
        }

        "Should revert a Cricket leg win in a best-of-2-legs game" {
            runTest {
                // GIVEN Player 1 closing every number and winning leg 1 of a Cricket game
                val (screenModel, repository) = undoScreenModel(GameMode.CRICKET, legsToWin = 2)
                listOf(20, 19, 18).forEach { screenModel.onScoreSelect(it, Multiplier.TRIPLE) }
                screenModel.endTurn()
                screenModel.onMiss()
                screenModel.endTurn()
                listOf(17, 16, 15).forEach { screenModel.onScoreSelect(it, Multiplier.TRIPLE) }
                screenModel.endTurn()
                screenModel.onMiss()
                screenModel.endTurn()
                repeat(3) { screenModel.onScoreSelect(25, Multiplier.SINGLE) }
                advanceUntilIdle()
                screenModel.state.value.showLegWonDialog shouldBe true

                // WHEN the dialog's Undo is pressed
                screenModel.undoWin()
                advanceUntilIdle()

                // THEN leg 1 is unwon, with the bull darts in place and the marks as before End Turn
                val state = screenModel.state.value
                state.showLegWonDialog shouldBe false
                state.getLegsWon(undoPlayer1Id) shouldBe 0
                state.session?.legs?.size shouldBe 1
                state.lastThrowResult.shouldBeInstanceOf<ThrowResult.CricketWin>()
                state.cricketState?.getPlayerState(undoPlayer1Id)?.getMarks(25) shouldBe 3
                repository.saved().legs.single().winnerId shouldBe null
                repository.saved().currentLeg.cricketState?.getPlayerState(undoPlayer1Id)?.getMarks(25) shouldBe 0

                // WHEN Undo removes the last bull
                screenModel.undo()

                // THEN that number is open again
                screenModel.state.value.cricketState?.getPlayerState(undoPlayer1Id)?.getMarks(25) shouldBe 2
            }
        }

        "Should not save when undoing a dart" {
            runTest {
                // GIVEN a Classic game with a dart thrown
                val (screenModel, repository) = undoScreenModel(GameMode.CLASSIC)
                screenModel.onScoreSelect(20, Multiplier.SINGLE)
                advanceUntilIdle()

                // WHEN the dart is undone
                screenModel.undo()
                advanceUntilIdle()

                // THEN nothing is written: the saved game didn't change
                repository.updateCount shouldBe 0
            }
        }

        "Should save ABANDONED without the knockout of the turn in progress" {
            runTest {
                // GIVEN Player 1 on 20 and Player 2 knocking them out mid-turn
                val (screenModel, repository) = undoScreenModel(GameMode.PARCHEESI)
                screenModel.onScoreSelect(20, Multiplier.SINGLE)
                screenModel.endTurn()
                screenModel.onScoreSelect(20, Multiplier.SINGLE)
                advanceUntilIdle()

                // WHEN the game is abandoned
                screenModel.abandonGame()
                advanceUntilIdle()

                // THEN it is saved ABANDONED, with Player 1 on 20 and no phantom turn
                val saved = repository.saved()
                saved.status shouldBe GameStatus.ABANDONED
                saved.currentLeg.turns.none { it.isPhantom } shouldBe true
                saved.currentLeg.turns.single().scoreAfterTurn shouldBe 20
            }
        }

        "Should disable Undo when the stored leg doesn't replay" {
            runTest {
                // GIVEN a stored game whose first turn records a score the engine wouldn't produce
                val config = GameConfig(
                    gameType = GameType.CLASSIC_301,
                    doubleOut = false,
                    playerIds = listOf(undoPlayer1Id, undoPlayer2Id),
                )
                val (played, _) = configScreenModel(config)
                played.onScoreSelect(20, Multiplier.SINGLE)
                played.endTurn()
                played.onScoreSelect(1, Multiplier.SINGLE)
                played.endTurn()
                advanceUntilIdle()
                val stored = played.state.value.session!!
                val corrupted = stored.copy(
                    legs = listOf(
                        stored.currentLeg.copy(
                            turns = stored.currentLeg.turns.mapIndexed { index, turn ->
                                if (index == 0) turn.copy(scoreAfterTurn = 100) else turn
                            },
                        ),
                    ),
                )
                val (screenModel, repository) = configScreenModel(config, session = corrupted)

                // THEN Undo is disabled
                screenModel.state.value.canUndo shouldBe false

                // WHEN it is pressed anyway
                screenModel.undo()
                advanceUntilIdle()

                // THEN nothing changes
                screenModel.state.value.session shouldBe corrupted
                repository.saved() shouldBe corrupted
            }
        }

        "Should be enabled only with darts thrown or an earlier turn in the leg" {
            runTest {
                // GIVEN a new Classic best-of-2-legs game
                val (screenModel, _) = undoScreenModel(GameMode.CLASSIC, legsToWin = 2)

                // THEN Undo is disabled on the first turn with no darts
                screenModel.state.value.canUndo shouldBe false

                // WHEN a dart is thrown
                screenModel.onScoreSelect(20, Multiplier.SINGLE)
                // THEN it is enabled
                screenModel.state.value.canUndo shouldBe true

                // WHEN the turn is ended
                screenModel.endTurn()
                advanceUntilIdle()
                // THEN it is still enabled, to go back to Player 1
                screenModel.state.value.canUndo shouldBe true

                // WHEN Player 1 wins the leg and the dialog is confirmed
                screenModel.undo()
                screenModel.undo()
                advanceUntilIdle()
                screenModel.state.value.canUndo shouldBe false
                screenModel.playClassicToOneLeft()
                screenModel.onScoreSelect(1, Multiplier.SINGLE)
                advanceUntilIdle()
                screenModel.state.value.showLegWonDialog shouldBe true
                screenModel.dismissLegWonDialog()

                // THEN Undo is disabled on the new leg's first turn
                screenModel.state.value.canUndo shouldBe false
            }
        }

        "Should reopen the previous player's turn and save the state at its start" {
            runTest {
                // GIVEN Player 1 ended a turn of T20 S5, then Player 2 ended a turn of S1
                val (screenModel, repository) = undoScreenModel(GameMode.CLASSIC)
                screenModel.onScoreSelect(20, Multiplier.TRIPLE)
                screenModel.onScoreSelect(5, Multiplier.SINGLE)
                screenModel.endTurn()
                advanceUntilIdle()
                val afterPlayer1 = repository.saved()
                screenModel.onScoreSelect(1, Multiplier.SINGLE)
                screenModel.endTurn()
                advanceUntilIdle()

                // WHEN Undo is pressed twice with no darts thrown
                screenModel.undo()
                advanceUntilIdle()
                // THEN Player 2's turn is open with their dart, saved as it was before it
                screenModel.state.value.currentPlayerId shouldBe undoPlayer2Id
                screenModel.state.value.currentTurnThrows.size shouldBe 1
                repository.saved() shouldBe afterPlayer1

                screenModel.undo()
                screenModel.undo()
                advanceUntilIdle()

                // THEN Player 1's turn is open with both darts and the score from before
                val state = screenModel.state.value
                state.currentPlayerId shouldBe undoPlayer1Id
                state.currentTurnThrows.map { it.segment } shouldBe listOf(20, 5)
                state.currentPlayerScore shouldBe 236
                state.getPlayerScore(undoPlayer1Id) shouldBe 301
                // AND the saved game has no turns, so a reopen starts at Player 1 with no darts
                repository.saved().currentLeg.turns shouldBe emptyList()
            }
        }

        "Should save the Parcheesi turn start without the knockout it caused" {
            runTest {
                // GIVEN Player 1 on 20 and Player 2 knocking them out with S20, then ending the turn
                val (screenModel, repository) = undoScreenModel(GameMode.PARCHEESI)
                screenModel.onScoreSelect(20, Multiplier.SINGLE)
                screenModel.endTurn()
                advanceUntilIdle()
                screenModel.onScoreSelect(20, Multiplier.SINGLE)
                screenModel.dismissKnockoutDialog()
                screenModel.endTurn()
                advanceUntilIdle()
                repository.saved().currentLeg.turns.count { it.isPhantom } shouldBe 1

                // WHEN Undo reopens Player 2's turn
                screenModel.undo()
                advanceUntilIdle()

                // THEN the knockout still shows while the dart is there, without the dialog
                screenModel.state.value.getPlayerScore(undoPlayer1Id) shouldBe 0
                screenModel.state.value.showKnockoutDialog shouldBe false
                // AND the saved game is back to Player 1 on 20 with no phantom
                val saved = repository.saved()
                saved.currentLeg.turns.none { it.isPhantom } shouldBe true
                saved.currentLeg.turns.single().scoreAfterTurn shouldBe 20

                // WHEN the knocking dart is undone too
                screenModel.undo()
                advanceUntilIdle()

                // THEN Player 1 is back on 20
                screenModel.state.value.getPlayerScore(undoPlayer1Id) shouldBe 20
            }
        }

        "Should end in progress in the repository when a win is undone straight away" {
            runTest {
                // GIVEN Player 1 one dart from winning, with the next save held in flight
                val (screenModel, repository) = undoScreenModel(GameMode.CLASSIC)
                screenModel.playClassicToOneLeft()
                advanceUntilIdle()
                val gate = CompletableDeferred<Unit>()
                repository.updateGate = gate

                // WHEN the winning dart lands (auto End Turn saves COMPLETED)
                screenModel.onScoreSelect(1, Multiplier.SINGLE)
                advanceUntilIdle()
                screenModel.state.value.showGameCompleteDialog shouldBe true
                // AND the dialog's Undo is pressed before that save finishes
                screenModel.undoWin()
                gate.complete(Unit)
                advanceUntilIdle()

                // THEN the repository holds the game in progress, without the winning turn
                val saved = repository.saved()
                saved.status shouldBe GameStatus.IN_PROGRESS
                saved.finishedAt shouldBe null
                saved.winnerId shouldBe null
                saved.currentLeg.winnerId shouldBe null
                saved.currentLeg.playerTurns.size shouldBe 4
                // AND the winning turn is open with its last dart, taking no more darts
                val state = screenModel.state.value
                state.showGameCompleteDialog shouldBe false
                state.isGameComplete shouldBe false
                state.currentTurnThrows.last().segment shouldBe 1
                state.canThrow shouldBe false
                state.canUndo shouldBe true

                // WHEN End Turn is pressed again
                screenModel.endTurn()
                advanceUntilIdle()

                // THEN the match is won again
                screenModel.state.value.showGameCompleteDialog shouldBe true
                repository.saved().status shouldBe GameStatus.COMPLETED
            }
        }

        "Should revert a won leg and its leg count from the Leg Won dialog" {
            runTest {
                // GIVEN Player 1 just won leg 1 of a best-of-2-legs game
                val (screenModel, repository) = undoScreenModel(GameMode.CLASSIC, legsToWin = 2)
                screenModel.playClassicToOneLeft()
                screenModel.onScoreSelect(1, Multiplier.SINGLE)
                advanceUntilIdle()
                screenModel.state.value.showLegWonDialog shouldBe true
                screenModel.state.value.getLegsWon(undoPlayer1Id) shouldBe 1

                // WHEN the dialog's Undo is pressed
                screenModel.undoWin()
                advanceUntilIdle()

                // THEN the dialog closes, the leg is unwon and the winning dart is in place
                val state = screenModel.state.value
                state.showLegWonDialog shouldBe false
                state.getLegsWon(undoPlayer1Id) shouldBe 0
                state.session?.legs?.size shouldBe 1
                state.currentTurnThrows.last().segment shouldBe 1
                state.lastThrowResult.shouldBeInstanceOf<ThrowResult.Checkout>()
                repository.saved().legs.size shouldBe 1
                repository.saved().legs.single().winnerId shouldBe null

                // WHEN Undo is pressed once more
                screenModel.undo()

                // THEN the winning dart is removed and darts can be thrown again
                screenModel.state.value.currentTurnThrows shouldBe emptyList()
                screenModel.state.value.canThrow shouldBe true
            }
        }
    }
})
