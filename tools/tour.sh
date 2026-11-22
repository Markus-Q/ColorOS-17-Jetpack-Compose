#!/bin/bash
# Screenshots of every kit demo in the dark and the light theme on the connected phone, for docs/catalog.md.
# usage: tour.sh            -> captures/shots/tour_dark_NAME.png and tour_light_NAME.png (full resolution, status bar still in)
#        tour.sh convert    -> docs/media/catalog/{dark,light}/NAME.png (status bar cropped, 540 px wide)
# The Dialog, BottomSheet and PopupMenu screens are shown open in the docs: their pictures are taken by hand
# (tap the button, screenshot) and copied to docs/media/catalog/{dark,light}.
# The dark shots show the system theme, so run it with the phone in dark mode (the script never changes the setting).
. "$(dirname "$0")/lib.sh"
LIST="BottomNavPill TopBar LargeTitle SegmentedTabs PageTransitions PrimaryButton SecondaryButton IconButton RoundPlusButton TextButton Switch TextField SearchBar Checkbox Slider Progress Card ListItem GroupedList SectionHeader Divider Badge BlurBackground BlurLab PressAnimation Haptics TouchGlow PersistentGlow DragPhysics PreferencesExample HabitsExample TypeScale"
if [ "$1" = "convert" ]; then
  for theme in dark light; do
    mkdir -p "$ROOT/docs/media/catalog/$theme"
    for f in "$SHOTS"/tour_${theme}_*.png; do
      n=$(basename "$f" .png); n=${n#tour_${theme}_}
      ffmpeg -v error -y -i "$f" -vf "crop=1080:2234:0:120,scale=540:-1:flags=lanczos" "$ROOT/docs/media/catalog/$theme/$n.png"
    done
  done
  exit 0
fi
for theme in dark light; do
  extra=""; [ "$theme" = light ] && extra="--ez light true"
  for d in $LIST; do
    catalog $d $extra; sleep 2.3
    shot "tour_${theme}_$d" >/dev/null
  done
done
adb shell am force-stop dev.irisui.catalog; wake; adb shell am start -n dev.irisui.catalog/.MainActivity >/dev/null
echo finished
