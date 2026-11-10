package cloud.osasoft.dartzvibe.ui.screen.game

import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.FakeGameRepository
import cloud.osasoft.dartzvibe.data.repository.FakePlayerRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class NewGameScreenModelTest : FreeSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    val playerIds = (1..4).map { index -> Uuid.parse("00000000-0000-0000-0000-00000000000$index") }

    // A seed whose shuffle of the 4 players differs from the selection order
    val seed = (0..1000).first { candidate -> playerIds.shuffled(Random(candidate)) != playerIds }

    suspend fun TestScope.createScreenModel(random: Random = Random(seed)): NewGameScreenModel {
        val playerRepository = FakePlayerRepository()
        playerIds.forEachIndexed { index, id ->
            playerRepository.insertPlayer(Player(id = id, name = "Player ${index + 1}", createdAt = 0L))
        }
        val screenModel = NewGameScreenModel(
            playerRepository = playerRepository,
            gameRepository = FakeGameRepository(),
            random = random,
        )
        advanceUntilIdle()
        return screenModel
    }

    "Random player order" - {
        "Should shuffle the throwing order when random order is enabled" {
            runTest {
                // GIVEN 4 selected players
                val screenModel = createScreenModel()
                playerIds.forEach { id -> screenModel.togglePlayerSelection(id) }
                // AND random order is enabled
                screenModel.setRandomizePlayerOrder(true)

                // WHEN the game is started
                screenModel.startGame()
                advanceUntilIdle()

                // THEN the game uses the seeded shuffle of the selected players
                val session = screenModel.state.value.createdSession.shouldNotBeNull()
                session.config.playerIds shouldContainExactly playerIds.shuffled(Random(seed))
                // AND every selected player is still in the game exactly once
                session.config.playerIds shouldContainExactlyInAnyOrder playerIds
            }
        }

        "Should keep the selected throwing order when random order is disabled" {
            runTest {
                // GIVEN 4 selected players
                val screenModel = createScreenModel()
                playerIds.forEach { id -> screenModel.togglePlayerSelection(id) }

                // WHEN the game is started without random order
                screenModel.startGame()
                advanceUntilIdle()

                // THEN the game uses the selection order
                val session = screenModel.state.value.createdSession.shouldNotBeNull()
                session.config.playerIds shouldContainExactly playerIds
            }
        }

        "Should not offer random order with a single player" {
            runTest {
                // GIVEN a single selected player
                val screenModel = createScreenModel()
                screenModel.togglePlayerSelection(playerIds.first())

                // WHEN random order is enabled
                screenModel.setRandomizePlayerOrder(true)

                // THEN random order is not available or applied
                screenModel.state.value.canRandomizePlayerOrder shouldBe false
                screenModel.state.value.isPlayerOrderRandomized shouldBe false
            }
        }

        "Should not apply random order in solo Checkout Practice" {
            runTest {
                // GIVEN 2 selected players with random order enabled
                val screenModel = createScreenModel()
                screenModel.togglePlayerSelection(playerIds[0])
                screenModel.togglePlayerSelection(playerIds[1])
                screenModel.setRandomizePlayerOrder(true)

                // WHEN switching to Checkout Practice (single-player mode)
                screenModel.setGameMode(GameMode.CHECKOUT_PRACTICE)

                // THEN random order is no longer applied
                screenModel.state.value.isPlayerOrderRandomized shouldBe false
            }
        }
    }
})
