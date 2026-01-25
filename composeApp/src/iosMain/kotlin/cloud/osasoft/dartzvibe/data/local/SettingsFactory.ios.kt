package cloud.osasoft.dartzvibe.data.local

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import platform.Foundation.NSUserDefaults

actual class SettingsFactory {
    actual fun createSettings(): ObservableSettings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
}
