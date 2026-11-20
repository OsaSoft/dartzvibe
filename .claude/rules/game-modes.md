---
paths:
  - "**/data/model/GameModels.kt"
  - "**/domain/game/GameEngine.kt"
  - "**/domain/game/*ModeEngine.kt"
  - "**/ui/screen/game/NewGameScreen*.kt"
---

# Adding a game mode

- **New game mode:**
  - a `GameMode` value (plus `GameType` values if needed);
  - a `<Mode>ModeEngine` and its branch in `GameEngine.fromSession`;
  - the mode selection and config in `NewGameScreen`/`NewGameScreenModel` (the compiler won't flag a missing one);
  - mode handling in `ActiveGameScreen`;
  - a `ModeStatistics` subtype and its `StatisticsCalculator` branch;
  - the leg content in `GameDetailScreen` (derive per-dart context in `domain/history/`);
  - any config columns, with a migration (see `.claude/rules/persistence.md`);
  - engine tests.
