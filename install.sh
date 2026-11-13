#!/usr/bin/env bash
# Install the Java backend software factory into a service repository.
#
#   ./install.sh <path-to-service-repo> [--force] [--labels] [--verify]
#
#   --force    overwrite factory files that already exist in the target
#   --labels   create the factory labels on GitHub (needs an authenticated gh)
#   --verify   run factory/checks.sh in the target after installing
#
# Copies kit/ into the target, sets base_branch from the target's origin,
# appends the gitignore entries and the "Software factory" section to the
# target's CLAUDE.md. Never commits or pushes: review the diff and commit it
# yourself (or let Claude do it on your yes).
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KIT="$HERE/kit"

usage() { sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

TARGET=""; FORCE=0; LABELS=0; VERIFY=0
for arg in "$@"; do
  case "$arg" in
    --force) FORCE=1 ;;
    --labels) LABELS=1 ;;
    --verify) VERIFY=1 ;;
    -h|--help) usage 0 ;;
    -*) echo "unknown option: $arg" >&2; usage 1 ;;
    *) [[ -z "$TARGET" ]] && TARGET="$arg" || { echo "one target only" >&2; exit 1; } ;;
  esac
done
[[ -n "$TARGET" ]] || usage 1
TARGET="$(cd "$TARGET" && pwd)"

fail() { echo "install: $*" >&2; exit 1; }
warn() { echo "install: warning: $*" >&2; }

# ---- 0. Check the target ------------------------------------------------------
git -C "$TARGET" rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "$TARGET is not a git repository (git init + a GitHub origin first)"
[[ "$(git -C "$TARGET" rev-parse --show-toplevel)" == "$TARGET" ]] || fail "$TARGET is not the root of its git repository"
[[ -f "$TARGET/pom.xml" ]] || fail "$TARGET has no pom.xml; this factory is for Maven services"
[[ -f "$TARGET/CLAUDE.md" ]] || warn "$TARGET has no CLAUDE.md; the agents rely on it for the architecture rules"
python3 -c 'import sys; sys.exit(sys.version_info < (3, 10))' || fail "python3 3.10+ is required"
command -v mvn >/dev/null || fail "mvn is not on PATH"

ORIGIN="$(git -C "$TARGET" remote get-url origin 2>/dev/null || true)"
[[ "$ORIGIN" == *github.com* ]] || warn "origin is '${ORIGIN:-missing}', not GitHub; the factory needs GitHub issues and PRs"
command -v gh >/dev/null || warn "gh (GitHub CLI) is not installed; the factory can't run without it"

BASE="$(git -C "$TARGET" symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null | sed 's#^origin/##' || true)"
[[ -n "$BASE" ]] || BASE="$(git -C "$TARGET" branch --show-current)"
[[ -n "$BASE" ]] || BASE=main

# ---- 1. Copy the kit -------------------------------------------------------------
cd "$KIT"
mapfile -t FILES < <(find . -type f ! -name '*.pyc' | sed 's#^\./##' | sort)
clashes=()
for f in "${FILES[@]}"; do
  [[ -e "$TARGET/$f" ]] && clashes+=("$f")
done
if (( ${#clashes[@]} )) && (( ! FORCE )); then
  printf 'install: these files already exist in the target:\n' >&2
  printf '  %s\n' "${clashes[@]}" >&2
  fail "rerun with --force to overwrite them"
fi
for f in "${FILES[@]}"; do
  mkdir -p "$TARGET/$(dirname "$f")"
  cp "$f" "$TARGET/$f"
done
chmod +x "$TARGET/factory/checks.sh" "$TARGET/factory/bin/job" "$TARGET/factory/bin/gate" "$TARGET/factory/bin/route"
echo "copied ${#FILES[@]} files"

# ---- 2. Adapt --------------------------------------------------------------------
python3 - "$TARGET/factory/config.json" "$BASE" <<'EOF'
import json, sys
path, base = sys.argv[1], sys.argv[2]
cfg = json.load(open(path))
cfg["base_branch"] = base
open(path, "w").write(json.dumps(cfg, indent=2) + "\n")
EOF
echo "base_branch: $BASE"

touch "$TARGET/.gitignore"
# Java .gitignores usually ignore `bin`, which would hide factory/bin/ from git.
for line in "/factory/jobs/" "/.worktrees/" "!/factory/bin/"; do
  grep -qxF "$line" "$TARGET/.gitignore" || echo "$line" >> "$TARGET/.gitignore"
done

if [[ -f "$TARGET/CLAUDE.md" ]] && ! grep -q '^## Software factory' "$TARGET/CLAUDE.md"; then
  cat >> "$TARGET/CLAUDE.md" <<'EOF'

## Software factory

`/factory run` builds every open GitHub issue labeled `factory` through
intake → spec → build → gate → review → approve, one PR per issue. State is in
`factory/jobs/` (gitignored); `factory/bin/job` is its only writer. Checks:
`factory/checks.sh` (`mvn -o clean verify`). Dashboard:
`cd factory && python3 -m http.server`, then open
http://localhost:8000/dashboard.html.

Each job works in its own git worktree at `.worktrees/<job-id>` (gitignored).
Factory agents follow this file and the skills above; the factory's own
output formats replace "Agent behavior"'s `## TREBA ODLUKA` / `## VERDIKT`
blocks, and the operator's `/factory run` authorizes commits and pushes on
`factory/*` branches only.
EOF
  echo "CLAUDE.md: added the Software factory section"
fi

# ---- 3. Labels -------------------------------------------------------------------
if (( LABELS )); then
  (cd "$TARGET" &&
    gh label create factory --color 5319E7 --description "Build this with the software factory" --force &&
    gh label create factory:needs-info --color FBCA04 --description "The factory asked the author a question" --force &&
    gh label create factory:needs-human --color D93F0B --description "The factory needs a human decision on the PR" --force)
  echo "labels created"
fi

# ---- 4. Verify -------------------------------------------------------------------
python3 -m json.tool "$TARGET/factory/config.json" >/dev/null
(cd "$TARGET" && git check-ignore -q factory/jobs/x && git check-ignore -q .worktrees/x) || fail "gitignore entries aren't effective"
for f in "${FILES[@]}"; do
  if (cd "$TARGET" && git check-ignore -q "$f"); then
    fail "$f is gitignored in the target ($(cd "$TARGET" && git check-ignore -v "$f")); un-ignore it so it gets committed"
  fi
done
if (( VERIFY )); then
  echo "running factory/checks.sh in $TARGET ..."
  (cd "$TARGET" && bash factory/checks.sh) || fail "factory/checks.sh fails on the current branch; fix that before the first run"
  echo "checks.sh: PASS"
fi

cat <<EOF

Installed. Next:
  cd $TARGET
  git add .claude factory .gitignore CLAUDE.md && git commit -m "Install software factory"
  git push origin $BASE        # jobs branch from origin/$BASE, so the install must be there
Then label an issue 'factory' and run '/factory run' in Claude Code from $TARGET.
EOF
