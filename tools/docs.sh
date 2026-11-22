#!/bin/bash
# Regenerates docs/api/*.md and docs/catalog.md from the source tree (see tools/docs/DocGen.java).
. "$(dirname "$0")/lib.sh"
cd "$ROOT" && "$JAVA" tools/docs/DocGen.java "$@"
