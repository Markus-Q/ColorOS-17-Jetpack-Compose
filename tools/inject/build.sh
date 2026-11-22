#!/bin/bash
# Builds Inject.java into inject.jar (dex) and pushes it to /data/local/tmp on the phone.
export MSYS_NO_PATHCONV=1
. "$(dirname "$0")/../lib.sh"   # SDK, ADB, JAVA (override with the environment variables named there)
JAVAC="${JAVAC:-C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot/bin/javac.exe}"
D8JAR="$SDK/build-tools/36.0.0/lib/d8.jar"
HERE="$(cd "$(dirname "$0")" && pwd -W)"
OUT="$HERE/out"
rm -rf "$OUT"; mkdir -p "$OUT/classes"
"$JAVAC" --release 17 -cp "$SDK/platforms/android-36/android.jar" -d "$OUT/classes" "$HERE/Inject.java" || exit 1
"$JAVA" -cp "$D8JAR" com.android.tools.r8.D8 --lib "$SDK/platforms/android-36/android.jar" --output "$OUT/inject.jar" "$OUT/classes/Inject.class" || exit 1
"$ADB" push "$OUT/inject.jar" /data/local/tmp/inject.jar
