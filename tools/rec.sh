#!/bin/bash
# Records the phone screen while the injector plays a gesture, then runs an analyzer over the cropped frames.
# usage: rec.sh NAME "INJECT SCRIPT" SECS W:H:X:Y ANALYZER [ANALYZER ARGS...]
#   NAME      result files go to captures/runs/NAME.{inj,mp4,times,csv}
#   crop      W:H:X:Y in screen pixels (use full for the whole 1080x2354 frame)
#   ANALYZER  a Java file in tools/analysis (Probe, Thumb, PageShift, Pix, Scroll, Mean ...), it gets "W H ARGS..." on
#             the command line and the cropped raw frames (rgb24, or gray when PIXFMT=gray) on stdin
# env: LEAD=1.0 seconds between recording start and gesture start, BITRATE=24000000, PIXFMT=rgb24|gray
. "$(dirname "$0")/lib.sh"
NAME="$1"; SCRIPT="$2"; SECS="$3"; CROP="$4"; ANALYZER="$5"; shift 5
LEAD="${LEAD:-1.0}"
OUT="$RUNS/$NAME"
echo "$SCRIPT" | tr ';' '\n' | sed 's/^ *//' > "$OUT.inj"
adb push "$OUT.inj" /data/local/tmp/s.txt >/dev/null
wake
adb shell "rm -f /sdcard/rec.mp4; screenrecord --time-limit $SECS --bit-rate ${BITRATE:-24000000} /sdcard/rec.mp4 & sleep $LEAD; CLASSPATH=/data/local/tmp/inject.jar app_process /system/bin Inject @/data/local/tmp/s.txt; wait" > "$OUT.inject.txt" 2>&1
adb pull /sdcard/rec.mp4 "$OUT.mp4" >/dev/null 2>&1
"$(dirname "$0")/ana.sh" "$NAME" "$CROP" "$ANALYZER" "$@"
