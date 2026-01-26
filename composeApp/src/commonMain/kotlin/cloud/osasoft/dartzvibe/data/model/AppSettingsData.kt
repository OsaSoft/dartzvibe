package cloud.osasoft.dartzvibe.data.model

data class AppSettingsData(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val keepScreenOn: Boolean = false,
    val showCheckoutHints: Boolean = true,
)
