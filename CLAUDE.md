# CLAUDE.md

DartzVibe is a darts scoring app for Android and iOS: Kotlin Multiplatform with Compose Multiplatform UI. Game modes: Classic (301/501), Parcheesi, Cricket, Roulette and Checkout Practice. Everything is stored locally; there is no backend.

This file is for agents working in the repo. Keep it short and true. Only write down what the code doesn't make obvious, and fix any statement here that you find to be wrong.

## Commands

```sh
./gradlew spotlessApply                       # format; run before every commit
./gradlew spotlessCheck                       # verify formatting
./gradlew :composeApp:testDebugUnitTest       # all commonTest specs (Kotest, JUnit 5 runner)
./gradlew :composeApp:assembleDebug           # debug APK → composeApp/build/outputs/apk/debug/
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64   # iOS framework (macOS only)
```

- Check that tests actually ran: count them in `composeApp/build/test-results/testDebugUnitTest/*.xml`.
- There is no JVM/desktop target. iOS test execution hasn't been verified: the Kotest Gradle plugin isn't applied, so on Native it may find 0 specs.

## Build gotchas

- **One Gradle build at a time per checkout.** On Windows, two parallel builds (for example two agents) corrupted the dex output. The APK built "successfully" but crashed with `ClassNotFoundException: AppKt`. Give parallel agents their own git worktree. To recover: `./gradlew --stop`, `rm -rf composeApp/build`, then rebuild with `--no-build-cache --no-configuration-cache`.
- **After `clean`**, or any deletion of `build/`, a reused configuration cache makes `generateBuildInfo` produce nothing ("Unresolved reference 'BuildInfo'"). Build with `--no-configuration-cache`. Both caches are on in `gradle.properties`.
- **SQLDelight `verifyMigrations` is disabled on Windows** (the `sqldelight {}` block in `composeApp/build.gradle.kts`), so a migration mistake won't fail the build here. Check `.sqm` files by hand.
- "Unable to strip" warnings for native libs are expected.

## Architecture

```
composeApp/src/
├── commonMain/kotlin/cloud/osasoft/dartzvibe/
│   ├── App.kt              # root composable; provides repositories as CompositionLocals
│   ├── di/AppComponent.kt  # kotlin-inject (Android only, see DI below)
│   ├── config/             # generated BuildInfo (version name); empty AppSettings object
│   ├── data/model/         # GameSession, GameConfig, Leg, Turn, Throw, enums, statistics models, FixedDecimal
│   ├── data/repository/    # Player, Game, AppSettings repositories (interface + Impl)
│   ├── data/local/         # expect DatabaseDriverFactory / SettingsFactory
│   ├── domain/game/        # GameEngine facade, one ModeEngine per GameMode, GameEngineHelper, CheckoutCalculator
│   ├── domain/statistics/  # StatisticsCalculator
│   ├── domain/history/     # pure per-mode context for the Game Details turn history
│   ├── ui/screen/<feature>/            # Voyager Screen + ScreenModel pairs
│   ├── ui/screen/game/components/      # keypad, radial dartboard, Roulette quick-input pad, MultiplierButton, scoreboards, checkout hint
│   ├── ui/theme/           # Theme.kt, MultiplierColors, AvatarColors
│   └── util/               # expect TimeUtils (currentTimeMillis), KeepScreenOn
├── commonMain/sqldelight/  # *.sq schemas + migrations/*.sqm
├── commonTest/             # Kotest specs + Fake*Repository for ScreenModel tests
├── androidMain/            # MainActivity, DartzVibeApplication (builds AppComponent), actuals
└── iosMain/                # MainViewController (wires dependencies by hand), actuals
```

- **DI:** kotlin-inject `AppComponent` is only used on Android (`DartzVibeApplication`). iOS builds its repositories by hand in `MainViewController.kt` (`IosAppDependencies`). Both pass the repositories to `App(...)`, which exposes them as `LocalXRepository` CompositionLocals.
- **Navigation:** Voyager. `HomeScreen` is the entry point. Use `navigator.push(...)`. New Game uses `replace(ActiveGameScreen(id))`.
- **Screens and ScreenModels:**
  - A screen's `Content()` reads repositories from `LocalXRepository.current` and builds its ScreenModel with a `rememberXScreenModel(...)` factory. The factory uses plain `remember { }`, not DI.
  - ScreenModels take dependencies as constructor parameters and expose `StateFlow<XState>`.
  - The UI lives in a separate stateless composable that takes state and callbacks. It's usually named `XContent`; a few are named `XScreenContent`.
