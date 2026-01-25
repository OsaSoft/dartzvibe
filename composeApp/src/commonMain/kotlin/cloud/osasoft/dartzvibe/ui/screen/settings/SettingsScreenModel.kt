package cloud.osasoft.dartzvibe.ui.screen.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import cloud.osasoft.dartzvibe.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val settings: AppSettingsData = AppSettingsData(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class SettingsScreenModel(
    private val appSettingsRepository: AppSettingsRepository,
) : ScreenModel {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        appSettingsRepository
            .getSettings()
            .onEach { settings ->
                _state.update { it.copy(settings = settings, isLoading = false) }
            }
            .catch { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(screenModelScope)
    }

    fun setKeepScreenOn(value: Boolean) {
        screenModelScope.launch {
            appSettingsRepository.setKeepScreenOn(value)
        }
    }

    fun setShowCheckoutHints(value: Boolean) {
        screenModelScope.launch {
            appSettingsRepository.setShowCheckoutHints(value)
        }
    }
}
