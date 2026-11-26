package cloud.osasoft.dartzvibe.data.model

data class AppSettingsData(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val keepScreenOn: Boolean = false,
    val showCheckoutHints: Boolean = true,
    val useRadialKeypad: Boolean = false,
    val showMultiplierButtons: Boolean = true,
    val swipeMultiplierMode: SwipeMultiplierMode = SwipeMultiplierMode.VERTICAL,
    val showKnockoutHints: Boolean = true,
    /** Show the scoreboard view (big text, readable from the oche) between turns; toggled from the game's top bar. */
    val scoreboardMode: Boolean = false,
)
