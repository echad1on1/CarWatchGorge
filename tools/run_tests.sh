#!/usr/bin/env bash
# Compiles core/ (main + test) and runs all automated tests.
# Requires: kotlinc on PATH (or set KOTLINC_HOME below), java on PATH.
set -euo pipefail
cd "$(dirname "$0")/.."

KOTLINC="${KOTLINC_HOME:-}/bin/kotlinc"
if ! command -v "$KOTLINC" >/dev/null 2>&1; then
  KOTLINC="kotlinc" # fall back to PATH
fi

STDLIB="${KOTLINC_HOME:-}/lib/kotlin-stdlib.jar"

mkdir -p build/classes/main build/classes/test
"$KOTLINC" core/src/main/kotlin -d build/classes/main
"$KOTLINC" core/src/test/kotlin -cp build/classes/main -d build/classes/test

java -cp "build/classes/main:build/classes/test:${STDLIB}" com.dashboard.core.tests.AllTestsKt
