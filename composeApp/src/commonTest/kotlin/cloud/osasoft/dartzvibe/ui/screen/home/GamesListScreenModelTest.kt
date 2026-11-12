package cloud.osasoft.dartzvibe.ui.screen.home

import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.FakeGameRepository
import cloud.osasoft.dartzvibe.data.repository.FakePlayerRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class GamesListScreenModelTest : FreeSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    "Coming back to the Games list" - {
        "Should show a game's new status and a renamed player after returning from the game screens" {
            runTest {
                // GIVEN the list loaded with one in-progress game of Alice's
                val playerRepository = FakePlayerRepository()
                val gameRepository = FakeGameRepository()
                val alice = Player(id = Uuid.random(), name = "Alice", createdAt = 0L)
                playerRepository.insertPlayer(alice)
                val session = gameRepository.createGameSession(
                    GameConfig(gameType = GameType.CLASSIC_301, playerIds = listOf(alice.id)),
                )
                val screenModel = GamesListScreenModel(playerRepository, gameRepository)
                advanceUntilIdle()

                // WHEN the game is abandoned, Alice is renamed and a second game is started from the screens above
                gameRepository.updateGameSession(session.copy(status = GameStatus.ABANDONED))
                playerRepository.updatePlayer(alice.copy(name = "Alicia"))
                gameRepository.createGameSession(
                    GameConfig(gameType = GameType.CLASSIC_501, playerIds = listOf(alice.id)),
                )
                advanceUntilIdle()

                // THEN the list shows both games, the first as abandoned, with the new name
                val games = screenModel.state.value.games
                games.size shouldBe 2
                games.single { it.session.id == session.id }.session.status shouldBe GameStatus.ABANDONED
                games.flatMap { it.players }.map { it.name }.distinct() shouldBe listOf("Alicia")
            }
        }
    }
})
