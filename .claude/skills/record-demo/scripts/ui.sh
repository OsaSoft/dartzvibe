#!/usr/bin/env bash
# Print every UI node that has text, a content-desc or a click action, one per line:
#   x,y C 'text' 'content-desc'      (x,y = centre of its bounds, C = clickable)
# Uses the device in ANDROID_SERIAL when several are connected. Needs python3.
export MSYS_NO_PATHCONV=1 # Git Bash on Windows: keep /sdcard paths as they are
# Delete the old dump first: a failed dump silently leaves the previous file in place.
adb shell rm -f /sdcard/ui.xml
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
adb shell cat /sdcard/ui.xml | python3 -c '
import re, sys
xml = sys.stdin.read()
for m in re.finditer(r"<node [^>]*>", xml):
    n = m.group(0)
    text = re.search(r"text=\"([^\"]*)\"", n)[1]
    desc = re.search(r"content-desc=\"([^\"]*)\"", n)[1]
    b = list(map(int, re.findall(r"\d+", re.search(r"bounds=\"([^\"]*)\"", n)[1])))
    clickable = "clickable=\"true\"" in n
    if text or desc or clickable:
        print(f"{(b[0] + b[2]) // 2},{(b[1] + b[3]) // 2}", "C" if clickable else " ", repr(text), repr(desc))
'
