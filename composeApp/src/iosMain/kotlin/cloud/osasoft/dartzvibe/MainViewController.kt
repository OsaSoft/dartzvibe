package cloud.osasoft.dartzvibe

import androidx.compose.ui.window.ComposeUIViewController
import cloud.osasoft.dartzvibe.data.local.DartzVibeDatabase
import cloud.osasoft.dartzvibe.data.local.DatabaseDriverFactory
import cloud.osasoft.dartzvibe.data.local.SettingsFactory
import cloud.osasoft.dartzvibe.data.repository.AppSettingsRepositoryImpl
import cloud.osasoft.dartzvibe.data.repository.GameRepositoryImpl
import cloud.osasoft.dartzvibe.data.repository.PlayerRepositoryImpl

@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController {
    val driverFactory = DatabaseDriverFactory()
    val database = DartzVibeDatabase(driverFactory.createDriver())
    val settingsFactory = SettingsFactory()
    val settings = settingsFactory.createSettings()
    val playerRepository = PlayerRepositoryImpl(database)
    val gameRepository = GameRepositoryImpl(database)
    val appSettingsRepository = AppSettingsRepositoryImpl(settings)
    App(
        playerRepository = playerRepository,
        gameRepository = gameRepository,
        appSettingsRepository = appSettingsRepository,
    )
}
