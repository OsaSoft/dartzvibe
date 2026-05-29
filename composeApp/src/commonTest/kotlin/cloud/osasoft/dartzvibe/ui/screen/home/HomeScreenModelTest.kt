package cloud.osasoft.dartzvibe.ui.screen.home

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Leg
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.FakeGameRepository
import cloud.osasoft.dartzvibe.data.repository.FakePlayerRepository
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

/**
 * Tests for HomeScreenModel — player/game count thresholds and in-progress game state.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class HomeScreenModelTest : FreeSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    fun makePlayer(name: String): Player = Player(
        id = Uuid.random(),
        name = name,
        avatarColor = 0,
        createdAt = currentTimeMillis(),
    )

    fun makeCompletedSession(playerIds: List<Uuid>): GameSession = GameSession(
        id = Uuid.random(),
        config = GameConfig(
            gameType = GameType.CLASSIC_501,
            playerIds = playerIds,
        ),
        legs = listOf(Leg()),
        currentLegIndex = 0,
        status = GameStatus.COMPLETED,
        startedAt = currentTimeMillis(),
        finishedAt = currentTimeMillis(),
        winnerId = playerIds.firstOrNull(),
    )

    fun makeInProgressSession(playerIds: List<Uuid>): GameSession = GameSession(
        id = Uuid.random(),
        config = GameConfig(
            gameType = GameType.CLASSIC_501,
            playerIds = playerIds,
        ),
        legs = listOf(Leg()),
        currentLegIndex = 0,
        status = GameStatus.IN_PROGRESS,
        startedAt = currentTimeMillis(),
        finishedAt = null,
        winnerId = null,
    )

    "Player count thresholds" - {
        "Should report playerCount 0 with no players" {
            runTest {
                // GIVEN empty repositories
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                val screenModel = HomeScreenModel(playerRepo, gameRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN playerCount is 0 (New Game should be disabled)
                state.playerCount shouldBe 0
            }
        }

        "Should report playerCount 1 after one player is added" {
            runTest {
                // GIVEN a repository with one player
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                val screenModel = HomeScreenModel(playerRepo, gameRepo)

                // WHEN one player is inserted
                playerRepo.insertPlayer(makePlayer("Alice"))
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN playerCount is 1 (New Game should be enabled, Head-to-Head still disabled)
                state.playerCount shouldBe 1
            }
        }

        "Should update playerCount reactively when player is deleted" {
            runTest {
                // GIVEN a repository with one player
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                val alice = makePlayer("Alice")
                playerRepo.insertPlayer(alice)
                val screenModel = HomeScreenModel(playerRepo, gameRepo)
                advanceUntilIdle()

                // WHEN that player is deleted
                playerRepo.deletePlayer(alice.id)
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN playerCount drops back to 0 (New Game re-disabled)
                state.playerCount shouldBe 0
            }
        }
    }

    "Head-to-Head threshold" - {
        "Should require exactly 2 players for Head-to-Head to be enabled" {
            runTest {
                // GIVEN one player exists
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                val alice = makePlayer("Alice")
                playerRepo.insertPlayer(alice)
                val screenModel = HomeScreenModel(playerRepo, gameRepo)
                advanceUntilIdle()
                val stateOne = screenModel.state.first()

                // THEN playerCount < 2 (Head-to-Head disabled)
                (stateOne.playerCount >= 2) shouldBe false

                // WHEN a second player is added
                playerRepo.insertPlayer(makePlayer("Bob"))
                advanceUntilIdle()
                val stateTwo = screenModel.state.first()

                // THEN playerCount >= 2 (Head-to-Head enabled)
                (stateTwo.playerCount >= 2) shouldBe true
            }
        }

        "Should not enable Head-to-Head with exactly 1 player" {
            runTest {
                // GIVEN exactly one player
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                playerRepo.insertPlayer(makePlayer("Alice"))
                val screenModel = HomeScreenModel(playerRepo, gameRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN playerCount is below the Head-to-Head threshold of 2
                state.playerCount shouldBe 1
                (state.playerCount >= 2) shouldBe false
            }
        }
    }

    "In-progress game state" - {
        "Should expose inProgressGame as null when no game is in progress" {
            runTest {
                // GIVEN no games exist
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                val screenModel = HomeScreenModel(playerRepo, gameRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN inProgressGame is null (Resume button hidden)
                state.inProgressGame shouldBe null
            }
        }

        "Should expose inProgressGame when a session is in progress" {
            runTest {
                // GIVEN one player and one in-progress session
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                val alice = makePlayer("Alice")
                playerRepo.insertPlayer(alice)
                val session = makeInProgressSession(listOf(alice.id))
                gameRepo.insertSession(session)
                val screenModel = HomeScreenModel(playerRepo, gameRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN inProgressGame is populated (Resume button shown)
                state.inProgressGame shouldNotBe null
                state.inProgressGame?.id shouldBe session.id
            }
        }

        "Should populate inProgressGamePlayers with player names for the session" {
            runTest {
                // GIVEN two players and an in-progress session with both
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                val alice = makePlayer("Alice")
                val bob = makePlayer("Bob")
                playerRepo.insertPlayer(alice)
                playerRepo.insertPlayer(bob)
                val session = makeInProgressSession(listOf(alice.id, bob.id))
                gameRepo.insertSession(session)
                val screenModel = HomeScreenModel(playerRepo, gameRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN inProgressGamePlayers contains both players (Resume subtitle shows "Alice vs Bob")
                state.inProgressGamePlayers.map { it.name }.toSet() shouldBe setOf("Alice", "Bob")
            }
        }

        "Should not count in-progress session in completedGameCount" {
            runTest {
                // GIVEN one in-progress session and one completed session
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                val alice = makePlayer("Alice")
                playerRepo.insertPlayer(alice)
                gameRepo.insertSession(makeInProgressSession(listOf(alice.id)))
                gameRepo.insertSession(makeCompletedSession(listOf(alice.id)))
                val screenModel = HomeScreenModel(playerRepo, gameRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN only the completed session is counted (Statistics/Leaderboard threshold)
                state.completedGameCount shouldBe 1
            }
        }
    }

    "New Game routing condition" - {
        "Should have playerCount < 1 when no players exist (route to PlayerListScreen)" {
            runTest {
                // GIVEN empty player repository
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                val screenModel = HomeScreenModel(playerRepo, gameRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN the routing condition (playerCount >= 1) is false → navigate to PlayerListScreen
                (state.playerCount >= 1) shouldBe false
            }
        }

        "Should have playerCount >= 1 when at least one player exists (route to NewGameScreen)" {
            runTest {
                // GIVEN one player
                val playerRepo = FakePlayerRepository()
                val gameRepo = FakeGameRepository()
                playerRepo.insertPlayer(makePlayer("Alice"))
                val screenModel = HomeScreenModel(playerRepo, gameRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN the routing condition (playerCount >= 1) is true → navigate to NewGameScreen
                (state.playerCount >= 1) shouldBe true
            }
        }
    }
})
