#!/usr/bin/env bash
#
# Cheap style guard: no file under src/ may contain a line longer than 120
# characters. Deliberately not Checkstyle -- no extra build dependency.
#
# Usage: scripts/check-line-length.sh [max-length] [root-dir]

set -euo pipefail

MAX="${1:-120}"
ROOT="${2:-src}"

if [ ! -d "$ROOT" ]; then
  echo "FAIL: '$ROOT' is not a directory." >&2
  exit 1
fi

# Strip CR so CRLF checkouts are not penalised for the line ending itself.
violations="$(
  find "$ROOT" -type f -print0 |
    xargs -0 awk -v max="$MAX" '
      { line = $0; sub(/\r$/, "", line) }
      length(line) > max { printf "%s:%d (%d)\n", FILENAME, FNR, length(line) }
    ' |
    sort -t'(' -k2 -rn
)"

if [ -z "$violations" ]; then
  echo "PASS: no line under $ROOT/ exceeds $MAX characters."
  exit 0
fi

count="$(printf '%s\n' "$violations" | wc -l | tr -d '[:space:]')"
files="$(printf '%s\n' "$violations" | cut -d: -f1 | sort -u | wc -l | tr -d '[:space:]')"

echo "FAIL: $count line(s) in $files file(s) under $ROOT/ exceed $MAX characters:" >&2
printf '%s\n' "$violations" >&2
exit 1
