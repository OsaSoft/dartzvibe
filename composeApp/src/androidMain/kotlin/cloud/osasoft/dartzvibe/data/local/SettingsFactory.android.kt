package cloud.osasoft.dartzvibe.data.local

import android.content.Context
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings

actual class SettingsFactory(private val context: Context) {
    actual fun createSettings(): ObservableSettings = SharedPreferencesSettings(
        context.getSharedPreferences("dartzvibe_settings", Context.MODE_PRIVATE),
    )
}
