package cloud.osasoft.dartzvibe

import androidx.compose.ui.window.ComposeUIViewController
import cloud.osasoft.dartzvibe.data.local.DartzVibeDatabase
import cloud.osasoft.dartzvibe.data.local.DatabaseDriverFactory
import cloud.osasoft.dartzvibe.data.repository.GameRepositoryImpl
import cloud.osasoft.dartzvibe.data.repository.PlayerRepositoryImpl

@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController {
    val driverFactory = DatabaseDriverFactory()
    val database = DartzVibeDatabase(driverFactory.createDriver())
    val playerRepository = PlayerRepositoryImpl(database)
    val gameRepository = GameRepositoryImpl(database)
    App(playerRepository = playerRepository, gameRepository = gameRepository)
}
