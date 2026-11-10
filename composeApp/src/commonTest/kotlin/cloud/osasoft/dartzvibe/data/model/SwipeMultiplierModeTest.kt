package cloud.osasoft.dartzvibe.data.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class SwipeMultiplierModeTest : FreeSpec({

    "SwipeMultiplierMode" - {
        "Should round-trip every mode through its stored value" - {
            withData(SwipeMultiplierMode.entries.toList()) { mode ->
                // GIVEN a mode's stored value
                val stored = mode.value

                // WHEN it is read back
                val restored = SwipeMultiplierMode.fromValue(stored)

                // THEN the same mode comes back
                restored shouldBe mode
            }
        }

        "Should fall back to vertical for unknown stored values" - {
            withData(
                mapOf(
                    "empty" to "",
                    "garbage" to "garbage",
                    "enum name VERTICAL" to "VERTICAL",
                    "enum name RADIAL" to "RADIAL",
                ),
            ) { stored ->
                // GIVEN a stored value that is not a known mode value
                // WHEN it is read back
                val restored = SwipeMultiplierMode.fromValue(stored)

                // THEN the safe default is used
                restored shouldBe SwipeMultiplierMode.VERTICAL
            }
        }

        "Should default to vertical in app settings" {
            // GIVEN default app settings
            val settings = AppSettingsData()

            // WHEN the swipe mode is read
            // THEN it is the existing up/down behaviour
            settings.swipeMultiplierMode shouldBe SwipeMultiplierMode.VERTICAL
        }
    }
})
