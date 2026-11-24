#!/usr/bin/env bash
# Load scripts/aliases.sh in every new shell (~/.bashrc, and ~/.zshrc when it exists).
set -euo pipefail
line="source \"$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/aliases.sh\""
for rc in "$HOME/.bashrc" "$HOME/.zshrc"; do
  [[ -f "$rc" || "$rc" == "$HOME/.bashrc" ]] || continue
  touch "$rc"; grep -qxF "$line" "$rc" || echo "$line" >> "$rc"
  echo "install-aliases: $rc"
done
echo "Open a new terminal (or: $line), then run dev-help"
