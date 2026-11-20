---
paths:
  - "**/domain/game/**"
  - "**/domain/history/**"
  - "**/androidMain/AndroidManifest.xml"
  - "**/data/model/GameModels.kt"
  - "**/ui/screen/game/ActiveGameScreenModel.kt"
  - "**/ui/screen/gamedetail/**"
---

# Game engine

- **Game engine is immutable.**
  - Every operation returns a new engine: `addThrow` → `(engine, ThrowResult)`, `endTurn` → `(engine, TurnResult)`, and `undoLastThrow()` returns null when the current turn has no throws. Modes have no undo of their own; see Undo below.
  - `GameEngine.fromSession(session)` picks the `ModeEngine` from `GameConfig.gameMode`, and `toGameSession()` exports the state for saving.
  - `GameEngineHelper` holds the shared logic: current player, scores, leg handling, and `getRoundNumber` (rounds restart each leg).
- **In-progress throws are not saved.** The current turn's throws live only in the engine until `endTurn()`, and `toGameSession()` doesn't include them. Pushing another screen (Settings) keeps them, because the ScreenModel outlives the covered screen's composition. Rotation survives because `AndroidManifest.xml` declares `configChanges` on the activity; keep it. Without it the model might still survive (Voyager's store is process-wide and the screen keys are saved with the stack), but that's not verified. Process death still loses the throws. In Parcheesi, though, `toGameSession()` mid-turn *does* hold the phantom turns the current darts caused, so anything that saves mid-turn (undo, abandon) saves `TurnRewind.turnStartSession(...)` instead.
- **Phantom turns:** Parcheesi knockouts add `Turn(isPhantom = true)`. For turn order, round number and display, use `Leg.playerTurns`, which leaves them out. The phantom is appended mid-turn, when the knocking dart lands, so it sits **before** the turn that caused it in `Leg.turns` (`parcheesiLegHistory` in `domain/history/` links them).
- **Undo** is one session-based rewind in `domain/game/TurnRewind.kt`, for every mode. A single dart: rebuild from the turn-start session and replay the turn's darts minus the last (this is what drops a knockout phantom). The previous turn: rebuild the current leg from `Leg()` by replaying every kept turn with `addThrow`/`endTurn` (that recomputes `cricketState`, `rouletteState`, `checkoutPracticeState` and phantoms, which `fromSession` only reads from the leg), then replay the removed turn's darts without ending it. No snapshots, so it works after a resume. Normal Undo stays in the current leg. `rewindTurn(crossLegBoundary = true)` reverts a leg or match win; it decides whether the win dialogs offer Undo (`canUndoWin`), but the dialog Undo itself keeps the frozen pre-`endTurn` engine, which equals that rewind (see Win dialogs in `.claude/rules/game-screen.md`). A leg that doesn't replay to its stored turns (old saves) refuses to rewind, and `canUndo`/`canUndoWin` then disable the button or hide the dialog Undo. A reopened winning turn takes no more darts (`ActiveGameState.isWinningDartThrown`); End Turn wins again. `ActiveGameScreenModel` saves are serialized and latest-wins, so an undo right after a win can't be overwritten by the COMPLETED save.
- **Cricket rules** live in `domain/game/CricketRules.kt`, shared by `CricketModeEngine` and the Game Details replay (`cricketTurnEffects`). Change scoring there so history and live play can't drift apart. Likewise, the Roulette round → target mapping is `domain/game/RouletteRules.kt`, used by the engine, `ActiveGameScreenModel`, statistics and history.