- **Game engine is immutable.**
  - Every operation returns a new engine: `addThrow` → `(engine, ThrowResult)`, `endTurn` → `(engine, TurnResult)`, and `undoLastThrow()` returns null when the current turn has no throws. Modes have no undo of their own; see Undo below.
  - `GameEngine.fromSession(session)` picks the `ModeEngine` from `GameConfig.gameMode`, and `toGameSession()` exports the state for saving.
  - `GameEngineHelper` holds the shared logic: current player, scores, leg handling, and `getRoundNumber` (rounds restart each leg).
- **In-progress throws are not saved.** The current turn's throws live only in the engine until `endTurn()`, and `toGameSession()` doesn't include them. Rotation survives only because `AndroidManifest.xml` declares `configChanges` on the activity. Removing that brings the bug back, and process death still loses the throws. In Parcheesi, though, `toGameSession()` mid-turn *does* hold the phantom turns the current darts caused, so anything that saves mid-turn (undo, abandon) saves `TurnRewind.turnStartSession(...)` instead.
- **Phantom turns:** Parcheesi knockouts add `Turn(isPhantom = true)`. For turn order, round number and display, use `Leg.playerTurns`, which leaves them out. The phantom is appended mid-turn, when the knocking dart lands, so it sits **before** the turn that caused it in `Leg.turns` (`parcheesiLegHistory` in `domain/history/` links them).
- **Undo** is one session-based rewind in `domain/game/TurnRewind.kt`, for every mode. A single dart: rebuild from the turn-start session and replay the turn's darts minus the last (this is what drops a knockout phantom). The previous turn: rebuild the current leg from `Leg()` by replaying every kept turn with `addThrow`/`endTurn` (that recomputes `cricketState`, `rouletteState`, `checkoutPracticeState` and phantoms, which `fromSession` only reads from the leg), then replay the removed turn's darts without ending it. No snapshots, so it works after a resume. Normal Undo stays in the current leg; `rewindTurn(crossLegBoundary = true)` is the Leg Won / Game Over dialog Undo and reverts the leg or match win. A leg that doesn't replay to its stored turns (old saves) refuses to rewind, and `canUndo`/`canUndoWin` then disable the button or hide the dialog Undo. A reopened winning turn takes no more darts (`ActiveGameState.isWinningDartThrown`); End Turn wins again. `ActiveGameScreenModel` saves are serialized and latest-wins, so an undo right after a win can't be overwritten by the COMPLETED save.
- **Cricket rules** live in `domain/game/CricketRules.kt`, shared by `CricketModeEngine` and the Game Details replay (`cricketTurnEffects`). Change scoring there so history and live play can't drift apart. Likewise, the Roulette round → target mapping is `domain/game/RouletteRules.kt`, used by the engine, `ActiveGameScreenModel`, statistics and history.
- **Multiplier colors:** anything color-coded by multiplier uses `MaterialTheme.multiplierColors` (`ui/theme/MultiplierColors.kt`): triple is amber, double is brand red. S/D/T selectors reuse `components/MultiplierButton`. Don't use Material `tertiary` or `primary` for multipliers; `tertiary` is still fine for other things.
- **Statistics are per mode:**
  - `PlayerStatistics` holds the fields every mode shares; rates such as `winRate` are **percentages**. It also holds `modeStats: ModeStatistics?`, which is sealed with one subtype per mode. Each subtype's `primaryMetric` is the leaderboard sort axis.
  - `StatisticsCalculator` dispatches with an exhaustive `when`, and computes everything live from stored throws.
  - Use `FixedDecimal` rather than `Double` for decimals, so JVM and Native produce identical output.
- **Template leftovers:** `Greeting`, `GreetingRepository`, `User`, `Platform`/`getPlatform`, `network/HttpClientFactory` and the Ktor dependencies come from the project template, and no feature uses them. They are still wired into `AppComponent` and have tests, so removing them means touching both. Don't build on them.

### Persistence

