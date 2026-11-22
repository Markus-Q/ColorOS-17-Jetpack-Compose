#!/bin/bash
# Shared helpers for the measurement scripts. Source it: . "$(dirname "$0")/lib.sh"
export MSYS_NO_PATHCONV=1
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -W)"   # C:/... form, works for bash and the Windows tools
ROOTW="$ROOT"
# Where things live on a Windows machine with Git Bash. Override with the environment variables SDK (or ANDROID_HOME) and JAVA.
if [ -z "$SDK" ]; then
  if [ -n "$ANDROID_HOME" ]; then SDK="$ANDROID_HOME"
  elif [ -n "$LOCALAPPDATA" ]; then SDK="$(cygpath -m "$LOCALAPPDATA" 2>/dev/null || echo "$LOCALAPPDATA")/Android/Sdk"
  else SDK="$HOME/Android/Sdk"; fi
fi
ADB="$SDK/platform-tools/adb.exe"
JAVA="${JAVA:-C:/Program Files/Android/Android Studio/jbr/bin/java.exe}"
RUNS="$ROOT/captures/runs"
SHOTS="$ROOT/captures/shots"
mkdir -p "$RUNS" "$SHOTS"

adb() { "$ADB" "$@"; }

# jv TOOL args...  -> runs tools/analysis/TOOL.java (single-file Java) with the Android Studio JBR
jv() { local t="$1"; shift; "$JAVA" "$ROOT/tools/analysis/$t.java" "$@"; }

# Wake the screen and dismiss the swipe keyguard (never enters credentials).
wake() {
  adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
  adb shell wm dismiss-keyguard >/dev/null 2>&1
}

# shot NAME  -> captures/shots/NAME.png
shot() { adb exec-out screencap -p > "$SHOTS/$1.png"; echo "$SHOTS/$1.png"; }

# px FILE X Y [W H] -> average RGB of the block
px() {
  local f="$1" x="$2" y="$3" w="${4:-1}" h="${5:-1}"
  ffmpeg -v error -i "$f" -vf "crop=$w:$h:$x:$y,scale=1:1:flags=area,format=rgb24" -frames:v 1 -f rawvideo -pix_fmt rgb24 - | od -An -tu1 | awk '{printf "%d,%d,%d\n", $1,$2,$3}'
}

# uidump [NAME] -> prints a compact view tree of the foreground window
uidump() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$RUNS/${1:-ui}.xml" >/dev/null 2>&1
  "$JAVA" "$ROOTW/tools/analysis/UiDump.java" "$RUNS/${1:-ui}.xml" ${2:-}
}

# Open a catalog demo: catalog DEST [extra args for am start]
catalog() {
  local dest="$1"; shift
  wake
  adb shell am force-stop dev.irisui.catalog
  adb shell am start -n dev.irisui.catalog/.MainActivity --es destination "$dest" "$@" >/dev/null
}

# Launch an app's launcher activity by package: launch PACKAGE
launch() { wake; adb shell monkey -p "$1" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; }

# vibs_since HH:MM:SS -> haptic entries of the vibrator log at or after that time: "time reason | segment"
vibs_since() {
  adb shell dumpsys vibrator_manager 2>/dev/null | grep -E "^\s+11-[0-9]{2} [0-9:.]+ \|" | awk -v d="$(adb shell date +%m-%d | tr -d "")" -v t0="$1" '{split($2,a,"."); if ($1==d && a[1] >= t0) print}' \
    | sed -E 's/\|\s+(effect|external)\s+\|[^|]*\|[^|]*\|[^|]*\|[^|]*\|[^|]*\|[^|]*\|[^|]*\|[^|]*\| ([^ ]+) \(uid[^|]*\| reason: ([^|]*)\| played: ([^|]*)\|.*/\2 \3 => \4/' | cut -c1-230
}

# fg PKG -> success when the focused window belongs to PKG (guards every injection into a real app)
fg() { adb shell dumpsys window 2>/dev/null | grep -m1 "mCurrentFocus" | grep -q " $1/"; }

# need PKG -> bring the app to the front and verify it; returns non-zero (and says why) when something else has focus
need() {
  launch "$1"; sleep 1.6
  if ! fg "$1"; then
    echo "ABORT: $1 does not have focus: $(adb shell dumpsys window 2>/dev/null | grep -m1 mCurrentFocus | cut -c1-150)"
    return 1
  fi
}
