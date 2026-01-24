# DartzVibe

A cross-platform darts scoring app built with Kotlin Multiplatform and Compose Multiplatform, targeting Android and iOS.

## Features

- **Player Management** - Create and manage player profiles with custom avatars
- **Multiple Game Types** - Support for 301 and 501 game variants
- **Configurable Rules** - Double-in, double-out, and configurable legs to win
- **Live Scoring** - Interactive score input with real-time score tracking
- **Game History** - View completed games and track statistics
- **Resume Games** - Continue in-progress games at any time

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
│       │   │   ├── domain/game/   # Game engine logic
│       │   │   ├── network/       # HTTP client
│       │   │   └── ui/screen/     # Screens & ViewModels
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

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Voyager Navigation](https://voyager.adriel.cafe/)