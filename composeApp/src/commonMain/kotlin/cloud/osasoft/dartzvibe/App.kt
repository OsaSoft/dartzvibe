package cloud.osasoft.dartzvibe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.ui.screen.home.HomeScreen

/**
 * CompositionLocal for accessing the PlayerRepository throughout the app.
 */
val LocalPlayerRepository = staticCompositionLocalOf<PlayerRepository> {
    error("PlayerRepository not provided")
}

/**
 * CompositionLocal for accessing the GameRepository throughout the app.
 */
val LocalGameRepository = staticCompositionLocalOf<GameRepository> {
    error("GameRepository not provided")
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun App(playerRepository: PlayerRepository, gameRepository: GameRepository) {
    CompositionLocalProvider(
        LocalPlayerRepository provides playerRepository,
        LocalGameRepository provides gameRepository,
    ) {
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Navigator(HomeScreen(playerRepository, gameRepository)) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }
}
