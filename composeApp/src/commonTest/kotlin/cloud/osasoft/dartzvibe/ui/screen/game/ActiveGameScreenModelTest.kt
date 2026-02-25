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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
