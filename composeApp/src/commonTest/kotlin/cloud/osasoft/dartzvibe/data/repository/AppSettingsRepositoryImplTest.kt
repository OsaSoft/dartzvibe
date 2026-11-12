package cloud.osasoft.dartzvibe.data.repository

import cloud.osasoft.dartzvibe.data.model.AppSettingsData
import cloud.osasoft.dartzvibe.data.model.SwipeMultiplierMode
import cloud.osasoft.dartzvibe.data.model.ThemeMode
import com.russhwolf.settings.MapSettings
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * Tests the real [AppSettingsRepositoryImpl] against in-memory [MapSettings].
 *
 * [MapSettings] keeps its listeners in a plain list, so the repository must register them from the test's
 * single-threaded dispatcher: with the default IO dispatcher the combined flows add listeners from several threads at
 * once and the list throws now and then.
 */
class AppSettingsRepositoryImplTest : FreeSpec({

    fun TestScope.repository(storage: MapSettings = MapSettings()) =
        AppSettingsRepositoryImpl(storage, StandardTestDispatcher(testScheduler))

    "Listener registration" - {
        "Should read the settings many times in a row without a listener race" {
            runTest {
                // GIVEN fresh in-memory storage for every read, like the flaky runs had
                // WHEN the settings are read a thousand times (on IO this threw within a few hundred reads)
                val reads = (1..1000).map { repository().getSettings().first() }

                // THEN every read returns the defaults
                reads.distinct() shouldBe listOf(AppSettingsData())
            }
        }
    }

    "Dartboard swipe direction" - {
        "Should default to vertical when nothing is stored" {
            runTest {
                // GIVEN empty settings storage
                val repository = repository()

                // WHEN the settings are read
                val settings = repository.getSettings().first()

                // THEN the swipe direction is the existing up/down mapping
                settings.swipeMultiplierMode shouldBe SwipeMultiplierMode.VERTICAL
            }
        }

        "Should read back radial after it is set, also through a new repository on the same storage" {
            runTest {
                // GIVEN a repository on some settings storage
                val storage = MapSettings()
                val repository = repository(storage)

                // WHEN radial is set
                repository.setSwipeMultiplierMode(SwipeMultiplierMode.RADIAL)

                // THEN getSettings() returns radial
                repository.getSettings().first().swipeMultiplierMode shouldBe SwipeMultiplierMode.RADIAL
                // AND it is stored by its stable value, not its enum name
                storage.getStringOrNull("swipe_multiplier_mode") shouldBe "radial"
                // AND a new repository on the same storage (like after an app restart) reads radial
                repository(storage).getSettings().first().swipeMultiplierMode shouldBe
                    SwipeMultiplierMode.RADIAL
            }
        }

        "Should fall back to vertical for an unknown stored value" {
            runTest {
                // GIVEN storage holding a value no mode uses
                val storage = MapSettings()
                storage.putString("swipe_multiplier_mode", "sideways")

                // WHEN the settings are read
                val settings = repository(storage).getSettings().first()

                // THEN the safe default is used
                settings.swipeMultiplierMode shouldBe SwipeMultiplierMode.VERTICAL
            }
        }

        "Should keep the other settings when the swipe direction is layered on" {
            runTest {
                // GIVEN a repository with non-default values in the first-stage settings
                val repository = repository()
                repository.setThemeMode(ThemeMode.DARK)
                repository.setShowMultiplierButtons(false)
                repository.setUseRadialKeypad(true)

                // WHEN the swipe direction is also set
                repository.setSwipeMultiplierMode(SwipeMultiplierMode.RADIAL)

                // THEN every setting round-trips through getSettings()
                val settings = repository.getSettings().first()
                settings.themeMode shouldBe ThemeMode.DARK
                settings.showMultiplierButtons shouldBe false
                settings.useRadialKeypad shouldBe true
                settings.swipeMultiplierMode shouldBe SwipeMultiplierMode.RADIAL
            }
        }
    }
})
