#!/bin/bash
# Side-by-side crop of a real screenshot and a kit screenshot (real left, kit right), scaled up for inspection.
# usage: sbs.sh REAL.png W:H:X:Y KIT.png W:H:X:Y OUT.png [SCALE=2] [GAMMA=1]
. "$(dirname "$0")/lib.sh"
SC="${6:-2}"; GM="${7:-1}"
IFS=: read RW RH RX RY <<< "$2"
IFS=: read KW KH KX KY <<< "$4"
ffmpeg -v error -y -i "$1" -i "$3" -filter_complex \
  "[0]crop=$RW:$RH:$RX:$RY,scale=iw*$SC:ih*$SC:flags=neighbor[a];[1]crop=$KW:$KH:$KX:$KY,scale=iw*$SC:ih*$SC:flags=neighbor[b];[a][b]hstack,eq=gamma=$GM" "$5"
echo "$5"
