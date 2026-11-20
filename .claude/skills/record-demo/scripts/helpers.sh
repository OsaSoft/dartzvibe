#!/usr/bin/env bash
# Source this: helpers for driving DartzVibe on an emulator with adb.
#   source .claude/skills/record-demo/scripts/helpers.sh
# Every helper finds its target through scripts/ui.sh (uiautomator), so none of them depend on the screen size.
# adb picks the device from ANDROID_SERIAL. D sets the pause after each tap (seconds).

HELPERS_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
UI_SH="$HELPERS_DIR/ui.sh"
export MSYS_NO_PATHCONV=1 # Git Bash on Windows: keep /sdcard paths as they are

# No adb on PATH: add the SDK's platform-tools and emulator, found through local.properties (missing in fresh
# worktrees), then ANDROID_HOME / ANDROID_SDK_ROOT. An adb already on PATH is kept (adb 36.0.2 froze a Linux PC).
if ! command -v adb >/dev/null 2>&1; then
  _repo=$(git -C "$HELPERS_DIR" rev-parse --show-toplevel 2>/dev/null)
  _sdk=$(sed -n 's/^sdk.dir=//p' "$_repo/local.properties" 2>/dev/null)
  _sdk=${_sdk:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}}
  export PATH="$_sdk/platform-tools:$_sdk/emulator:$PATH"
  unset _repo _sdk
fi

# ui: the node list from ui.sh, retried while a dump comes back empty.
ui() {
  local o i
  for i in 1 2 3; do
    o=$(bash "$UI_SH" 2>/dev/null)
    [ -n "$o" ] && { echo "$o"; return 0; }
    sleep 1
  done
  return 1
}

_tap() { adb shell input tap "${1%,*}" "${1#*,}"; }

# t 'Text' [n]: tap the n-th node whose text matches exactly.
t() {
  local c
  c=$(ui | grep -F " '$1' " | sed -n "${2:-1}p" | cut -d' ' -f1)
  [ -z "$c" ] && { echo "NOTFOUND: $1" >&2; return 1; }
  _tap "$c"; sleep "${D:-1.2}"
}

# tc 'Desc': tap the first node without text whose content-desc matches exactly (icon buttons).
tc() {
  local c
  c=$(ui | grep -F " '' '$1'" | head -1 | cut -d' ' -f1)
  [ -z "$c" ] && { echo "NOTFOUND desc: $1" >&2; return 1; }
  _tap "$c"; sleep "${D:-1.2}"
}

# w 'Text': wait until a text appears (about 10 s at most).
w() {
  local i
  for i in 1 2 3 4 5 6 7 8 9 10; do
    ui | grep -qF " '$1' " && return 0
    sleep 1
  done
  echo "TIMEOUT: $1" >&2; return 1
}

# padkey <label>: centre of a keypad key. With S/D/T buttons off the keys expose their number as
# content-desc ("25" matches "25, Outer bull"); with buttons on, the label is the text ("D20", "BULL").
# Takes the last match, so the keypad wins over the checkout hint above it.
padkey() { ui | grep -E " '$1(, [^']*)?'( |\$)" | tail -1 | cut -d' ' -f1; }

# key <label>: tap a keypad key (key 20, key Miss, key BULL). pk is the same.
key() {
  local c
  c=$(padkey "$1")
  [ -z "$c" ] && { echo "NOKEY: $1" >&2; return 1; }
  _tap "$c"; sleep "${D:-1.8}"
}
pk() { key "$@"; }

# th <dart>: throw with the S/D/T buttons on: th T20, th D16, th 5, th miss.
th() {
  case $1 in
    miss) key Miss; return ;;
    T*) t Triple ;;
    D*) t Double ;;
  esac
  key "$1"
}

# swk <label> up|down: swipe on a key (swipe-to-multiply, S/D/T buttons off).
swk() {
  local c x y d=110
  c=$(padkey "$1")
  [ -z "$c" ] && { echo "NOKEY: $1" >&2; return 1; }
  x=${c%,*} y=${c#*,}
  [ "$2" = up ] && d=-110
  adb shell input swipe "$x" "$y" "$x" $((y + d)) 600; sleep "${D:-1.8}"
}

# scrolldown: scroll down by about 40% of the screen.
scrolldown() {
  local s w h
  s=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1)
  w=${s%x*} h=${s#*x}
  adb shell input swipe $((w / 2)) $((h * 3 / 4)) $((w / 2)) $((h / 3)) 400; sleep 1
}

# startgame: on New Game, scroll until Start Game is visible (four players push it below the fold) and tap it.
startgame() {
  local i
  for i in 1 2 3 4; do
    ui | grep -qF " 'Start Game' " && { t 'Start Game'; sleep 1.5; return 0; }
    scrolldown
  done
  echo "NOTFOUND: Start Game" >&2; return 1
}

# addplayer <Name>: add a player; call it from the Players screen.
addplayer() {
  tc 'Add Player'
  t 'Name *'
  adb shell input text "$1"; sleep 0.5
  adb shell input keyevent 111; sleep 0.5 # ESC closes the keyboard
  tc 'Save'; sleep 1
}

# abandon: abandon the running game (the X in the top bar, then Yes). Abandoned games open Game Details.
abandon() { tc 'Abandon game' && sleep 0.5 && t 'Yes'; }

# rec <name> [seconds=170]: record the screen to /sdcard/<name>.mp4 in the background.
rec() { adb shell screenrecord --time-limit "${2:-170}" "/sdcard/$1.mp4" & }

# stoprec: stop the recording and give screenrecord time to finish the file.
stoprec() { adb shell pkill -INT screenrecord; sleep 3; }

# pullrec <name> <dest.mp4>: pull a finished recording.
pullrec() { mkdir -p "$(dirname "$2")" && adb pull "/sdcard/$1.mp4" "$2"; }
