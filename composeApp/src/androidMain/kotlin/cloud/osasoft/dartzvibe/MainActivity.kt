package cloud.osasoft.dartzvibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cloud.osasoft.dartzvibe.data.local.DartzVibeDatabase
import cloud.osasoft.dartzvibe.data.local.DatabaseDriverFactory
import cloud.osasoft.dartzvibe.data.local.SettingsFactory
import cloud.osasoft.dartzvibe.di.AppComponent
import cloud.osasoft.dartzvibe.di.create

class MainActivity : ComponentActivity() {

    private lateinit var appComponent: AppComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize database, settings, and DI
        val driverFactory = DatabaseDriverFactory(applicationContext)
        val database = DartzVibeDatabase(driverFactory.createDriver())
        val settingsFactory = SettingsFactory(applicationContext)
        val settings = settingsFactory.createSettings()
        appComponent = AppComponent::class.create(database, settings)

        setContent {
            App(
                playerRepository = appComponent.playerRepository,
                gameRepository = appComponent.gameRepository,
                appSettingsRepository = appComponent.appSettingsRepository,
            )
        }
    }
}
