#!/usr/bin/env bash
#
# Builds a self-contained native launcher with jpackage, which ships with JDK 14 and newer,
# so no extra tooling is required.
#
# Usage: scripts/build-exe.sh [output-dir]
#        default output dir: target/dist
#
# What you get is a DIRECTORY, not a single file: the launcher sits next to an `app/` folder
# and a bundled `runtime/` (a whole JRE, ~130 MB on Windows). The launcher will not run
# without those siblings, so distribute the whole folder.
#
# For a genuinely standalone single binary you want GraalVM's native-image instead, which is
# a separate toolchain. This script deliberately only uses what the JDK already provides.

set -euo pipefail

APP_NAME=PrescriptionReporter
MAIN_CLASS=com.softwareascraft.prescriptionreporter.Main
APP_VERSION=1.0
VENDOR=softwareascraft

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

OUT_DIR="${1:-target/dist}"
JAR="target/$APP_NAME.jar"
STAGING="target/jpackage-in"

# --- locate the JDK tools -------------------------------------------------------
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jpackage" ]; then
  JPACKAGE="$JAVA_HOME/bin/jpackage"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jpackage.exe" ]; then
  JPACKAGE="$JAVA_HOME/bin/jpackage.exe"
else
  JPACKAGE="jpackage"
fi

if ! "$JPACKAGE" --version >/dev/null 2>&1; then
  echo "build-exe: cannot run jpackage ('$JPACKAGE')." >&2
  echo "build-exe: it ships with JDK 14+. Set JAVA_HOME to a JDK 17 and retry." >&2
  exit 127
fi

# --- make sure there is a jar to package ----------------------------------------
if [ ! -f "$JAR" ]; then
  echo "build-exe: $JAR not found; building it with ./mvnw package"
  if ! ./mvnw --batch-mode -DskipTests package; then
    echo "build-exe: './mvnw package' failed, so there is nothing to package." >&2
    echo "build-exe: build the jar yourself, then re-run this script." >&2
    exit 1
  fi
fi

# --- platform differences --------------------------------------------------------
# --win-console is accepted only on Windows, and omitting it there produces a
# GUI-subsystem binary: piped output still works, but run interactively it shows
# nothing and the shell does not wait for it. Always set it for a CLI tool.
PLATFORM_OPTS=()
case "$(uname -s)" in
  MINGW* | MSYS* | CYGWIN* | Windows_NT)
    PLATFORM_OPTS+=(--win-console)
    LAUNCHER="$OUT_DIR/$APP_NAME/$APP_NAME.exe"
    ;;
  Darwin)
    LAUNCHER="$OUT_DIR/$APP_NAME.app/Contents/MacOS/$APP_NAME"
    ;;
  *)
    LAUNCHER="$OUT_DIR/$APP_NAME/bin/$APP_NAME"
    ;;
esac

# --- stage just the jar ----------------------------------------------------------
# jpackage takes an input DIRECTORY and packages everything in it, so target/ as a
# whole would drag in classes and test output.
rm -rf "$STAGING" "$OUT_DIR/$APP_NAME" "$OUT_DIR/$APP_NAME.app"
mkdir -p "$STAGING" "$OUT_DIR"
cp "$JAR" "$STAGING/"

echo "build-exe: packaging $APP_NAME $APP_VERSION into $OUT_DIR"
"$JPACKAGE" \
  --type app-image \
  --name "$APP_NAME" \
  --input "$STAGING" \
  --main-jar "$(basename "$JAR")" \
  --main-class "$MAIN_CLASS" \
  --dest "$OUT_DIR" \
  --app-version "$APP_VERSION" \
  --vendor "$VENDOR" \
  "${PLATFORM_OPTS[@]}"

if [ ! -x "$LAUNCHER" ] && [ ! -f "$LAUNCHER" ]; then
  echo "build-exe: jpackage reported success but no launcher at '$LAUNCHER'." >&2
  exit 1
fi

# --- prove the thing we just built actually runs ---------------------------------
# A packaging mistake (the --win-console one especially) produces a binary that
# builds cleanly and misbehaves only when used, so check it rather than trust it.
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT
cat >"$WORK_DIR/sample.txt" <<'EOF'
Nick A created
Mark B created
Mark B filled
Mark C filled
Mark B returned
John E created
Mark B filled
Mark B filled
Paul D filled
John E filled
John E returned
EOF
EXPECTED="$(printf 'John: 0 fills -$1 income\nMark: 2 fills $9 income\nNick: 0 fills $0 income')"

actual="$("$LAUNCHER" "$WORK_DIR/sample.txt" 2>/dev/null | tr -d '\r')"
if [ "$actual" != "$EXPECTED" ]; then
  echo "build-exe: the packaged launcher did not produce the expected report." >&2
  printf -- '--- expected ---\n%s\n--- actual ---\n%s\n' "$EXPECTED" "$actual" >&2
  exit 1
fi

missing_status=0
"$LAUNCHER" "$WORK_DIR/does-not-exist.txt" >/dev/null 2>&1 || missing_status=$?
if [ "$missing_status" -ne 1 ]; then
  echo "build-exe: expected exit 1 for an unreadable file, got $missing_status." >&2
  exit 1
fi

# On Windows, check the PE subsystem directly. Running the binary cannot detect a missing
# --win-console, because a GUI-subsystem process still inherits a redirected stdout and so
# prints correctly into a pipe like the one above; it misbehaves only when a human runs it
# interactively. Subsystem 2 = GUI, 3 = console.
case "$(uname -s)" in
  MINGW* | MSYS* | CYGWIN* | Windows_NT)
    pe_offset=$(od -An -tu4 -j 60 -N 4 "$LAUNCHER" | tr -d ' ')
    subsystem=$(od -An -tu2 -j $((pe_offset + 92)) -N 2 "$LAUNCHER" | tr -d ' ')
    if [ "$subsystem" != "3" ]; then
      echo "build-exe: '$LAUNCHER' has PE subsystem $subsystem, expected 3 (console)." >&2
      echo "build-exe: a GUI-subsystem build shows no output when run interactively." >&2
      exit 1
    fi
    echo "build-exe: PE subsystem 3 (console) confirmed"
    ;;
esac

echo
echo "build-exe: OK -- report and exit codes verified"
echo "build-exe: launcher  $LAUNCHER"
echo "build-exe: size      $(du -sh "$OUT_DIR/$APP_NAME" 2>/dev/null | cut -f1 || echo '?') (includes a bundled JRE)"
echo "build-exe: ship the whole $OUT_DIR/$APP_NAME folder -- the launcher needs app/ and runtime/"
