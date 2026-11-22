#!/bin/bash
# Checks the relative links, images and anchors of README.md and docs/ (see tools/docs/LinkCheck.java).
. "$(dirname "$0")/lib.sh"
cd "$ROOT" && "$JAVA" tools/docs/LinkCheck.java
