package cloud.osasoft.dartzvibe.ui.screen.game

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.ScreenModelStore
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cloud.osasoft.dartzvibe.data.model.GameConfig
import cloud.osasoft.dartzvibe.data.model.GameMode
import cloud.osasoft.dartzvibe.data.model.GameSession
import cloud.osasoft.dartzvibe.data.model.GameStatus
import cloud.osasoft.dartzvibe.data.model.GameType
import cloud.osasoft.dartzvibe.data.model.Multiplier
import cloud.osasoft.dartzvibe.data.model.Player
import cloud.osasoft.dartzvibe.data.repository.FakeGameRepository
import cloud.osasoft.dartzvibe.data.repository.FakePlayerRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The game's ScreenModel now lives as long as its screen is in the navigator stack, and popping the screen disposes it,
 * which cancels its `screenModelScope`. Saves requested right before a pop (End Turn then Back, Abandon, Finish) must
 * still land.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class ActiveGameScreenModelLifecycleTest : FreeSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000301")
    val player1Id = Uuid.parse("00000000-0000-0000-0000-000000000031")
    val player2Id = Uuid.parse("00000000-0000-0000-0000-000000000032")

    val session = GameSession(
        id = sessionId,
        config = GameConfig(
            gameType = GameType.CLASSIC_301,
            gameMode = GameMode.CLASSIC,
            doubleIn = false,
            doubleOut = false,
            playerIds = listOf(player1Id, player2Id),
            legsToWin = 1,
        ),
        startedAt = 0L,
    )

    suspend fun TestScope.loadedGame(): Pair<ActiveGameScreenModel, FakeGameRepository> {
        val playerRepository = FakePlayerRepository()
        playerRepository.insertPlayer(Player(id = player1Id, name = "Player 1", createdAt = 0L))
        playerRepository.insertPlayer(Player(id = player2Id, name = "Player 2", createdAt = 0L))
        val gameRepository = FakeGameRepository()
        gameRepository.insertSession(session)
        val screenModel = ActiveGameScreenModel(gameRepository, playerRepository, sessionId)
        advanceUntilIdle()
        return screenModel to gameRepository
    }

    // A model built outside rememberScreenModel (as here) has no scope of its own: while nothing in the process has
    // called rememberScreenModel, Voyager gives it one shared "standalone" scope, and onDispose() alone doesn't cancel
    // it. Disposing a screen with that key is what popping does to a real screen's model: its scope is cancelled and
    // dropped from the store, and the next model gets a fresh one. popScreen() checks the scope really was cancelled.
    val standaloneScreen = object : Screen {
        override val key = "standalone"

        @Composable
        override fun Content() = Unit
    }

    fun ActiveGameScreenModel.popScreen() {
        val scope = screenModelScope
        ScreenModelStore.onDispose(standaloneScreen)
        onDispose()
        scope.isActive shouldBe false
    }

    "Screen keys" - {
        "Should give every game screen its own key, so a new screen for the same game never reuses a model" {
            // GIVEN two screens for the same game, as when a game is left and resumed
            val first = ActiveGameScreen(sessionId)
            val resumed = ActiveGameScreen(sessionId)

            // THEN their keys, which key their ScreenModels, differ
            resumed.key shouldNotBe first.key
        }
    }

    "Leaving the screen" - {
        "Should still save an ended turn when the screen is popped before the save runs" {
            runTest {
                // GIVEN a turn of S20 S20 S20 just ended, its save queued but not yet run
                val (screenModel, gameRepository) = loadedGame()
                repeat(3) { screenModel.onScoreSelect(20, Multiplier.SINGLE) }
                screenModel.endTurn()

                // WHEN the screen is popped right away (Back)
                screenModel.popScreen()
                advanceUntilIdle()

                // THEN the turn is saved anyway
                val saved = gameRepository.getGameSessionById(sessionId).first()!!
                saved.currentLeg.turns.size shouldBe 1
                saved.currentLeg.turns.single().totalScore shouldBe 60
            }
        }

        "Should still save an abandon whose save is in flight when the screen is popped" {
            runTest {
                // GIVEN a game whose next save is held in flight
                val (screenModel, gameRepository) = loadedGame()
                val gate = CompletableDeferred<Unit>()
                gameRepository.updateGate = gate

                // WHEN the game is abandoned and the screen pops while that save is still running
                screenModel.abandonGame()
                advanceUntilIdle()
                screenModel.popScreen()
                gate.complete(Unit)
                advanceUntilIdle()

                // THEN the game is stored as abandoned, so Home no longer offers Resume Game
                gameRepository.getGameSessionById(sessionId).first()!!.status shouldBe GameStatus.ABANDONED
                gameRepository.getInProgressGame().first() shouldBe null
            }
        }
    }
})
