---
paths:
  - "**/sqldelight/**"
  - "**/data/**"
  - "**/di/**"
  - "**/iosMain/**/MainViewController.kt"
---

# Persistence

- **Migrations are checked by `DatabaseMigrationTest`** (`androidUnitTest`), which builds each historical schema on a real SQLite database, migrates it and compares it with `Schema.create`. A new `N.sqm` means adding the previous schema (version N) to its `HISTORICAL_SCHEMAS`. SQLDelight's `verifySqlDelightMigration` task can't help: there are no `.db` schema files in the repo, so on Linux and macOS it fails with "requires a database file" (it's turned off on Windows), and so does `check`, which depends on it. `assembleDebug` and `testDebugUnitTest` don't run it.
- **Schema version = highest `N.sqm` + 1**, and `N.sqm` runs when the device's version is ≤ N. Version 2 never existed: `2.sqm` took databases from 1 straight to 3.
- **`1.sqm` must stay idempotent** (`CREATE TABLE IF NOT EXISTS`). It only runs on v1 databases, which already have both tables.
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
