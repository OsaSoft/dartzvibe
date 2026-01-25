package cloud.osasoft.dartzvibe.data.repository

import cloud.osasoft.dartzvibe.data.model.AppSettingsData
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

    override suspend fun setKeepScreenOn(value: Boolean) {
        _settings.update { it.copy(keepScreenOn = value) }
    }

    override suspend fun setShowCheckoutHints(value: Boolean) {
        _settings.update { it.copy(showCheckoutHints = value) }
    }

    // Test helpers
    fun reset() {
        _settings.value = AppSettingsData()
    }

    fun getCurrentSettings(): AppSettingsData = _settings.value
}
