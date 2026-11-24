#!/usr/bin/env bash
# Map every *.indir.local name in scripts/hosts.txt to 127.0.0.1 in /etc/hosts, so a
# service started from the IDE resolves the same names the containers use. Idempotent:
# rewrites only the block between the indir markers. Needs sudo.
set -euo pipefail
LIST="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/hosts.txt"
HOSTS="${HOSTS_FILE:-/etc/hosts}"
block="# >>> indir development >>>
$(sed 's/^/127.0.0.1 /' "$LIST")
# <<< indir development <<<"
tmp="$(mktemp)"
sed '/# >>> indir development >>>/,/# <<< indir development <<</d' "$HOSTS" > "$tmp"
printf '%s\n' "$block" >> "$tmp"
if [[ -w "$HOSTS" ]]; then cat "$tmp" > "$HOSTS"; else sudo cp "$tmp" "$HOSTS"; fi
rm -f "$tmp"
echo "setup-hosts: $(wc -l < "$LIST") names -> 127.0.0.1 in $HOSTS"
