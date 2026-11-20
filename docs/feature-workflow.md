# Feature workflow

How features and fixes go from idea to PR in this repo. `CLAUDE.md` has the commit and branch basics; recording on the emulator is the `record-demo` skill (`.claude/skills/record-demo/`).

## Steps

1. **Acceptance criteria first.** Write down what a person should *see* when the feature works, per mode, and in both themes if it changes colours or adds or restyles UI. Number them: the scorer judges against them and the PR description lists them.
2. **Record "before"** on `main` for UI changes. Build `main` in a separate git worktree rather than switching branches (one build per checkout).
3. **Implement**: one agent for small changes. For big or risky features, run 2–3 independent implementations in separate worktrees and keep the one that scores best.
4. **Verify by evidence:** spotless, tests and the build pass, then record "after". Record light **and** dark only when the change affects colours or appearance (a new colour, new UI, a restyle); for behaviour, logic or layout-position fixes one theme is enough. An agent that never saw the code scores the recording against the acceptance criteria. Iterate until it scores ≥ 9/10.
5. **Independent review** by a fresh agent that gets the diff, not the implementer's conclusions. Fix the real findings. When findings sit on the PR, answer each one with "Fixed in `<sha>`: what changed, which test covers it", or with a reason for declining.
6. **Clean history:** messy iteration commits are fine on a work branch. Before opening the PR, replay the work into a few logical commits, with prerequisite fixes first, then the feature, then tests and docs.

## PR description

- PR description sections:
  - **Problem**
  - **Acceptance criteria**: the numbered criteria written before implementation, each with its final result from the independent scorer (pass/partial/fail), plus the overall score and its history (e.g. 8/10 → 9/10). This shows what the feature was built and judged against.
  - **Change**: per feature, with the non-obvious decisions.
  - **Verification**: the exact commands and their results, and test counts.
  - **Not checked**: platforms, modes or interactions you didn't verify.
  - **Already broken on `main`**: problems found along the way but left out of scope.
  - **Videos**: one heading per feature. Mark each clip **Before**/**After**, and **Dark**/**Light** when both themes were recorded.
- **Media files:**
  - Store videos and screenshots in the gitignored `pr-media/<slug>/<feature>/<what>-{before,after}-{light,dark}.mp4`. Rename the folder to `pr-<N>-<slug>` once the PR exists.
  - Attach them with `gh` 2.99 or later (`--attach`; you need push access).
    - In the PR body (`--body-file`), put each video on its own line as `![](pr-media/…/clip.mp4)`, with a blank line before and after. A video inside a sentence becomes a plain link.
    - Pass the same file with `--attach`, for example `gh pr create --body-file body.md --attach pr-media/…/clip.mp4`, repeating `--attach` for each file. `gh` uploads the file and replaces the local path with an inline player. Attached files the body doesn't mention are appended at the end. `gh pr edit` and `gh pr comment` accept `--attach` too.
    - Videos can't have alt text, so put a **Before/After** (and **Dark/Light**) label line above each one.
    - Afterwards, check the body with `gh pr view <N> --json body` and make sure no local `pr-media/` path is left.


## Prompt templates

Fill in the `{{…}}` placeholders. Each agent gets its own git worktree (one Gradle build per checkout).

### Implementer brief (common part)

Put the task, the acceptance criteria and the files in scope above this.

```markdown
## How to work
- You're in your own git worktree of DartzVibe (Kotlin Multiplatform / Compose Multiplatform darts scoring app), based on `main` @ `{{BASE_SHA}}`. Read `CLAUDE.md` at the repo root first and follow it, together with the `.claude/rules/` files for the code you touch (conventions, test style, "input never moves mid-turn", multiplier colours).
- `local.properties` is gitignored and missing from fresh worktrees: copy it from the main checkout before building.
- Keep the diff tight to your own hunks. Don't refactor or reformat surrounding code. {{PARALLEL_WORK_NOTE}}
- **Do not touch the Android emulator or adb**; the coordinator records on it.
- One Gradle build at a time in your worktree. Run `./gradlew spotlessApply`, `./gradlew :composeApp:testDebugUnitTest --no-configuration-cache` and `./gradlew :composeApp:assembleDebug --no-configuration-cache`. Baseline is {{BASELINE_TEST_COUNT}} tests on `main`; count them from `composeApp/build/test-results/testDebugUnitTest/*.xml`.
- Branch `{{BRANCH}}`. Conventional commits ending with `{{CO_AUTHORED_BY_TRAILER}}`; never a claude.ai session link. Commit your work; don't push, don't open a PR.

## Report back
- Branch, final SHA, worktree path, absolute APK path, test count (before/after).
- What a user sees now, per criterion, and which criteria you think are at risk.
- Anything already broken on `main` you noticed (don't fix it unless it's in scope).
```

### Scorer

```markdown
You are an independent scorer for a UI fix in DartzVibe, an Android darts-scoring app. You have NOT seen the code and must not look for it: judge only what the screen shows.

Inputs:
- Acceptance criteria: {{CRITERIA}}
- Frames from screen recordings (PNG, in time order; file names say before/after, theme and the action just taken): {{FRAMES}}
- Notes from the recorder about what each clip does: {{NOTES}}

"Before" frames come from `main` and show the bug; "after" frames come from the fix. Look at every frame (use Read on each PNG).

For each numbered criterion output: PASS / PARTIAL / FAIL / NOT SHOWN, with the frame(s) that prove it and one sentence of reasoning. Code-only criteria (tests, docs) are NOT SHOWN, which is expected; don't penalise them.
Then list any visual defect you notice that the criteria don't mention (clipping, overlap, misalignment, colours, layout shifts between consecutive frames).
Finish with an overall score out of 10 for the visible behaviour and the single most important thing to fix to raise it. Be strict: 9/10 means a user would see nothing wrong.
```

The frames can come from the record-demo skill's `sheets.sh` (contact sheets with timestamps).

### Reviewer

```markdown
You are an independent code reviewer for DartzVibe (Kotlin Multiplatform / Compose Multiplatform darts app). Review ONE branch's diff against `main` ({{BASE_SHA}}). You get the diff and the acceptance criteria only, not the implementer's reasoning.

- Repo checkout to read (read-only; do not edit, commit, build, or touch adb/emulator): {{WORKTREE}}
- Diff: `git -C {{WORKTREE}} diff {{BASE_SHA}}..HEAD`
- Acceptance criteria: {{CRITERIA}}
- Read `CLAUDE.md` at the repo root and the `.claude/rules/` files for the changed paths for conventions (immutable engine, phantom turns, undo via TurnRewind, "input never moves mid-turn", exhaustive `when` without `else`, Kotest FreeSpec "Should…" names with GIVEN/WHEN/THEN, FixedDecimal, etc.).

Look for: correctness bugs (wrong values in edge cases: undo, reopened turns, new legs, phantom turns, old saves with missing data, 4 players), criteria the code doesn't actually meet, layout risks (height changes, wrapping, clipping), convention violations, missing or weak tests, dead code. Read surrounding code as needed to verify claims.

Report: a list of findings, each with file:line, severity (bug / risk / nit), a concrete failure scenario, and a suggested fix. Only report things you verified in the code. Say explicitly if you found no real bugs.
```
