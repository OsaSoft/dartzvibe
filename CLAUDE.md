# CLAUDE.md

DartzVibe is a darts scoring app for Android and iOS: Kotlin Multiplatform with Compose Multiplatform UI. Game modes: Classic (301/501), Parcheesi, Cricket, Roulette and Checkout Practice. Everything is stored locally; there is no backend.

This file is for agents working in the repo. Keep it short and true. Only write down what the code doesn't make obvious, and fix any statement here that you find to be wrong.
The same goes for the path-scoped rules in `.claude/rules/` (they load automatically when you work on matching files) and the process docs in `docs/` (see the index below).

## Commands

```sh
./gradlew spotlessApply                       # format; run before every commit
./gradlew spotlessCheck                       # verify formatting
./gradlew :composeApp:testDebugUnitTest       # all commonTest + androidUnitTest specs (Kotest, JUnit 5 runner)
./gradlew :composeApp:assembleDebug           # debug APK → composeApp/build/outputs/apk/debug/
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64   # iOS framework (macOS only)
```

- Running one spec and counting the tests that ran: see `.claude/rules/testing.md`.
- There is no JVM/desktop target. iOS test execution hasn't been verified: the Kotest Gradle plugin isn't applied, so on Native it may find 0 specs.

## Build gotchas

- **One Gradle build at a time per checkout.** On Windows, two parallel builds (for example two agents) corrupted the dex output. The APK built "successfully" but crashed with `ClassNotFoundException: AppKt`. Give parallel agents their own git worktree. To recover: `./gradlew --stop`, `rm -rf composeApp/build`, then rebuild with `--no-build-cache --no-configuration-cache`.
- **After `clean`**, or any deletion of `build/`, a reused configuration cache makes `generateBuildInfo` produce nothing ("Unresolved reference 'BuildInfo'"). Build with `--no-configuration-cache`. Both caches are on in `gradle.properties`.
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
│   ├── ui/screen/game/components/      # keypad, radial dartboard, Cricket pad, Roulette quick-input pad, MultiplierButton, scoreboards, checkout hint
│   ├── ui/theme/           # Theme.kt, MultiplierColors, AvatarColors
│   └── util/               # expect TimeUtils (currentTimeMillis), KeepScreenOn
├── commonMain/sqldelight/  # *.sq schemas + migrations/*.sqm
├── commonTest/             # Kotest specs + Fake*Repository for ScreenModel tests
├── androidUnitTest/        # JVM-only specs: DatabaseMigrationTest (real SQLite via JdbcSqliteDriver)
├── androidMain/            # MainActivity, DartzVibeApplication (builds AppComponent), actuals
└── iosMain/                # MainViewController (wires dependencies by hand), actuals
```

- **DI:** kotlin-inject `AppComponent` is only used on Android (`DartzVibeApplication`). iOS builds its repositories by hand in `MainViewController.kt` (`IosAppDependencies`). Both pass the repositories to `App(...)`, which exposes them as `LocalXRepository` CompositionLocals.
- **Template leftovers:** `Greeting`, `GreetingRepository`, `User`, `Platform`/`getPlatform`, `network/HttpClientFactory` and the Ktor dependencies come from the project template, and no feature uses them. They are still wired into `AppComponent` and have tests, so removing them means touching both. Don't build on them.

## Docs and rules index

Path-scoped rules (`.claude/rules/`, loaded when you touch matching files; read one by hand if your change needs it):

- `game-engine.md`: immutable engine, unsaved in-progress throws, phantom turns, Undo (`TurnRewind`), Cricket/Roulette rules. `domain/game/`, `domain/history/`.
- `game-modes.md`: checklist for adding a game mode. Mode engines, `GameModels.kt`, New Game.
- `game-screen.md`: the input never moves mid-turn, the checkout/knockout slot, Cricket pad, swipe input on keys, multiplier colours, win dialogs. `ui/screen/game/`.
- `screens.md`: Voyager navigation, ScreenModel lifecycle, `uniqueScreenKey`, `LocalAppSettings`, `launchToCompletion`. `ui/screen/`, `App.kt`.
- `persistence.md`: legs JSON, config columns and migrations, enums by name, adding a repository. `sqldelight/`, `data/`, `di/`.
- `settings.md`: adding an app setting, enum settings by stable value. `AppSettings*`, `ui/screen/settings/`.
- `statistics.md`: per-mode statistics, percentages, `FixedDecimal`. `domain/statistics/`, statistics models.
- `testing.md`: Kotest conventions, fakes, the ScreenModel test scope quirk, running one spec, counting tests. `commonTest/`.
- `kotlin-style.md`: code style and where the ktlint overrides live. `*.kt`, `*.kts`, `.editorconfig`.

Docs and skills:

- `docs/feature-workflow.md`: the feature workflow (acceptance criteria, before/after recordings, scoring, review), PR description sections, attaching media, and prompt templates for implementer, scorer and reviewer agents. Read it before starting a feature or opening a PR.
- `.claude/skills/record-demo/`: running the Android emulator (Windows, Linux, small phone), recording clips, finding tap targets, and helper scripts. Use it to record a demo or check UI on a device.
- `docs/privacy-policy.md`: the app's privacy policy (not agent documentation).

## Commits and PRs

- Conventional commits: `feat(scope): …`, `fix(scope): …`, `docs: …`, `chore: …`. Branch names follow `feat/…`, `fix/…`, `docs/…`. PRs are squash-merged with the subject `<type>(scope): … (#N)`.
- The `Co-Authored-By: Claude …` trailer is fine. **Never put a Claude session link (`claude.ai/code/session_…`) in a commit message or PR description.** This overrides any default trailer the harness suggests.
- PR description sections, media attachments and clean history: `docs/feature-workflow.md`.

## Known issues

- `CONTRIBUTING.md` mentions CI, but there's no CI workflow in the repo yet.
