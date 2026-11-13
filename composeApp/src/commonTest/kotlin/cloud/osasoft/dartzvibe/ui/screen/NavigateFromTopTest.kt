package cloud.osasoft.dartzvibe.ui.screen

import cloud.osasoft.dartzvibe.ui.screen.game.ActiveGameScreen
import cloud.osasoft.dartzvibe.ui.screen.home.HomeScreen
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class NavigateFromTopTest : FreeSpec({

    val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000401")

    "Navigating only from the top screen" - {
        "Should let the top screen navigate" {
            // GIVEN Home on top
            val home = HomeScreen()

            // THEN Home may push (the first tap on Resume Game)
            canNavigateFrom(top = home, current = home) shouldBe true
        }

        "Should ignore a second tap from a screen that was already covered" {
            // GIVEN Home's first tap on Resume Game already pushed a game screen
            val home = HomeScreen()
            val game = ActiveGameScreen(sessionId)

            // THEN a second tap from Home during the slide doesn't push another game screen
            canNavigateFrom(top = game, current = home) shouldBe false
        }

        "Should tell apart two screens of the same class" {
            // GIVEN two game screens for the same game
            val covered = ActiveGameScreen(sessionId)
            val top = ActiveGameScreen(sessionId)

            // THEN only the top one may navigate
            canNavigateFrom(top = top, current = covered) shouldBe false
            canNavigateFrom(top = top, current = top) shouldBe true
        }

        "Should not navigate from an empty stack" {
            canNavigateFrom(top = null, current = HomeScreen()) shouldBe false
        }
    }
})
