# DartzVibe

A cross-platform darts scoring app built with Kotlin Multiplatform and Compose Multiplatform, targeting Android and iOS.

## Features

### Game Modes

**Classic (Count Down)**
- 301 and 501 variants
- Players count down from the target score to exactly zero
- Optional Double-In rule (must start scoring with a double)
- Double-Out rule (must finish on a double)
- Bust detection when score drops below zero or leaves an impossible finish

**Parcheesi (Count Up)**
- 301 and 501 variants
- Players count up from zero to the target score
- Knockouts: landing on another player's exact score resets them to zero
- Bounce-back: overshooting the target bounces the score back down
- Configurable legs to win (1, 3, 5, etc.)

**Cricket**
- Regular Cricket: close segments 15-20 and bullseye
- Random Cricket: 7 randomly selected segments per game
- Mark segments three times to close them, then score points on opponents' open segments
- Win by closing all segments with equal or higher points

### Scoring & Input

- **Score Input Keypad** - traditional number grid with multiplier buttons
- **Radial Dartboard** - interactive wheel with swipe gestures (swipe up for double, down for triple)
- **Checkout Hints** - real-time display of optimal finish combinations
- **Undo** - replay-safe undo for any throw, including Cricket state recalculation
- **Miss Tracking** - dedicated miss button for accurate statistics

### Statistics & Tracking

- 3-Dart Average
- First 9 Darts Average
- Checkout Percentage
- Best Checkout (linked to the game it occurred in)
- 180s count with expandable game list
- 140+ and 100+ high score counts
- Highest single-turn score
- Games won/lost record and win rate
- Legs won/played
- Parcheesi-specific: knockouts dealt, times knocked out, KO ratio

### Additional Features

- **Leaderboard** - ranked player standings sortable by win rate, 3-dart average, games won, or 180s
- **Head-to-Head** - compare stats and win/loss records between any two players
- **Game History** - browse completed games with full turn-by-turn breakdowns
- **Resume Games** - pick up in-progress games where you left off
- **Player Management** - create profiles with custom color-coded avatars
- **Multi-Leg Matches** - configurable number of legs to win
- **Theme Support** - light, dark, and system-matching themes
- **2-4 Players** - support for up to four players per game with drag-to-reorder throwing order

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.3 |
| UI Framework | Compose Multiplatform |
| Navigation | Voyager |
| Dependency Injection | kotlin-inject |
| Database | SQLDelight |
| Networking | Ktor Client |
| Testing | Kotest |

## Prerequisites

### Android Development
- [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended) or [IntelliJ IDEA](https://www.jetbrains.com/idea/) with Android plugin
- JDK 17+
- Android SDK 24+ (target SDK 36)

### iOS Development (macOS only)
- [Xcode](https://developer.apple.com/xcode/) 15+
- [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio (optional)

## Running the App

### Android

#### Option 1: Android Studio (Recommended)
1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Select the `composeApp` run configuration
4. Choose an emulator or connected device
5. Click **Run**

#### Option 2: IntelliJ IDEA
1. Install [IntelliJ IDEA](https://www.jetbrains.com/idea/) (Ultimate or Community Edition)
2. Install the **Android** plugin (bundled with Ultimate, available in Community)
3. Configure an Android SDK in **File > Project Structure > SDKs**
4. Open the project and wait for Gradle sync
5. Create a run configuration for `composeApp`
6. Select an emulator or connected device
7. Click **Run**

#### Option 3: Command Line
```shell
# Build debug APK
./gradlew :composeApp:assembleDebug

# The APK will be at:
# composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Install on connected device
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### iOS (macOS only)

#### Option 1: Xcode (Recommended)
1. Build the shared framework first:
   ```shell
   ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
   ```
2. Open the iOS project in Xcode:
   ```shell
   open iosApp/iosApp.xcodeproj
   ```
3. Select a simulator or connected device
4. Click **Run**

#### Option 2: Android Studio with KMM Plugin
1. Install the Kotlin Multiplatform Mobile plugin
2. Open the project in Android Studio
3. Select the `iosApp` run configuration
4. Choose an iOS simulator
5. Click **Run**

## Project Structure

```
dartzvibe/
├── composeApp/                    # Shared KMP module
│   └── src/
│       ├── commonMain/            # Shared Kotlin code
│       │   ├── kotlin/.../
│       │   │   ├── config/        # App settings
│       │   │   ├── data/          # Models & repositories
│       │   │   ├── di/            # Dependency injection
│       │   │   ├── domain/
│       │   │   │   ├── game/      # Game engine & checkout calculator
│       │   │   │   └── statistics/ # Statistics calculations
│       │   │   ├── network/       # HTTP client
│       │   │   ├── ui/screen/     # Screens & ViewModels
│       │   │   │   ├── home/
│       │   │   │   ├── game/      # Active game + components
│       │   │   │   ├── players/
│       │   │   │   ├── statistics/
│       │   │   │   ├── leaderboard/
│       │   │   │   ├── headtohead/
│       │   │   │   ├── gamedetail/
│       │   │   │   ├── gameslist/
│       │   │   │   └── settings/
│       │   │   └── util/          # Shared utilities
│       │   └── sqldelight/        # Database schemas
│       ├── commonTest/            # Shared tests
│       ├── androidMain/           # Android-specific code
│       └── iosMain/               # iOS-specific code
├── iosApp/                        # iOS application entry point
├── build.gradle.kts               # Root build configuration
└── gradle/libs.versions.toml      # Dependency versions
```

## Build Commands

```shell
# Format code (required before commits)
./gradlew spotlessApply

# Check code formatting
./gradlew spotlessCheck

# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Run all tests
./gradlew :composeApp:allTests

# Run Android unit tests only
./gradlew :composeApp:testDebugUnitTest

# Build iOS framework (macOS only)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Testing

The project uses Kotest with FreeSpec style for testing:

```shell
# Run all tests
./gradlew :composeApp:allTests

# Run with detailed output
./gradlew :composeApp:allTests --info
```

## Architecture Highlights

- **Immutable Game Engine** - all operations return new engine instances with no side effects, making state management predictable and undo trivial
- **Checkout Calculator** - computes up to 3 optimal finish paths for any reachable checkout score
- **Cricket Replay-Safe Undo** - recalculates full cricket segment state by replaying throws when undoing
- **Phantom Turns** - system-generated turns for Parcheesi knockouts that don't affect player turn order
- **Compile-Time DI** - kotlin-inject resolves all dependencies at build time with no runtime reflection

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Voyager Navigation](https://voyager.adriel.cafe/)
