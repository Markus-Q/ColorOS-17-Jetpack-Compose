#!/bin/bash
# Contact sheet of a recording: frames every STEP_MS from T0 (seconds into the recording), cropped and tiled.
# usage: sheet.sh NAME T0 STEP_MS COLS ROWS W:H:X:Y [SCALE=0.5]   -> captures/runs/NAME.sheet.png
. "$(dirname "$0")/lib.sh"
NAME="$1"; T0="$2"; STEP="$3"; COLS="$4"; ROWS="$5"; CROP="$6"; SC="${7:-0.5}"
IFS=: read CW CH CX CY <<< "$CROP"
N=$((COLS*ROWS))
FPS=$(awk -v s="$STEP" 'BEGIN{printf "%.4f", 1000/s}')
ffmpeg -v error -y -ss "$T0" -i "$RUNS/$NAME.mp4" -vf "fps=$FPS,crop=$CW:$CH:$CX:$CY,scale=iw*$SC:ih*$SC,tile=${COLS}x${ROWS}" -frames:v 1 "$RUNS/$NAME.sheet.png"
echo "$RUNS/$NAME.sheet.png"
