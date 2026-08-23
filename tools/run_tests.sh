#!/usr/bin/env bash
# Compiles core/ (main + test) and runs all automated tests.
# Requires: kotlinc AND kotlin on PATH (both ship together with any standard Kotlin install —
# Homebrew's `brew install kotlin`, SDKMAN's `sdk install kotlin`, or the manual download used
# to build this project originally).
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v kotlinc >/dev/null 2>&1; then
  echo "error: kotlinc not found on PATH." >&2
  echo "Install it, e.g.: brew install kotlin   (or)   sdk install kotlin" >&2
  exit 1
fi
if ! command -v kotlin >/dev/null 2>&1; then
  echo "error: kotlin (the runner, distinct from kotlinc the compiler) not found on PATH." >&2
  echo "It should have installed alongside kotlinc — check your Kotlin installation." >&2
  exit 1
fi

mkdir -p build/classes/main build/classes/test
kotlinc core/src/main/kotlin -d build/classes/main
kotlinc core/src/test/kotlin -cp build/classes/main -d build/classes/test

# `kotlin` (not `java`) automatically puts kotlin-stdlib.jar on the classpath for us, so we don't
# need to locate it manually or set any extra environment variables.
kotlin -cp "build/classes/main:build/classes/test" com.dashboard.core.tests.AllTestsKt
