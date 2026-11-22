#!/bin/bash
# Runs an analyzer over an existing recording captures/runs/NAME.mp4 (see rec.sh).
# usage: ana.sh NAME W:H:X:Y ANALYZER [ANALYZER ARGS...]   -> captures/runs/NAME.csv (t_ms,idx,analyzer columns...)
. "$(dirname "$0")/lib.sh"
NAME="$1"; CROP="$2"; ANALYZER="$3"; shift 3
OUT="$RUNS/$NAME"
ffprobe -v error -select_streams v:0 -show_entries frame=pts_time -of csv=p=0 "$OUT.mp4" | tr -d '\r' > "$OUT.times"
if [ "$CROP" = "full" ]; then CROP="1080:2354:0:0"; fi
IFS=: read CW CH CX CY <<< "$CROP"
FMT="${PIXFMT:-rgb24}"
if [ "$FMT" = "gray" ]; then PF="gray"; else PF="rgb24"; fi
ffmpeg -v fatal -i "$OUT.mp4" -vf "crop=$CW:$CH:$CX:$CY,format=$PF" -fps_mode passthrough -f rawvideo -pix_fmt $PF - \
  | "$JAVA" "$ROOTW/tools/analysis/$ANALYZER.java" "$CW" "$CH" "$@" > "$OUT.raw"
paste -d, "$OUT.times" "$OUT.raw" | awk -F, '{printf "%.1f", $1*1000; for(i=2;i<=NF;i++) printf ",%s", $i; printf "\n"}' > "$OUT.csv"
cp "$OUT.csv" "$OUT.$ANALYZER.csv"
echo "frames: $(wc -l < "$OUT.csv")  -> captures/runs/$NAME.csv"
