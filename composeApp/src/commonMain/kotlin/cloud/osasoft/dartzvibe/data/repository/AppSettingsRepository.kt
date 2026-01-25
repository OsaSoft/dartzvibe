package cloud.osasoft.dartzvibe.data.repository

import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.tatarka.inject.annotations.Inject

interface AppSettingsRepository {
    fun getSettings(): Flow<AppSettingsData>
    suspend fun setKeepScreenOn(value: Boolean)
    suspend fun setShowCheckoutHints(value: Boolean)
}

@OptIn(ExperimentalSettingsApi::class)
@Inject
class AppSettingsRepositoryImpl(
    settings: ObservableSettings,
) : AppSettingsRepository {
    private val flowSettings: FlowSettings = settings.toFlowSettings()

    override fun getSettings(): Flow<AppSettingsData> = combine(
        flowSettings.getBooleanFlow(KEY_KEEP_SCREEN_ON, defaultValue = false),
        flowSettings.getBooleanFlow(KEY_SHOW_CHECKOUT_HINTS, defaultValue = true),
    ) { keepScreenOn, showCheckoutHints ->
        AppSettingsData(
            keepScreenOn = keepScreenOn,
            showCheckoutHints = showCheckoutHints,
        )
    }

    override suspend fun setKeepScreenOn(value: Boolean) {
        flowSettings.putBoolean(KEY_KEEP_SCREEN_ON, value)
    }

    override suspend fun setShowCheckoutHints(value: Boolean) {
        flowSettings.putBoolean(KEY_SHOW_CHECKOUT_HINTS, value)
    }

    companion object {
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_SHOW_CHECKOUT_HINTS = "show_checkout_hints"
    }
}
