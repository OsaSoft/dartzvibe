# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Workflow

**Always run `./gradlew spotlessApply` before committing code or after finishing a task.**

**After completing a task, always provide a clickable list of changed files so they can be opened in the IDE.**

## Documentation Maintenance

**After every task that changes the project structure, update the documentation:**

- **Added/removed/renamed a screen?** → Update the Screen Inventory table in this file
- **Added/removed/renamed a data model or changed its fields?** → Update the Data Model Quick Reference in this file
- **Added/renamed a file that changes the File Lookup Table?** → Update it
- **Added a new game mode or engine?** → Update the Game Engine section and task recipes
- **Changed DI wiring or added a repository?** → Update the DI & Repository section
- **Discovered a new pattern, gotcha, or build quirk?** → Update MEMORY.md
- **Added a new common task recipe?** → Add it to the Common Task Recipes section

**Keep CLAUDE.md accurate and current — it is the primary source of truth that prevents redundant codebase exploration.**

**Keep MEMORY.md under 200 lines** (it gets truncated beyond that). Move detailed content to separate files in the memory directory and link from MEMORY.md.

## Build Commands

```shell
# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Run all tests
./gradlew :composeApp:allTests

# Run Android unit tests only
./gradlew :composeApp:testDebugUnitTest

# Format code with Spotless/ktlint
./gradlew spotlessApply

# Check code formatting
./gradlew spotlessCheck

# Build iOS framework (macOS only)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Architecture

DartzVibe is a Kotlin Multiplatform (KMP) darts scoring app targeting Android and iOS with Compose Multiplatform UI.

### Project Structure

```
composeApp/src/
├── commonMain/kotlin/cloud/osasoft/dartzvibe/
│   ├── config/          # App settings
│   ├── data/
│   │   ├── local/       # SQLDelight database (*.sq files in sqldelight/)
│   │   ├── model/       # Data classes (GameSession, Player, etc.)
│   │   └── repository/  # Data access layer
│   ├── di/              # kotlin-inject DI component
│   ├── domain/game/     # Core game logic (GameEngine)
│   ├── network/         # Ktor HTTP client setup
│   ├── ui/
│   │   ├── screen/      # Voyager screens and ScreenModels
│   │   └── theme/       # Compose theme
│   └── util/            # Shared utilities
├── commonTest/          # Shared tests (Kotest)
├── androidMain/         # Android-specific (OkHttp, SQLDelight Android driver)
└── iosMain/             # iOS-specific (Darwin client, Native driver)
```

### Key Libraries

- **DI**: kotlin-inject (compile-time, uses `@Component`, `@Provides`, `@Inject`)
- **Navigation**: Voyager (screens + ScreenModels)
- **Database**: SQLDelight (type-safe SQL, schemas in `*.sq` files)
- **Networking**: Ktor Client
- **Testing**: Kotest with FreeSpec style (`"description" - { "test" { } }`)

## Screen Inventory

All screens live under `ui/screen/`. Path prefix: `composeApp/src/commonMain/kotlin/cloud/osasoft/dartzvibe/ui/screen/`

| Screen | Screen File | ScreenModel File | Navigates To |
|--------|------------|-----------------|-------------|
| HomeScreen | `home/HomeScreen.kt` | `home/HomeScreenModel.kt` | NewGame, ActiveGame, GamesList, Statistics, HeadToHead, Leaderboard, PlayerList, Settings |
| GamesListScreen | `home/GamesListScreen.kt` | (inline in same file) | ActiveGame, GameDetail |
| NewGameScreen | `game/NewGameScreen.kt` | `game/NewGameScreenModel.kt` | ActiveGame (replace) |
| ActiveGameScreen | `game/ActiveGameScreen.kt` | `game/ActiveGameScreenModel.kt` | Settings |
| PlayerListScreen | `players/PlayerListScreen.kt` | `players/PlayerListScreenModel.kt` | AddEditPlayer, NewGame |
| AddEditPlayerScreen | `players/AddEditPlayerScreen.kt` | `players/AddEditPlayerScreenModel.kt` | (pops on save) |
| GameDetailScreen | `gamedetail/GameDetailScreen.kt` | `gamedetail/GameDetailScreenModel.kt` | (read-only) |
| LeaderboardScreen | `leaderboard/LeaderboardScreen.kt` | `leaderboard/LeaderboardScreenModel.kt` | (read-only) |
| StatisticsScreen | `statistics/StatisticsScreen.kt` | `statistics/StatisticsScreenModel.kt` | (read-only) |
| HeadToHeadScreen | `statistics/HeadToHeadScreen.kt` | `statistics/HeadToHeadScreenModel.kt` | (read-only) |
| SettingsScreen | `settings/SettingsScreen.kt` | `settings/SettingsScreenModel.kt` | (read-only) |

### Navigation Flow

```
HomeScreen (entry point)
├── NewGameScreen → ActiveGameScreen (replace)
├── GamesListScreen → ActiveGameScreen | GameDetailScreen
├── PlayerListScreen → AddEditPlayerScreen | NewGameScreen
├── StatisticsScreen
├── HeadToHeadScreen
├── LeaderboardScreen
└── SettingsScreen
ActiveGameScreen → SettingsScreen
```

## Data Model Quick Reference

Models in `data/model/`: `GameModels.kt` (GameSession, GameConfig, Leg, Turn, Throw, enums), `StatisticsModels.kt` (PlayerStatistics, HeadToHeadStatistics), `Player.kt`, `AppSettingsData.kt`, `ThemeMode.kt`, `FixedDecimal.kt`. Read these files directly for field details.

## Game Engine

`GameEngine` in `domain/game/` is immutable — all operations return new engine instances. Read `GameEngine.kt` for the full API (methods, `ThrowResult`/`TurnResult` sealed classes).

### Files

| File | Contains | Purpose |
|------|---------|---------|
| `GameEngine.kt` | `GameEngine`, `ThrowResult` (sealed), `TurnResult` (sealed) | Public API facade |
| `ModeEngine.kt` | `ModeEngine` (sealed interface) | Base for mode-specific engines |
| `ClassicModeEngine.kt` | `ClassicModeEngine` | Classic 501/301 countdown |
| `ParcheesiModeEngine.kt` | `ParcheesiModeEngine` | Count-up with knockouts |
| `CricketModeEngine.kt` | `CricketModeEngine` | Cricket segment marking |
| `CheckoutPracticeModeEngine.kt` | `CheckoutPracticeModeEngine` | Solo checkout training |
| `CheckoutCalculator.kt` | `CheckoutCalculator` (object), `CheckoutPath` | Checkout hints/paths |
| `GameEngineHelper.kt` | `GameEngineHelper` (object) | Shared utilities (current player, scores, leg handling) |

## DI & Repository

Repositories (all in `data/repository/`):
- `PlayerRepository.kt` — player CRUD, `getAllPlayers(): Flow`
- `GameRepository.kt` — game session CRUD, `getAllGameSessions(): Flow`
- `AppSettingsRepository.kt` — app settings, `getSettings(): Flow<AppSettingsData>`

DI wiring: `di/AppComponent.kt`. CompositionLocals defined in `App.kt`: `LocalPlayerRepository`, `LocalGameRepository`, `LocalAppSettingsRepository`.

**Access**: In `Screen.Content()` use `LocalXRepository.current` → pass to `rememberXScreenModel()`. In ScreenModels, use constructor injection.

## File Lookup Table

| I want to... | Go to |
|--------------|-------|
| Add/modify a screen | `ui/screen/<feature>/` (see Screen Inventory) |
| Change game scoring rules | `domain/game/<Mode>ModeEngine.kt` |
| Add a game mode | `domain/game/` + `data/model/GameModels.kt` (enums) |
| Modify checkout hints | `domain/game/CheckoutCalculator.kt` |
| Change player data | `data/model/Player.kt` + `data/repository/PlayerRepository.kt` + `Player.sq` |
| Change game session persistence | `data/repository/GameRepository.kt` + `GameSession.sq` |
| Change app settings | `data/model/AppSettingsData.kt` + `data/repository/AppSettingsRepository.kt` |
| Change theme/colors | `ui/theme/` |
| Add platform-specific code | `commonMain` (expect) + `androidMain`/`iosMain` (actual) |
| Modify DI wiring | `di/AppComponent.kt` |
| Modify HTTP client | `network/HttpClientFactory.kt` |
| Change database schema | `commonMain/sqldelight/cloud/osasoft/dartzvibe/data/local/*.sq` |
| Add a utility function | `util/` |
| Modify app entry point | `App.kt` (composable root with CompositionLocals) |
| Android app initialization | `androidMain/.../DartzVibeApplication.kt` + `MainActivity.kt` |
| iOS app initialization | `iosMain/.../MainViewController.kt` |

## Common Task Recipes

### Add a new Screen

1. Create `ui/screen/<feature>/<Name>Screen.kt` with `class <Name>Screen : Screen`
2. Create `ui/screen/<feature>/<Name>ScreenModel.kt` with `class <Name>ScreenModel(...) : ScreenModel`
3. Add state class (e.g. `data class <Name>ScreenState(...)`) in the ScreenModel file
4. Add `@Composable fun remember<Name>ScreenModel(...)` factory in the Screen file
5. In `Content()`, get repos from `LocalXRepository.current`, create model via factory
6. Add navigation from the calling screen via `navigator.push(<Name>Screen())`
7. **Update Screen Inventory table in this file**

### Add a new game mode

1. Add enum value to `GameMode` in `data/model/GameModels.kt`
2. Add supported `GameType` entries if needed
3. Create `domain/game/<Mode>ModeEngine.kt` implementing `ModeEngine`
4. Add branch in `GameEngine.fromSession()` to instantiate the new engine
5. Add UI support in `NewGameScreen` for selecting the mode
6. Add handling in `ActiveGameScreen`/`ActiveGameScreenModel` for mode-specific UI
7. **Update this file's Game Engine and Data Model sections**

### Add a statistic

1. Add field to `PlayerStatistics` in `data/model/StatisticsModels.kt`
2. Compute it in the statistics calculation (in `StatisticsScreenModel` or `LeaderboardScreenModel`)
3. Display it in `StatisticsScreen` or `LeaderboardScreen` UI
4. **Update Data Model Quick Reference in this file**

### Add a new app setting

1. Add field to `AppSettingsData` in `data/model/AppSettingsData.kt`
2. Add setter method to `AppSettingsRepository` interface and impl
3. Add key constant and read/write in `AppSettingsRepositoryImpl`
4. Add UI toggle in `SettingsScreen`
5. **Update Data Model Quick Reference in this file**

### Modify scoring rules

1. Identify the mode engine: `ClassicModeEngine`, `ParcheesiModeEngine`, `CricketModeEngine`, or `CheckoutPracticeModeEngine`
2. Edit `addThrow()` for per-throw rules or `endTurn()` for turn-end rules
3. Add tests in `commonTest/` using Kotest FreeSpec
4. Remember: engines are immutable — return new instances

### Add a new repository

1. Create interface + impl in `data/repository/`
2. Add `@Inject` to the impl class
3. Add `@Provides` binding in `di/AppComponent.kt`
4. If needed from composables: add `staticCompositionLocalOf` in `App.kt` and provide it in `CompositionLocalProvider`
5. **Update DI & Repository Quick Map in this file**

## Testing Guidelines

- Use Kotest `FreeSpec` as the base class
- Test names should start with "Should"
- Structure tests with GIVEN, WHEN, THEN comments
- Use `withData` for data-driven tests

```kotlin
class GameEngineTest : FreeSpec({
    "Checkout" - {
        "Should detect checkout with double" {
            // GIVEN a game session with score at 40
            val engine = createEngineWithScore(40)
            // AND player 1 is on turn

            // WHEN player hits D20
            val (newEngine, result) = engine.addThrow(20, Multiplier.DOUBLE)

            // THEN checkout is detected
            result.shouldBeInstanceOf<ThrowResult.Checkout>()
            // AND player 1 is the winner
            (result as ThrowResult.Checkout).winnerId shouldBe playerId1
        }
    }
})
```

Fake repositories exist in `commonTest/` for testing ScreenModels.

## Code Style

- Never use fully qualified names; use imports (alias imports for name clashes)
- Never use wildcard imports
- Use expression body syntax for single-expression functions
- Use `val` over `var` wherever possible
- Use named arguments for multi-line function calls
- Prefer exhaustive `when` over `else` branch (compiler catches missing enum values)
- For expression body syntax, `=` must NOT trail at the end of a line:
  ```kotlin
  // Correct - = on same line as expression
  fun foo(): Int = when { ... }

  // Correct - long args, = with expression on last line
  fun foo(
      param1: String,
      param2: Int,
  ): Int = when { ... }

  // Incorrect - = trailing at end of line
  fun foo(): Int =
      when { ... }
  ```
- Prefer Kotlin collection functions over `for` loops:
  ```kotlin
  // Preferred
  items.forEach { item -> process(item) }
  (1..20).map { segment -> createThrow(segment) }
  items.flatMap { item -> transform(item) }

  // Avoid
  for (item in items) { process(item) }
  ```
