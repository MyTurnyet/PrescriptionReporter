#!/usr/bin/env bash
#
# End-to-end smoke test for the packaged jar.
#
# Runs the real artifact three ways and asserts on STDOUT and exit codes only.
# STDERR is deliberately NOT asserted: warning wording is free to change.
#
# Usage: scripts/smoke-test.sh [path-to-jar]
#        defaults to target/PrescriptionReporter.jar (the pom sets <finalName>)

set -euo pipefail

JAR="${1:-target/PrescriptionReporter.jar}"

if [ ! -f "$JAR" ]; then
  echo "FAIL: jar not found at '$JAR' -- run './mvnw package' first (or pass the jar path)." >&2
  exit 1
fi

# Resolve the JVM explicitly, preferring JAVA_HOME, and prove it runs before asserting
# anything. Without this the checks below can "pass" on exit code 127 (command not found):
# a non-zero-exit assertion is satisfied by a missing binary, which is a false pass.
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java.exe" ]; then
  JAVA="$JAVA_HOME/bin/java.exe"
else
  JAVA="java"
fi

if ! "$JAVA" -version >/dev/null 2>&1; then
  echo "FAIL: cannot run a JVM ('$JAVA'). Set JAVA_HOME or put java on PATH." >&2
  exit 1
fi

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

INPUT="$WORK_DIR/prescriptions.txt"
cat >"$INPUT" <<'EOF'
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

EXPECTED="$(cat <<'EOF'
John: 0 fills -$1 income
Mark: 2 fills $9 income
Nick: 0 fills $0 income
EOF
)"

failures=0

note_pass() { printf 'PASS: %s\n' "$1"; }

note_fail() {
  printf 'FAIL: %s\n' "$1" >&2
  failures=$((failures + 1))
}

# Normalise CRLF so the script also passes on Windows, where the JVM's
# line.separator is "\r\n".
run_jar() {
  "$JAVA" -jar "$JAR" "$@" 2>"$WORK_DIR/stderr.txt" | tr -d '\r'
}

# --- 1. file argument: exact stdout and exit code 0 -------------------------
file_status=0
file_output="$(run_jar "$INPUT")" || file_status=$?

if [ "$file_status" -ne 0 ]; then
  note_fail "file argument: expected exit code 0, got $file_status"
else
  note_pass "file argument: exit code 0"
fi

if [ "$file_output" = "$EXPECTED" ]; then
  note_pass "file argument: stdout matches the expected report"
else
  note_fail "file argument: stdout did not match the expected report"
  printf -- '--- expected ---\n%s\n--- actual ---\n%s\n--------------\n' \
    "$EXPECTED" "$file_output" >&2
fi

# --- 2. same input on stdin: identical stdout, exit code 0 ------------------
stdin_status=0
stdin_output="$(cat "$INPUT" | "$JAVA" -jar "$JAR" 2>"$WORK_DIR/stderr-stdin.txt" | tr -d '\r')" \
  || stdin_status=$?

if [ "$stdin_status" -ne 0 ]; then
  note_fail "stdin: expected exit code 0, got $stdin_status"
else
  note_pass "stdin: exit code 0"
fi

if [ "$stdin_output" = "$EXPECTED" ]; then
  note_pass "stdin: stdout matches the expected report"
else
  note_fail "stdin: stdout did not match the expected report"
  printf -- '--- expected ---\n%s\n--- actual ---\n%s\n--------------\n' \
    "$EXPECTED" "$stdin_output" >&2
fi

if [ "$stdin_output" = "$file_output" ]; then
  note_pass "stdin and file argument produce identical stdout"
else
  note_fail "stdin and file argument produced different stdout"
fi

# --- 3. missing file argument: non-zero exit --------------------------------
missing_status=0
"$JAVA" -jar "$JAR" "$WORK_DIR/does-not-exist.txt" \
  >"$WORK_DIR/missing-stdout.txt" 2>"$WORK_DIR/missing-stderr.txt" \
  || missing_status=$?

if [ "$missing_status" -ne 0 ]; then
  note_pass "missing file argument: non-zero exit code ($missing_status)"
else
  note_fail "missing file argument: expected a non-zero exit code, got 0"
fi

# --- 4. the documented launcher -------------------------------------------------
#
# The README tells people to run ./prescriptionreporter, so that is what has to work.
# Checking only `java -jar` would leave the documented entry point unproven, including
# whether it forwards stdin and propagates the program's exit codes.
LAUNCHER="$(cd "$(dirname "$0")/.." && pwd)/prescriptionreporter"

if [ ! -f "$LAUNCHER" ]; then
  note_fail "launcher not found at '$LAUNCHER'"
else
  launcher_status=0
  launcher_output="$("$LAUNCHER" "$INPUT" 2>"$WORK_DIR/launcher-stderr.txt" | tr -d '\r')" \
    || launcher_status=$?

  if [ "$launcher_status" -eq 0 ] && [ "$launcher_output" = "$EXPECTED" ]; then
    note_pass "launcher with a file argument: exit 0 and matching stdout"
  else
    note_fail "launcher with a file argument: exit $launcher_status, stdout mismatch"
    printf -- '--- expected ---\n%s\n--- actual ---\n%s\n--------------\n' \
      "$EXPECTED" "$launcher_output" >&2
  fi

  launcher_stdin_status=0
  launcher_stdin_output="$(cat "$INPUT" | "$LAUNCHER" 2>"$WORK_DIR/launcher-stdin-stderr.txt" \
    | tr -d '\r')" || launcher_stdin_status=$?

  if [ "$launcher_stdin_status" -eq 0 ] && [ "$launcher_stdin_output" = "$EXPECTED" ]; then
    note_pass "launcher reading stdin: exit 0 and matching stdout"
  else
    note_fail "launcher reading stdin: exit $launcher_stdin_status, stdout mismatch"
  fi

  # Exit codes are part of the contract, so the launcher must not flatten them:
  # 1 for an unreadable input, 2 for a bad command line.
  launcher_missing_status=0
  "$LAUNCHER" "$WORK_DIR/does-not-exist.txt" >/dev/null 2>&1 || launcher_missing_status=$?
  if [ "$launcher_missing_status" -eq 1 ]; then
    note_pass "launcher propagates exit 1 for an unreadable file"
  else
    note_fail "launcher: expected exit 1 for an unreadable file, got $launcher_missing_status"
  fi

  launcher_usage_status=0
  "$LAUNCHER" one.txt two.txt >/dev/null 2>&1 || launcher_usage_status=$?
  if [ "$launcher_usage_status" -eq 2 ]; then
    note_pass "launcher propagates exit 2 for a usage error"
  else
    note_fail "launcher: expected exit 2 for a usage error, got $launcher_usage_status"
  fi
fi

# --- summary ----------------------------------------------------------------
echo
if [ "$failures" -eq 0 ]; then
  echo "SMOKE TEST PASSED"
  exit 0
fi

echo "SMOKE TEST FAILED ($failures check(s) failed)" >&2
exit 1
