package cloud.osasoft.dartzvibe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.SlideTransition
import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import cloud.osasoft.dartzvibe.data.repository.AppSettingsRepository
import cloud.osasoft.dartzvibe.data.repository.GameRepository
import cloud.osasoft.dartzvibe.data.repository.PlayerRepository
import cloud.osasoft.dartzvibe.ui.screen.home.HomeScreen
import cloud.osasoft.dartzvibe.ui.theme.DartzVibeTheme
import cloud.osasoft.dartzvibe.util.KeepScreenOn

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

/**
 * The current app settings, collected once at the root. Screens read settings from here rather than collecting
 * `getSettings()` themselves, so a screen coming back from Settings shows the new values from its first frame instead
 * of starting over from the defaults.
 */
val LocalAppSettings = compositionLocalOf { AppSettingsData() }

@OptIn(ExperimentalVoyagerApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun App(
    playerRepository: PlayerRepository,
    gameRepository: GameRepository,
    appSettingsRepository: AppSettingsRepository,
) {
    // One flow for the app's lifetime: a new one per recomposition would restart the collection on every change
    val settingsFlow = remember(appSettingsRepository) { appSettingsRepository.getSettings() }
    val settings by settingsFlow.collectAsState(initial = AppSettingsData())

    KeepScreenOn(enabled = settings.keepScreenOn)

    CompositionLocalProvider(
        LocalPlayerRepository provides playerRepository,
        LocalGameRepository provides gameRepository,
        LocalAppSettingsRepository provides appSettingsRepository,
        LocalAppSettings provides settings,
    ) {
        DartzVibeTheme(themeMode = settings.themeMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                // ScreenModels (rememberScreenModel) live while their screen is in the stack. A popped or replaced
                // screen's model is disposed once the slide ends, not at the pop, because the outgoing screen is
                // still composed during the slide and would otherwise build a fresh model.
                Navigator(
                    screen = HomeScreen(),
                    disposeBehavior = NavigatorDisposeBehavior(disposeSteps = false),
                ) { navigator ->
                    SlideTransition(navigator = navigator, disposeScreenAfterTransitionEnd = true)
                }
            }
        }
    }
}
