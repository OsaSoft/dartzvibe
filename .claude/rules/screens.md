---
paths:
  - "**/ui/screen/**"
  - "**/App.kt"
  - "**/NavigateFromTop.kt"
---

# Screens and navigation

- **Navigation:** Voyager. `HomeScreen` is the entry point. Push with `navigator.pushFrom(this@XScreen, next)` and replace with `replaceFrom` (`ui/screen/NavigateFromTop.kt`), never bare `push`/`replace`: they act only while the calling screen is on top, so a double tap during the slide can't stack two screens with their own ScreenModels (two game screens would save over each other). New Game uses `replaceFrom(…, ActiveGameScreen(id))`.
- **Screens and ScreenModels:**
  - A screen's `Content()` reads repositories from `LocalXRepository.current` and builds its ScreenModel with a `Screen.rememberXScreenModel(...)` factory, which calls Voyager's `rememberScreenModel { }` (not plain `remember`, not DI). The model lives while its screen is in the navigator stack, so it survives a push on top (e.g. Settings mid-turn), and is disposed once the screen is popped or replaced.
  - Every screen overrides `key` with `uniqueScreenKey`: the ScreenModel store is keyed by it, and the default key is the class name, so a second `ActiveGameScreen` would otherwise share the first one's model.
  - `App.kt` disposes screens after the slide ends (`disposeSteps = false` plus `SlideTransition(disposeScreenAfterTransitionEnd = true)`): disposing at the pop would let the still-sliding screen build a fresh model.
  - Because models stay alive in the background, they must observe repository Flows rather than load once in `init` (only `ActiveGameScreenModel` and `AddEditPlayerScreenModel` load once, on purpose). A write requested right before the screen leaves (a save then pop, a settings toggle then Back) uses `launchToCompletion`, which a disposed scope can't cancel.
  - Screens read settings from `LocalAppSettings` (collected once in `App.kt`), not by collecting `getSettings()` themselves, so a screen coming back from Settings has the new values from its first frame.
  - ScreenModels take dependencies as constructor parameters and expose `StateFlow<XState>`.
  - The UI lives in a separate stateless composable that takes state and callbacks. It's usually named `XContent`; a few are named `XScreenContent`.
