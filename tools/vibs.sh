#!/bin/bash
# Prints the haptic entries of the vibrator log (dumpsys vibrator_manager) from today at or after HH:MM:SS:
# "time usage package => reason | played segment". The reason names the performHapticFeedback constant.
# usage: vibs.sh HH:MM:SS
. "$(dirname "$0")/lib.sh"
DAY="$(adb shell date +%m-%d | tr -d '\r')"
adb shell dumpsys vibrator_manager 2>/dev/null | awk -v d="$DAY" -v t0="$1" '
  $1 == d && $3 == "|" {
    split($2, a, ".");
    if (a[1] < t0) next;
    line = $0;
    usage = ""; pkg = ""; reason = ""; played = "";
    if (match(line, /usage: [A-Z_]+/)) usage = substr(line, RSTART + 7, RLENGTH - 7);
    if (match(line, /\| [A-Za-z0-9_.]+ \(uid=/)) { pkg = substr(line, RSTART + 2, RLENGTH - 2); sub(/ \(uid=/, "", pkg) }
    if (match(line, /reason: [^|]*/)) reason = substr(line, RSTART + 8, RLENGTH - 8);
    if (match(line, /played: [^|]*/)) played = substr(line, RSTART + 8, RLENGTH - 8);
    gsub(/OplusPrebakedSegment/, "Seg", played);
    printf "%s %s %s => %s | %s\n", $2, usage, pkg, reason, played
  }' | cut -c1-260
