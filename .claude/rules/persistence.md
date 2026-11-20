---
paths:
  - "**/sqldelight/**"
  - "**/data/**"
  - "**/di/**"
  - "**/iosMain/**/MainViewController.kt"
---

# Persistence

- **SQLDelight `verifyMigrations` is disabled on Windows** (the `sqldelight {}` block in `composeApp/build.gradle.kts`), so on Windows a migration mistake won't fail the build; check `.sqm` files by hand there. Linux and macOS builds verify them.
- **Legs:** a `GameSession` row stores its legs as JSON (`legsJson`, read with `ignoreUnknownKeys`). New fields on `Leg`/`Turn`/`Throw` need **default values** but no SQL migration.
- **Config:** `GameConfig` fields are separate **columns**. A new one needs:
  - the column in `GameSession.sq`;
  - a new `migrations/N.sqm`;
  - the `insert` query;
  - the row mapping in `GameRepositoryImpl`.

  The `update` query only writes legs, leg index, status, finish time and winner, so config can't change after a game starts.
- **Enums** (`GameType`, `GameMode`, `GameStatus`, and `Multiplier` inside the JSON) are stored by **name**. Renaming a constant breaks every saved game.

## New repository

- **New repository:** interface + `@Inject` impl, a `@Provides` in `AppComponent`, the iOS `IosAppDependencies`, a CompositionLocal in `App.kt`, and a `Fake*Repository` in commonTest.
