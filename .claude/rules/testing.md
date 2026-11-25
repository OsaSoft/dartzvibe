---
paths:
  - "**/commonTest/**"
  - "**/androidUnitTest/**"
---

# Tests

- Kotest `FreeSpec`. Test names start with "Should". Mark the GIVEN / WHEN / THEN steps with comments. Use `withData` (`io.kotest.datatest.withData`) for data-driven cases.
- New game rules get engine-level tests in `commonTest/.../domain/game/`. ScreenModel tests use the `Fake*Repository` classes; update the fakes whenever a repository interface changes.
- ScreenModels built directly in tests (not through `rememberScreenModel`) get no scope of their own: Voyager hands them the scope of the last model key it gave out (`lastScreenModelKey`, never cleared) or, while nothing in the process has called `rememberScreenModel` (true in unit tests), one shared "standalone" scope. `onDispose()` alone doesn't cancel that scope; to simulate a pop, cancel it through the store as `ActiveGameScreenModelLifecycleTest` does.
- In new code, inject randomness and time (for example `random: Random = Random.Default`) so tests can pin them. Most existing code still calls global `Random` and `currentTimeMillis()`.

## Running tests

- Check that tests actually ran: count them in `composeApp/build/test-results/testDebugUnitTest/*.xml`.
- To run one spec, pass its fully qualified name: `--tests 'cloud.osasoft.dartzvibe.data.repository.AppSettingsRepositoryImplTest'`. A wildcard such as `--tests '*AppSettingsRepositoryImplTest*'` fails with "Could not find spec TestDescriptor for …ComposeAppCommonTest". Add `--rerun` to repeat a passing run.
