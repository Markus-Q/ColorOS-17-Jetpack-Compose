#!/bin/bash
# Builds and installs the catalog on the phone (or runs other gradle tasks: build.sh :iris-core:compileDebugKotlin).
# Only the last lines of the gradle output are printed.
. "$(dirname "$0")/lib.sh"
# Gradle 9 needs JDK 17 or newer. The system JAVA_HOME is often older, so it is not used: set COLOROS_JAVA_HOME to use another JDK.
export JAVA_HOME="${COLOROS_JAVA_HOME:-C:/Program Files/Android/Android Studio/jbr}"
export ANDROID_HOME="$SDK"
TASK="${1:-:catalog:installDebug}"
cd "$ROOT" && ./gradlew "$TASK" --console=plain 2>&1 | grep -v -E "^(> Task|$)" | tail -${LINES_OUT:-25}
echo "gradle exit: ${PIPESTATUS[0]}"
