package cloud.osasoft.dartzvibe.data.local

import com.russhwolf.settings.ObservableSettings

expect class SettingsFactory {
    fun createSettings(): ObservableSettings
}
