package cloud.osasoft.dartzvibe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import cloud.osasoft.dartzvibe.data.repository.AppSettingsRepository
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.ui.screen.home.HomeScreen
import cloud.osasoft.dartzvibe.ui.theme.DartzVibeTheme

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

/**
 * CompositionLocal for accessing the AppSettingsRepository throughout the app.
 */
val LocalAppSettingsRepository = staticCompositionLocalOf<AppSettingsRepository> {
    error("AppSettingsRepository not provided")
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun App(
    playerRepository: PlayerRepository,
    gameRepository: GameRepository,
    appSettingsRepository: AppSettingsRepository,
) {
    val settings by appSettingsRepository.getSettings().collectAsState(initial = AppSettingsData())

    CompositionLocalProvider(
        LocalPlayerRepository provides playerRepository,
        LocalGameRepository provides gameRepository,
        LocalAppSettingsRepository provides appSettingsRepository,
    ) {
        DartzVibeTheme(themeMode = settings.themeMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Navigator(HomeScreen()) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }
}
