#!/bin/bash
# Records the phone screen while the injector plays a touch script and turns the recording into a small GIF for the README.
# usage: gif.sh NAME "INJECT SCRIPT" SECS [W:H:X:Y crop] [WIDTH px] [FPS]
#   NAME      the GIF is written to docs/media/NAME.gif, the raw video stays in captures/runs/NAME.mp4 (not in git)
#   SECS      length of the recording in whole seconds (screenrecord takes no fractions); it starts LEAD seconds before the first touch event
#   crop      region of the 1080x2354 frame to keep, default the whole screen (the status bar is 120 px high)
# env: REENCODE=1 (skip the recording, make the GIF again from captures/runs/NAME.mp4 with other crop, width, fps or COLORS),
#      LEAD=0.7 (seconds before the gesture), TRIM=0.2 (seconds cut from the start of the video), END (seconds to keep),
#      COLORS=112 (palette size), HOLD=50 (hundredths of a second the last frame stays before the loop restarts)
# Open the screen first, e.g. `catalog DragPhysics --ez touches true` (see lib.sh); `--ez touches true` draws a circle under
# the finger, because the screen recording does not show touches and the system "show touches" setting is left alone.
. "$(dirname "$0")/lib.sh"
NAME="$1"; SCRIPT="$2"; SECS="$3"; CROP="${4:-1080:2354:0:0}"; OW="${5:-360}"; FPS="${6:-20}"
LEAD="${LEAD:-0.7}"; TRIM="${TRIM:-0.2}"
OUT="$RUNS/$NAME"; MEDIA="$ROOT/docs/media"
mkdir -p "$MEDIA"
if [ -z "$REENCODE" ]; then
echo "$SCRIPT" | tr ';' '\n' | sed 's/^ *//' > "$OUT.inj"
adb push "$OUT.inj" /data/local/tmp/s.txt >/dev/null
wake
adb shell "rm -f /sdcard/rec.mp4; screenrecord --time-limit $SECS --bit-rate 16000000 /sdcard/rec.mp4 & sleep $LEAD; CLASSPATH=/data/local/tmp/inject.jar app_process /system/bin Inject @/data/local/tmp/s.txt; wait" > "$OUT.inject.txt" 2>&1
adb pull /sdcard/rec.mp4 "$OUT.mp4" >/dev/null 2>&1
adb shell rm -f /sdcard/rec.mp4
fi
IFS=: read CW CH CX CY <<< "$CROP"
ffmpeg -v error -y -ss "$TRIM" ${END:+-t "$END"} -i "$OUT.mp4" \
  -vf "fps=$FPS,crop=$CW:$CH:$CX:$CY,scale=$OW:-2:flags=lanczos,split[a][b];[a]palettegen=max_colors=${COLORS:-112}:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle" \
  -final_delay "${HOLD:-50}" "$MEDIA/$NAME.gif"
ls -l "$MEDIA/$NAME.gif" | awk '{printf "%s  %.2f MB\n", $9, $5/1048576}'
