package cloud.osasoft.dartzvibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appComponent = (application as DartzVibeApplication).appComponent

        setContent {
            App(
                playerRepository = appComponent.playerRepository,
                gameRepository = appComponent.gameRepository,
                appSettingsRepository = appComponent.appSettingsRepository,
            )
        }
    }
}
