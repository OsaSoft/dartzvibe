package cloud.osasoft.dartzvibe.data.repository

import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import cloud.osasoft.dartzvibe.data.model.SwipeMultiplierMode
import cloud.osasoft.dartzvibe.data.model.ThemeMode
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.tatarka.inject.annotations.Inject

interface AppSettingsRepository {
    fun getSettings(): Flow<AppSettingsData>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setKeepScreenOn(value: Boolean)
    suspend fun setShowCheckoutHints(value: Boolean)
    suspend fun setUseRadialKeypad(value: Boolean)
    suspend fun setShowMultiplierButtons(value: Boolean)
    suspend fun setSwipeMultiplierMode(mode: SwipeMultiplierMode)
}

@OptIn(ExperimentalSettingsApi::class)
@Inject
class AppSettingsRepositoryImpl(
    settings: ObservableSettings,
) : AppSettingsRepository {
    private val flowSettings: FlowSettings = settings.toFlowSettings()

    // The typed combine overloads stop at 5 flows, so later settings are layered onto the first five
    override fun getSettings(): Flow<AppSettingsData> = combine(
        baseSettings(),
        flowSettings.getStringFlow(KEY_SWIPE_MULTIPLIER_MODE, defaultValue = SwipeMultiplierMode.VERTICAL.value),
    ) { base, swipeMultiplierMode ->
        base.copy(swipeMultiplierMode = SwipeMultiplierMode.fromValue(swipeMultiplierMode))
    }

    private fun baseSettings(): Flow<AppSettingsData> = combine(
        flowSettings.getStringFlow(KEY_THEME_MODE, defaultValue = ThemeMode.SYSTEM.value),
        flowSettings.getBooleanFlow(KEY_KEEP_SCREEN_ON, defaultValue = false),
        flowSettings.getBooleanFlow(KEY_SHOW_CHECKOUT_HINTS, defaultValue = true),
        flowSettings.getBooleanFlow(KEY_USE_RADIAL_KEYPAD, defaultValue = false),
        flowSettings.getBooleanFlow(KEY_SHOW_MULTIPLIER_BUTTONS, defaultValue = true),
    ) { themeMode, keepScreenOn, showCheckoutHints, useRadialKeypad, showMultiplierButtons ->
        AppSettingsData(
            themeMode = ThemeMode.fromValue(themeMode),
            keepScreenOn = keepScreenOn,
            showCheckoutHints = showCheckoutHints,
            useRadialKeypad = useRadialKeypad,
            showMultiplierButtons = showMultiplierButtons,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        flowSettings.putString(KEY_THEME_MODE, mode.value)
    }

    override suspend fun setKeepScreenOn(value: Boolean) {
        flowSettings.putBoolean(KEY_KEEP_SCREEN_ON, value)
    }

    override suspend fun setShowCheckoutHints(value: Boolean) {
        flowSettings.putBoolean(KEY_SHOW_CHECKOUT_HINTS, value)
    }

    override suspend fun setUseRadialKeypad(value: Boolean) {
        flowSettings.putBoolean(KEY_USE_RADIAL_KEYPAD, value)
    }

    override suspend fun setShowMultiplierButtons(value: Boolean) {
        flowSettings.putBoolean(KEY_SHOW_MULTIPLIER_BUTTONS, value)
    }

    override suspend fun setSwipeMultiplierMode(mode: SwipeMultiplierMode) {
        flowSettings.putString(KEY_SWIPE_MULTIPLIER_MODE, mode.value)
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_SHOW_CHECKOUT_HINTS = "show_checkout_hints"
        private const val KEY_USE_RADIAL_KEYPAD = "use_radial_keypad"
        private const val KEY_SHOW_MULTIPLIER_BUTTONS = "show_multiplier_buttons"
        private const val KEY_SWIPE_MULTIPLIER_MODE = "swipe_multiplier_mode"
    }
}
