# DartzVibe

A free, open-source darts scoring app for Android and iOS. Track games, stats, and head-to-head records with friends.

Built with Kotlin Multiplatform and Compose Multiplatform.

<!-- [<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80">](https://play.google.com/store/apps/details?id=cloud.osasoft.dartzvibe) -->

## Features

### Game Modes

**Classic (Count Down)**
- 301 and 501 variants
- Count down from the target score to exactly zero
- Optional Double-In rule (must start scoring with a double)
- Double-Out rule (must finish on a double)
- Bust detection when score drops below zero or leaves an impossible finish

**Parcheesi (Count Up)**
- 301 and 501 variants
- Count up from zero to the target score
- Knockouts: landing on another player's exact score resets them to zero
- Bounce-back: overshooting the target bounces the score back down

**Cricket**
- Regular Cricket: close segments 15-20 and bullseye
- Random Cricket: 7 randomly selected segments per game
- Mark segments three times to close them, then score points on opponents' open segments
- Win by closing all segments with equal or higher points

**Roulette**
- Random target segments each round -- hit the target to score
- Rounds mode: play a fixed number of rounds, highest score wins
- Score mode: first to reach the target score wins

**Checkout Practice**
- Solo training mode for practicing checkouts
- Difficulty levels: Easy, Medium, Hard, Full range
- Focuses on finishing combinations to sharpen your game

### Scoring & Input

- **Score Input Keypad** -- traditional number grid with multiplier buttons
- **Radial Dartboard** -- interactive wheel with swipe gestures (swipe up for double, down for triple)
- **Checkout Hints** -- real-time display of optimal finish combinations
- **Undo** -- replay-safe undo for any throw
- **Miss Tracking** -- dedicated miss button for accurate statistics

### Statistics & Tracking

- 3-Dart Average and First 9 Darts Average
- Checkout percentage and best checkout
- 180s, 140+, and 100+ counts
- Games won/lost and win rate
- Parcheesi-specific: knockouts dealt, times knocked out, KO ratio

### More

- **Leaderboard** -- ranked standings sortable by win rate, average, games won, or 180s
- **Head-to-Head** -- compare stats between any two players
- **Game History** -- browse completed games with full turn-by-turn breakdowns
- **Resume Games** -- pick up in-progress games where you left off
- **Player Management** -- custom color-coded avatars
- **Multi-Leg Matches** -- configurable legs to win
- **2-4 Players** with drag-to-reorder throwing order
- **Theme Support** -- light, dark, and system-matching

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI Framework | Compose Multiplatform |
| Navigation | Voyager |
| Dependency Injection | kotlin-inject |
| Database | SQLDelight |
| Networking | Ktor Client |
| Testing | Kotest |

## Building from Source

### Prerequisites

- JDK 17+
- Android SDK 24+ (target SDK 36)
- [Android Studio](https://developer.android.com/studio) or [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- For iOS: Xcode 15+ (macOS only)

### Build & Run

```shell
# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Run tests
./gradlew :composeApp:allTests

# Format code
./gradlew spotlessApply

# Build iOS framework (macOS only)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

The debug APK will be at `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

## Project Structure

```
composeApp/src/
├── commonMain/            # Shared Kotlin code
│   ├── kotlin/.../
│   │   ├── data/          # Models & repositories
│   │   ├── di/            # Dependency injection
│   │   ├── domain/game/   # Game engine & scoring logic
│   │   ├── ui/screen/     # Screens & ViewModels
│   │   └── util/          # Shared utilities
│   └── sqldelight/        # Database schemas
├── commonTest/            # Shared tests
├── androidMain/           # Android-specific code
└── iosMain/               # iOS-specific code
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on reporting bugs, suggesting features, and submitting code.

## License

This project is licensed under the [European Union Public Licence v. 1.2](LICENSE) (EUPL-1.2).

Copyright (c) 2026 Oscar Hernandez
