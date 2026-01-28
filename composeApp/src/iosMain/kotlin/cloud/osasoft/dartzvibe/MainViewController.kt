package cloud.osasoft.dartzvibe

import androidx.compose.ui.window.ComposeUIViewController
import cloud.osasoft.dartzvibe.data.local.DartzVibeDatabase
import cloud.osasoft.dartzvibe.data.local.DatabaseDriverFactory
import cloud.osasoft.dartzvibe.data.local.SettingsFactory
import cloud.osasoft.dartzvibe.data.repository.AppSettingsRepositoryImpl
import cloud.osasoft.dartzvibe.data.repository.GameRepositoryImpl
import cloud.osasoft.dartzvibe.data.repository.PlayerRepositoryImpl

private object IosAppDependencies {
    private val database: DartzVibeDatabase by lazy {
        val driverFactory = DatabaseDriverFactory()
        DartzVibeDatabase(driverFactory.createDriver())
    }

    private val settings by lazy {
        val settingsFactory = SettingsFactory()
        settingsFactory.createSettings()
    }

    val playerRepository by lazy { PlayerRepositoryImpl(database) }
    val gameRepository by lazy { GameRepositoryImpl(database) }
    val appSettingsRepository by lazy { AppSettingsRepositoryImpl(settings) }
}

@Suppress("ktlint:standard:function-naming", "unused")
fun MainViewController() = ComposeUIViewController {
    App(
        playerRepository = IosAppDependencies.playerRepository,
        gameRepository = IosAppDependencies.gameRepository,
        appSettingsRepository = IosAppDependencies.appSettingsRepository,
    )
}
