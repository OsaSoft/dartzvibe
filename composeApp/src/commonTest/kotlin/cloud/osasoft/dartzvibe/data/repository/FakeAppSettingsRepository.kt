package cloud.osasoft.dartzvibe.data.repository

import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import cloud.osasoft.dartzvibe.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of AppSettingsRepository for testing purposes.
 */
class FakeAppSettingsRepository : AppSettingsRepository {

    private val _settings = MutableStateFlow(AppSettingsData())

    override fun getSettings(): Flow<AppSettingsData> = _settings.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        _settings.update { it.copy(themeMode = mode) }
    }

    override suspend fun setKeepScreenOn(value: Boolean) {
        _settings.update { it.copy(keepScreenOn = value) }
    }

    override suspend fun setShowCheckoutHints(value: Boolean) {
        _settings.update { it.copy(showCheckoutHints = value) }
    }

    override suspend fun setUseRadialKeypad(value: Boolean) {
        _settings.update { it.copy(useRadialKeypad = value) }
    }

    override suspend fun setShowMultiplierButtons(value: Boolean) {
        _settings.update { it.copy(showMultiplierButtons = value) }
    }

    // Test helpers
    fun reset() {
        _settings.value = AppSettingsData()
    }

    fun getCurrentSettings(): AppSettingsData = _settings.value
}
