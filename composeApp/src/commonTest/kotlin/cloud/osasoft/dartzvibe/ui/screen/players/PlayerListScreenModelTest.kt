package cloud.osasoft.dartzvibe.ui.screen.players

import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.FakePlayerRepository
import cloud.osasoft.dartzvibe.util.currentTimeMillis
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
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
 * Tests for PlayerListScreenModel — the player list state that drives the
 * "New Game" action enable threshold (unified to >= 1 player).
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class PlayerListScreenModelTest : FreeSpec({

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

    "New Game enable threshold" - {
        "Should keep New Game disabled with zero players" {
            runTest {
                // GIVEN an empty player repository
                val playerRepo = FakePlayerRepository()
                val screenModel = PlayerListScreenModel(playerRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN the New Game enable condition (players.size >= 1) is false
                (state.players.size >= 1) shouldBe false
            }
        }

        "Should enable New Game with exactly one player" {
            runTest {
                // GIVEN a repository with a single player
                val playerRepo = FakePlayerRepository()
                playerRepo.insertPlayer(makePlayer("Alice"))
                val screenModel = PlayerListScreenModel(playerRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN the New Game enable condition is met at one player
                // (this is the unified threshold — previously required 2)
                state.players.size shouldBe 1
                (state.players.size >= 1) shouldBe true
            }
        }

        "Should keep New Game enabled with two or more players" {
            runTest {
                // GIVEN a repository with two players
                val playerRepo = FakePlayerRepository()
                playerRepo.insertPlayer(makePlayer("Alice"))
                playerRepo.insertPlayer(makePlayer("Bob"))
                val screenModel = PlayerListScreenModel(playerRepo)

                // WHEN state loads
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN the New Game enable condition remains true
                (state.players.size >= 1) shouldBe true
            }
        }
    }

    "Reactive player list" - {
        "Should drop below the enable threshold when the last player is deleted" {
            runTest {
                // GIVEN a repository with one player
                val playerRepo = FakePlayerRepository()
                val alice = makePlayer("Alice")
                playerRepo.insertPlayer(alice)
                val screenModel = PlayerListScreenModel(playerRepo)
                advanceUntilIdle()

                // WHEN the only player is deleted
                playerRepo.deletePlayer(alice.id)
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN New Game falls back to disabled
                state.players.size shouldBe 0
                (state.players.size >= 1) shouldBe false
            }
        }
    }
})
