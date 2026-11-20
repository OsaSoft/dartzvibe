---
name: record-demo
description: Drive DartzVibe on an Android emulator to record a demo video or screenshot, verify a UI change on a device, or check the layout on a small screen. Use when asked to record a demo, video, clip or screenshot of the app, make before/after recordings for a PR, check how a screen looks on the emulator or a short phone, or tap through the app with adb.
---

# Recording demos (Android emulator)

- **AVDs:** `Pixel_9` (1080×2424, 411×923 dp) for every recording. Check layout changes on a short phone too: `Small_Phone` (720×1280, 360×640 dp; created with `avdmanager create avd -n Small_Phone -d small_phone`). Toggle 3-button navigation with `adb shell cmd overlay enable|disable com.android.internal.systemui.navbar.threebutton` and font size with `adb shell settings put system font_scale 1.3`.
- **Cold-boot** (`-no-snapshot`): a saved snapshot held an old build and personal data, and restoring it gave stale screen captures, invisible dialogs and taps landing in the wrong place.
- **Windows:** start `"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd Pixel_9 -no-snapshot -no-boot-anim` as a background process. Don't use `-gpu swiftshader_indirect` there: it was too slow and Android stopped responding. In Git Bash, `export MSYS_NO_PATHCONV=1` before `adb shell` commands that take `/sdcard/...` paths.
- **Linux (AMD GPU):** run headless with software rendering, `emulator -avd Pixel_9 -no-window -gpu swiftshader_indirect -no-audio -no-boot-anim -no-snapshot -port 5554` (a second AVD on `-port 5556`, then `ANDROID_SERIAL=emulator-5556`). It's fast enough here and screen recording works. Use platform-tools **37.x or newer**: with adb 36.0.2 the whole PC hard-froze a few minutes after the adb server started (the same freeze is reported for Ryzen + RX 9060 XT + KDE Wayland machines), with or without the emulator.
- **Setup:**
  - Install with `adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`, then `adb shell pm clear cloud.osasoft.dartzvibe` for clean test data.
  - Turn animations off with `adb shell settings put global window_animation_scale 0`, the same for `transition_animation_scale`, and `adb shell settings put global animator_duration_scale 0`.
- **Theme:** `adb shell cmd uimode night yes|no` only affects the app while its in-app theme is "System", which is the default.
- **Finding tap targets:** `adb shell rm -f /sdcard/ui.xml && adb shell uiautomator dump /sdcard/ui.xml`, then parse the `bounds`. Delete the old file first, because a failed dump silently leaves the previous one in place. Check the result with `adb exec-out screencap -p`.
  - With S/D/T buttons off, the grid and Cricket keys expose their number as `content-desc` (e.g. `25, Outer bull`), not `text`. With Double/Triple selected, the key labels change (`D20`, `BULL`).
  - With four players, Start Game sits below the fold on New Game: scroll first.
- **Recording:** `adb shell screenrecord --time-limit N /sdcard/x.mp4` as a background process, then `adb pull`.
- **Dartboard:**
  - It only appears with Settings → "Radial Dartboard Keypad" on, which is off by default.
  - Swipe-to-multiply also needs "Show Multiplier Buttons" off.
  - The swipe direction comes from Settings → "Dartboard Swipe Direction": up/down (default), or "Like a real board" (toward center = triple, outward = double).
  - A slow `input swipe x y x y2 2500` works; separate `input motionevent` calls don't form one gesture.
- **Game history:** only completed or abandoned games open `GameDetailScreen`. To get one, abandon the game: the X in the top bar, then Yes.

## Helper scripts

`.claude/skills/record-demo/scripts/` holds bash helpers that find nodes through `uiautomator dump`, so they work at any screen size. They use plain `adb`, so set `ANDROID_SERIAL` to pick an emulator (e.g. `ANDROID_SERIAL=emulator-5556`). They need `python3`; `sheets.sh` needs `ffmpeg`. `helpers.sh` puts the SDK's `platform-tools` on `PATH` only when no `adb` is found, so check `adb --version` (see the adb 36.0.2 freeze above).

- `.claude/skills/record-demo/scripts/ui.sh`: prints every node with text, a `content-desc` or a click action as `x,y C 'text' 'content-desc'` (`C` = clickable, `x,y` = centre).
- `source .claude/skills/record-demo/scripts/helpers.sh` (from the repo root), then:
  - `t 'Text' [n]` taps the n-th node whose text matches exactly; `tc 'Desc'` taps by `content-desc`; `w 'Text'` waits up to ~10 s for a text to appear.
  - `key 20`, `key Miss`, `key BULL` tap a keypad key by text or `content-desc` (the last match, so the keypad wins over the checkout hint); `th T20` / `th D16` / `th 5` / `th miss` select the multiplier with the S/D/T buttons and throw.
  - `pk 25` taps a key by `content-desc` (S/D/T buttons off) or text; `swk 20 up|down` swipes on a key (swipe-to-multiply).
  - `startgame` scrolls New Game until Start Game shows and taps it; `scrolldown` scrolls a bit; `addplayer Name` adds a player from the Players screen; `abandon` abandons the running game (the X in the top bar, then Yes).
  - `rec name [seconds]` starts `screenrecord` to `/sdcard/name.mp4` in the background; `stoprec` stops it; `pullrec name dest.mp4` pulls the clip.
  - `D` sets the pause after each tap in seconds (e.g. `D=2.5` while recording, so every dart is visible).
- `.claude/skills/record-demo/scripts/sheets.sh <video> <outdir> [fps]`: contact sheets (5×2 tiles with timestamps) from a clip, for scoring a recording without watching it.
