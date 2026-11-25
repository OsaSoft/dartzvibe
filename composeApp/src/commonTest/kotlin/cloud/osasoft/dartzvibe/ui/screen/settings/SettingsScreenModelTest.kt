package cloud.osasoft.dartzvibe.ui.screen.settings

import cloud.osasoft.dartzvibe.data.model.SwipeMultiplierMode
import cloud.osasoft.dartzvibe.data.model.ThemeMode
import cloud.osasoft.dartzvibe.data.repository.FakeAppSettingsRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
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
                // AND knockout hints are on by default too
                state.settings.showKnockoutHints shouldBe true
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

        "Should update show knockout hints setting without touching checkout hints" {
            runTest {
                // GIVEN a SettingsScreenModel
                val repository = FakeAppSettingsRepository()
                val screenModel = SettingsScreenModel(repository)
                advanceUntilIdle()

                // WHEN setShowKnockoutHints(false) is called
                screenModel.setShowKnockoutHints(false)
                advanceUntilIdle()

                // THEN state and the repository hold false, and checkout hints stay on
                val state = screenModel.state.first()
                state.settings.showKnockoutHints shouldBe false
                state.settings.showCheckoutHints shouldBe true
                repository.getCurrentSettings().showKnockoutHints shouldBe false
            }
        }

        "Should default the dartboard swipe direction to up/down" {
            runTest {
                // GIVEN a fresh SettingsScreenModel
                val repository = FakeAppSettingsRepository()
                val screenModel = SettingsScreenModel(repository)

                // WHEN state is observed after initialization
                advanceUntilIdle()
                val state = screenModel.state.first()

                // THEN the swipe direction is the existing vertical mapping
                state.settings.swipeMultiplierMode shouldBe SwipeMultiplierMode.VERTICAL
            }
        }

        "Should update the dartboard swipe direction setting" {
            runTest {
                // GIVEN a SettingsScreenModel
                val repository = FakeAppSettingsRepository()
                val screenModel = SettingsScreenModel(repository)
                advanceUntilIdle()

                // WHEN setSwipeMultiplierMode(RADIAL) is called
                screenModel.setSwipeMultiplierMode(SwipeMultiplierMode.RADIAL)
                advanceUntilIdle()

                // THEN state and the repository both hold RADIAL
                screenModel.state.first().settings.swipeMultiplierMode shouldBe SwipeMultiplierMode.RADIAL
                repository.getCurrentSettings().swipeMultiplierMode shouldBe SwipeMultiplierMode.RADIAL
                // AND the other multiplier settings are unchanged
                repository.getCurrentSettings().showMultiplierButtons shouldBe true

                // AND WHEN it is switched back to VERTICAL
                screenModel.setSwipeMultiplierMode(SwipeMultiplierMode.VERTICAL)
                advanceUntilIdle()

                // THEN state holds VERTICAL again
                screenModel.state.first().settings.swipeMultiplierMode shouldBe SwipeMultiplierMode.VERTICAL
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

        "Dartboard swipe direction availability" - {
            data class Case(val useRadialKeypad: Boolean, val showMultiplierButtons: Boolean, val enabled: Boolean) {
                override fun toString() =
                    "Should be ${if (enabled) "enabled" else "disabled"} with radial dartboard " +
                        "${if (useRadialKeypad) "on" else "off"} and S/D/T buttons ${if (showMultiplierButtons) "on" else "off"}"
            }

            withData(
                Case(useRadialKeypad = true, showMultiplierButtons = false, enabled = true),
                Case(useRadialKeypad = true, showMultiplierButtons = true, enabled = false),
                Case(useRadialKeypad = false, showMultiplierButtons = false, enabled = false),
                Case(useRadialKeypad = false, showMultiplierButtons = true, enabled = false),
            ) { case ->
                runTest {
                    // GIVEN a SettingsScreenModel
                    val repository = FakeAppSettingsRepository()
                    val screenModel = SettingsScreenModel(repository)
                    advanceUntilIdle()

                    // WHEN the keypad switches are set
                    screenModel.setUseRadialKeypad(case.useRadialKeypad)
                    screenModel.setShowMultiplierButtons(case.showMultiplierButtons)
                    advanceUntilIdle()

                    // THEN the swipe direction option is only enabled when it affects input
                    screenModel.state.first().isSwipeMultiplierModeEnabled shouldBe case.enabled
                }
            }

            "Should be disabled with the default settings" {
                runTest {
                    // GIVEN a fresh SettingsScreenModel (grid keypad, S/D/T buttons on)
                    val screenModel = SettingsScreenModel(FakeAppSettingsRepository())

                    // WHEN the settings have loaded
                    advanceUntilIdle()

                    // THEN the swipe direction option is disabled
                    screenModel.state.first().isSwipeMultiplierModeEnabled shouldBe false
                }
            }

            "Should keep the chosen direction while disabled and re-enabled" {
                runTest {
                    // GIVEN the swipe direction is enabled and set to RADIAL
                    val repository = FakeAppSettingsRepository()
                    val screenModel = SettingsScreenModel(repository)
                    advanceUntilIdle()
                    screenModel.setUseRadialKeypad(true)
                    screenModel.setShowMultiplierButtons(false)
                    screenModel.setSwipeMultiplierMode(SwipeMultiplierMode.RADIAL)
                    advanceUntilIdle()

                    // WHEN the S/D/T buttons are turned on (disabling it)
                    screenModel.setShowMultiplierButtons(true)
                    advanceUntilIdle()

                    // THEN it is disabled and the direction is still RADIAL
                    screenModel.state.first().isSwipeMultiplierModeEnabled shouldBe false
                    screenModel.state.first().settings.swipeMultiplierMode shouldBe SwipeMultiplierMode.RADIAL

                    // AND when the S/D/T buttons are turned off again
                    screenModel.setShowMultiplierButtons(false)
                    advanceUntilIdle()

                    // THEN it is enabled again with RADIAL still selected
                    screenModel.state.first().isSwipeMultiplierModeEnabled shouldBe true
                    screenModel.state.first().settings.swipeMultiplierMode shouldBe SwipeMultiplierMode.RADIAL
                }
            }
        }
    }
})
