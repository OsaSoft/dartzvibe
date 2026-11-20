---
paths:
  - "**/ui/screen/**"
  - "**/App.kt"
  - "**/NavigateFromTop.kt"
---

# Screens and navigation

- **Brand red as text:** `colorScheme.primary` (#C62828) is the fill colour (buttons, badges, white on red) and is too dark as text on the dark theme's greys. Score highlights and labels in brand red (Cricket PTS, points and closed marks, the turn summary, the current player's score, results and leaderboard values, "Throwing Order:", settings section headers) use `MaterialTheme.brandColors.text` (`ui/theme/BrandColors.kt`, lighter red in dark theme; `textOnContainer` on default cards and the Cricket PTS tint). `BrandColorsTest` checks 4.5:1 on each surface. The current player's card border and the Cricket PTS strip's tint use `brandColors.highlight` (same red as the text: lighter in dark theme, brand red in light), so frame and text read as one red. Exceptions that stay `colorScheme.primary`: the avatar selection ring (a border, not text), and Material controls' default colours (buttons, radio buttons, text fields). Dialogs' text buttons are the exception to that: they take `dialogTextButtonColors()`, as the default label is ~2.5:1 on the dark dialog surface.
- **Navigation:** Voyager. `HomeScreen` is the entry point. Push with `navigator.pushFrom(this@XScreen, next)` and replace with `replaceFrom` (`ui/screen/NavigateFromTop.kt`), never bare `push`/`replace`: they act only while the calling screen is on top, so a double tap during the slide can't stack two screens with their own ScreenModels (two game screens would save over each other). New Game uses `replaceFrom(…, ActiveGameScreen(id))`.
- **Screens and ScreenModels:**
  - A screen's `Content()` reads repositories from `LocalXRepository.current` and builds its ScreenModel with a `Screen.rememberXScreenModel(...)` factory, which calls Voyager's `rememberScreenModel { }` (not plain `remember`, not DI). The model lives while its screen is in the navigator stack, so it survives a push on top (e.g. Settings mid-turn), and is disposed once the screen is popped or replaced.
  - Every screen overrides `key` with `uniqueScreenKey`: the ScreenModel store is keyed by it, and the default key is the class name, so a second `ActiveGameScreen` would otherwise share the first one's model.
  - `App.kt` disposes screens after the slide ends (`disposeSteps = false` plus `SlideTransition(disposeScreenAfterTransitionEnd = true)`): disposing at the pop would let the still-sliding screen build a fresh model.
  - Because models stay alive in the background, they must observe repository Flows rather than load once in `init` (only `ActiveGameScreenModel` and `AddEditPlayerScreenModel` load once, on purpose). A write requested right before the screen leaves (a save then pop, a settings toggle then Back) uses `launchToCompletion`, which a disposed scope can't cancel.
  - Screens read settings from `LocalAppSettings` (collected once in `App.kt`), not by collecting `getSettings()` themselves, so a screen coming back from Settings has the new values from its first frame.
  - ScreenModels take dependencies as constructor parameters and expose `StateFlow<XState>`.
  - The UI lives in a separate stateless composable that takes state and callbacks. It's usually named `XContent`; a few are named `XScreenContent`.