- **Legs:** a `GameSession` row stores its legs as JSON (`legsJson`, read with `ignoreUnknownKeys`). New fields on `Leg`/`Turn`/`Throw` need **default values** but no SQL migration.
- **Config:** `GameConfig` fields are separate **columns**. A new one needs:
  - the column in `GameSession.sq`;
  - a new `migrations/N.sqm`;
  - the `insert` query;
  - the row mapping in `GameRepositoryImpl`.

  The `update` query only writes legs, leg index, status, finish time and winner, so config can't change after a game starts.
- **Enums** (`GameType`, `GameMode`, `GameStatus`, and `Multiplier` inside the JSON) are stored by **name**. Renaming a constant breaks every saved game.

## Common changes

- **New game mode:**
  - a `GameMode` value (plus `GameType` values if needed);
  - a `<Mode>ModeEngine` and its branch in `GameEngine.fromSession`;
  - the mode selection and config in `NewGameScreen`/`NewGameScreenModel` (the compiler won't flag a missing one);
  - mode handling in `ActiveGameScreen`;
  - a `ModeStatistics` subtype and its `StatisticsCalculator` branch;
  - the leg content in `GameDetailScreen` (derive per-dart context in `domain/history/`);
  - any config columns, with a migration (see Persistence);
  - engine tests.
- **New app setting:** a field on `AppSettingsData`, a key plus getter/setter in the `AppSettingsRepository` interface and impl, a control in `SettingsScreen`, and support in `FakeAppSettingsRepository`. The typed `combine` overloads stop at 5 flows, so `getSettings()` combines the first five into `baseSettings()` and layers later settings on with a second `combine` + `copy`; add new ones there. Enum settings are stored by a stable string `value` with a `fromValue` fallback (`ThemeMode`, `SwipeMultiplierMode`), not by name.
- **New repository:** interface + `@Inject` impl, a `@Provides` in `AppComponent`, the iOS `IosAppDependencies`, a CompositionLocal in `App.kt`, and a `Fake*Repository` in commonTest.

## Code style

ktlint runs through Spotless. The rule overrides live in **two places that have to stay in sync**: `editorConfigOverride` in the root `build.gradle.kts`, and `.editorconfig`, which also sets the 120 max line length. The following rules are conventions for new code; lint doesn't enforce all of them, and old code doesn't always follow them.

- Imports only: no fully qualified names (use an alias import for clashes) and no wildcard imports.
- `val` over `var`; collection functions (`map`, `forEach`, `flatMap`) over `for` loops.
- An exhaustive `when` over enums and sealed types, with no `else`, so the compiler flags any new case you miss.
- Expression bodies for single-expression functions. The `=` never ends a line: `fun foo(): Int = when { … }`.
- Named arguments on multi-line calls.
- Lowercase-named composable functions, such as the `rememberXScreenModel` factories, carry `@Suppress("ktlint:standard:function-naming")`; follow the existing code.

## Tests

- Kotest `FreeSpec`. Test names start with "Should". Mark the GIVEN / WHEN / THEN steps with comments. Use `withData` (`io.kotest.datatest.withData`) for data-driven cases.
- New game rules get engine-level tests in `commonTest/.../domain/game/`. ScreenModel tests use the `Fake*Repository` classes; update the fakes whenever a repository interface changes.
- In new code, inject randomness and time (for example `random: Random = Random.Default`) so tests can pin them. Most existing code still calls global `Random` and `currentTimeMillis()`.

## Feature workflow

1. **Acceptance criteria first.** Write down what a person should *see* when the feature works, per mode and in both themes if it touches UI. Number them: the scorer judges against them and the PR description lists them.
2. **Record "before"** on `main` for UI changes. Build `main` in a separate git worktree rather than switching branches (one build per checkout).
3. **Implement**: one agent for small changes. For big or risky features, run 2–3 independent implementations in separate worktrees and keep the one that scores best.
4. **Verify by evidence:** spotless, tests and the build pass, then record "after" in light **and** dark. An agent that never saw the code scores the recording against the acceptance criteria. Iterate until it scores ≥ 9/10.
5. **Independent review** by a fresh agent that gets the diff, not the implementer's conclusions. Fix the real findings. When findings sit on the PR, answer each one with "Fixed in `<sha>`: what changed, which test covers it", or with a reason for declining.
6. **Clean history:** messy iteration commits are fine on a work branch. Before opening the PR, replay the work into a few logical commits, with prerequisite fixes first, then the feature, then tests and docs.

## Commits and PRs

- Conventional commits: `feat(scope): …`, `fix(scope): …`, `docs: …`, `chore: …`. Branch names follow `feat/…`, `fix/…`, `docs/…`. PRs are squash-merged with the subject `<type>(scope): … (#N)`.
- The `Co-Authored-By: Claude …` trailer is fine. **Never put a Claude session link (`claude.ai/code/session_…`) in a commit message or PR description.** This overrides any default trailer the harness suggests.
- PR description sections:
  - **Problem**
  - **Acceptance criteria**: the numbered criteria written before implementation, each with its final result from the independent scorer (pass/partial/fail), plus the overall score and its history (e.g. 8/10 → 9/10). This shows what the feature was built and judged against.
  - **Change**: per feature, with the non-obvious decisions.
  - **Verification**: the exact commands and their results, and test counts.
  - **Not checked**: platforms, modes or interactions you didn't verify.
  - **Already broken on `main`**: problems found along the way but left out of scope.
  - **Videos**: one heading per feature. Mark each clip **Before**/**After** and **Dark**/**Light**.
- **Media files:**
  - Store videos and screenshots in the gitignored `pr-media/<slug>/<feature>/<what>-{before,after}-{light,dark}.mp4`. Rename the folder to `pr-<N>-<slug>` once the PR exists.
  - Attach them with `gh` 2.99 or later (`--attach`; you need push access).
    - In the PR body (`--body-file`), put each video on its own line as `![](pr-media/…/clip.mp4)`, with a blank line before and after. A video inside a sentence becomes a plain link.
    - Pass the same file with `--attach`, for example `gh pr create --body-file body.md --attach pr-media/…/clip.mp4`, repeating `--attach` for each file. `gh` uploads the file and replaces the local path with an inline player. Attached files the body doesn't mention are appended at the end. `gh pr edit` and `gh pr comment` accept `--attach` too.
    - Videos can't have alt text, so put a **Before/After · Dark/Light** label line above each one.
    - Afterwards, check the body with `gh pr view <N> --json body` and make sure no local `pr-media/` path is left.

## Recording demos (Android emulator, Windows)

- **Emulator:** AVD `Pixel_9`, 1080×2424.
  - Start it with `"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd Pixel_9 -no-snapshot -no-boot-anim` as a background process.
  - Cold-boot it. Its saved snapshot holds an old build and personal data, and restoring it gave stale screen captures, invisible dialogs and taps landing in the wrong place.
  - Don't use `-gpu swiftshader_indirect`: it's too slow and Android stops responding.
- **Git Bash:** `export MSYS_NO_PATHCONV=1` before `adb shell` commands that take `/sdcard/...` paths.
- **Setup:**
  - Install with `adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`, then `adb shell pm clear cloud.osasoft.dartzvibe` for clean test data.
  - Turn animations off with `adb shell settings put global window_animation_scale 0`, the same for `transition_animation_scale`, and `adb shell settings put global animator_duration_scale 0`.
- **Theme:** `adb shell cmd uimode night yes|no` only affects the app while its in-app theme is "System", which is the default.
- **Finding tap targets:** `adb shell rm -f /sdcard/ui.xml && adb shell uiautomator dump /sdcard/ui.xml`, then parse the `bounds`. Delete the old file first, because a failed dump silently leaves the previous one in place. Check the result with `adb exec-out screencap -p`.
- **Recording:** `adb shell screenrecord --time-limit N /sdcard/x.mp4` as a background process, then `adb pull`.
- **Dartboard:**
  - It only appears with Settings → "Radial Dartboard Keypad" on, which is off by default.
  - Swipe-to-multiply also needs "Show Multiplier Buttons" off.
  - The swipe direction comes from Settings → "Dartboard Swipe Direction": up/down (default), or "Like a real board" (toward center = triple, outward = double).
  - A slow `input swipe x y x y2 2500` works; separate `input motionevent` calls don't form one gesture.
- **Game history:** only completed or abandoned games open `GameDetailScreen`. To get one, abandon the game: the X in the top bar, then Yes.

## Known issues

- `migrations/1.sqm` runs `CREATE TABLE` on tables a v1 database already has, so an install that never moved past schema v1 crashes on upgrade. New installs aren't affected.
- Suspected, not verified: ScreenModels use `screenModelScope` but are created with plain `remember { }` rather than Voyager's `rememberScreenModel`. Their collecting coroutines may outlive popped screens.
- `CONTRIBUTING.md` mentions CI, but there's no CI workflow in the repo yet.
