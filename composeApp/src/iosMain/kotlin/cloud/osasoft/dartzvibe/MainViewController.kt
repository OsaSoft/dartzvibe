package cloud.osasoft.dartzvibe

import androidx.compose.ui.window.ComposeUIViewController
import cloud.osasoft.dartzvibe.data.local.DatabaseDriverFactory
import cloud.osasoft.dartzvibe.data.local.DartzVibeDatabase
import cloud.osasoft.dartzvibe.data.repository.PlayerRepositoryImpl

fun MainViewController() = ComposeUIViewController {
    val driverFactory = DatabaseDriverFactory()
    val database = DartzVibeDatabase(driverFactory.createDriver())
    val playerRepository = PlayerRepositoryImpl(database)
    App(playerRepository = playerRepository)
}