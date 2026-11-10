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
})
