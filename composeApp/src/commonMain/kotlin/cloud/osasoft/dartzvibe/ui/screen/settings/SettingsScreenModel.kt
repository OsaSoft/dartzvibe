package cloud.osasoft.dartzvibe.ui.screen.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import cloud.osasoft.dartzvibe.data.model.SwipeMultiplierMode
import cloud.osasoft.dartzvibe.data.model.ThemeMode
import cloud.osasoft.dartzvibe.data.repository.AppSettingsRepository
import cloud.osasoft.dartzvibe.ui.screen.launchToCompletion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

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

    // The setters write with launchToCompletion: Back right after a toggle disposes this model, and the write must
    // still land

    fun setThemeMode(mode: ThemeMode) {
        launchToCompletion {
            appSettingsRepository.setThemeMode(mode)
        }
    }

    fun setKeepScreenOn(value: Boolean) {
        launchToCompletion {
            appSettingsRepository.setKeepScreenOn(value)
        }
    }

    fun setShowCheckoutHints(value: Boolean) {
        launchToCompletion {
            appSettingsRepository.setShowCheckoutHints(value)
        }
    }

    fun setUseRadialKeypad(value: Boolean) {
        launchToCompletion {
            appSettingsRepository.setUseRadialKeypad(value)
        }
    }

    fun setShowMultiplierButtons(value: Boolean) {
        launchToCompletion {
            appSettingsRepository.setShowMultiplierButtons(value)
        }
    }

    fun setSwipeMultiplierMode(mode: SwipeMultiplierMode) {
        launchToCompletion {
            appSettingsRepository.setSwipeMultiplierMode(mode)
        }
    }
}
