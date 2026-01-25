# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Workflow

**Always run `./gradlew spotlessApply` before committing code or after finishing a task.**

**After completing a task, always provide a clickable list of changed files so they can be opened in the IDE.**

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

### Game Engine

`GameEngine` in `domain/game/` is immutable - all operations return new engine instances. Key methods:
- `addThrow(segment, multiplier)` - returns `(GameEngine, ThrowResult)`
- `endTurn()` - returns `(GameEngine, TurnResult)`
- `undoLastThrow()` - returns nullable GameEngine

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
