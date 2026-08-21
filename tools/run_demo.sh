#!/usr/bin/env bash
# Compiles core/ and runs the console demo (Car panel + connection state machine).
# Requires: kotlinc on PATH (or set KOTLINC_HOME below), java on PATH.
set -euo pipefail
cd "$(dirname "$0")/.."

KOTLINC="${KOTLINC_HOME:-}/bin/kotlinc"
if ! command -v "$KOTLINC" >/dev/null 2>&1; then
  KOTLINC="kotlinc" # fall back to PATH
fi

STDLIB="${KOTLINC_HOME:-}/lib/kotlin-stdlib.jar"

mkdir -p build/classes/main
"$KOTLINC" core/src/main/kotlin -d build/classes/main

java -cp "build/classes/main:${STDLIB}" com.dashboard.core.demo.ConsoleDemoKt
