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
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.ui.screen.players.PlayerListScreen

/**
 * CompositionLocal for accessing the PlayerRepository throughout the app.
 */
val LocalPlayerRepository = staticCompositionLocalOf<PlayerRepository> {
    error("PlayerRepository not provided")
}

@Composable
fun App(playerRepository: PlayerRepository) {
    CompositionLocalProvider(LocalPlayerRepository provides playerRepository) {
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Navigator(PlayerListScreen(playerRepository)) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }
}