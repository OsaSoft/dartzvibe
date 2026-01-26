package cloud.osasoft.dartzvibe.ui.screen.settings

import cloud.osasoft.dartzvibe.data.model.ThemeMode
import cloud.osasoft.dartzvibe.data.repository.FakeAppSettingsRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Tests for SettingsScreenModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenModelTest : FreeSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    "Settings" - {
        "Should load default settings initially" {
            runTest {
                // GIVEN a fresh SettingsScreenModel
                val repository = FakeAppSettingsRepository()
                val screenModel = SettingsScreenModel(repository)

                // WHEN state is observed after initialization
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN themeMode is SYSTEM, keepScreenOn is false and showCheckoutHints is true (defaults)
                state.settings.themeMode shouldBe ThemeMode.SYSTEM
                state.settings.keepScreenOn shouldBe false
                state.settings.showCheckoutHints shouldBe true
                state.isLoading shouldBe false
            }
        }

        "Should update theme mode setting" {
            runTest {
                // GIVEN a SettingsScreenModel
                val repository = FakeAppSettingsRepository()
                val screenModel = SettingsScreenModel(repository)
                advanceUntilIdle()

                // WHEN setThemeMode(DARK) is called
                screenModel.setThemeMode(ThemeMode.DARK)
                advanceUntilIdle()

                // THEN state.settings.themeMode should be DARK
                val state = screenModel.state.first()
                state.settings.themeMode shouldBe ThemeMode.DARK
            }
        }

        "Should update keep screen on setting" {
            runTest {
                // GIVEN a SettingsScreenModel
                val repository = FakeAppSettingsRepository()
                val screenModel = SettingsScreenModel(repository)
                advanceUntilIdle()

                // WHEN setKeepScreenOn(true) is called
                screenModel.setKeepScreenOn(true)
                advanceUntilIdle()

                // THEN state.settings.keepScreenOn should be false
                val state = screenModel.state.first()
                state.settings.keepScreenOn shouldBe true
            }
        }

        "Should update show checkout hints setting" {
            runTest {
                // GIVEN a SettingsScreenModel
                val repository = FakeAppSettingsRepository()
                val screenModel = SettingsScreenModel(repository)
                advanceUntilIdle()

                // WHEN setShowCheckoutHints(false) is called
                screenModel.setShowCheckoutHints(false)
                advanceUntilIdle()

                // THEN state.settings.showCheckoutHints should be false
                val state = screenModel.state.first()
                state.settings.showCheckoutHints shouldBe false
            }
        }

        "Should persist settings independently" {
            runTest {
                // GIVEN a SettingsScreenModel
                val repository = FakeAppSettingsRepository()
                val screenModel = SettingsScreenModel(repository)
                advanceUntilIdle()

                // WHEN one setting is changed
                screenModel.setKeepScreenOn(true)
                advanceUntilIdle()

                // THEN other settings remain unchanged
                val state = screenModel.state.first()
                state.settings.keepScreenOn shouldBe true
                state.settings.showCheckoutHints shouldBe true

                // AND when the other setting is changed
                screenModel.setShowCheckoutHints(false)
                advanceUntilIdle()

                // THEN the first setting remains unchanged
                val updatedState = screenModel.state.first()
                updatedState.settings.keepScreenOn shouldBe true
                updatedState.settings.showCheckoutHints shouldBe false
            }
        }
    }
})
