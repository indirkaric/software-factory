#!/usr/bin/env bash
# Internal, run by `software-factory attach` (the /software-factory skill does
# that first): put the factory's runtime part into a service repo, or upgrade it.
#
#   lib/attach.sh <service repo> <stamp>
#
# Copies kit/factory/ to <repo>/factory/ keeping the repo's config.json choices,
# sets base_branch from origin, keeps factory/, .worktrees/ and CLAUDE.local.md
# out of git through .git/info/exclude (nothing to commit), creates the
# factory labels on GitHub, and downloads the Maven dependencies once.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="$(cd "$1" && pwd)"; STAMP="$2"
fail() { echo "attach: $*" >&2; exit 1; }
warn() { echo "attach: warning: $*" >&2; }

[[ "$(git -C "$TARGET" rev-parse --show-toplevel)" == "$TARGET" ]] || fail "$TARGET is not the root of its git repository"
[[ -f "$TARGET/pom.xml" ]] || fail "$TARGET has no pom.xml; the factory works on Maven services"
ORIGIN="$(git -C "$TARGET" remote get-url origin 2>/dev/null || true)"
[[ "$ORIGIN" == *github.com* ]] || warn "origin is '${ORIGIN:-missing}', not GitHub; the factory needs GitHub issues and PRs"

BASE="$(git -C "$TARGET" symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null | sed 's#^origin/##' || true)"
[[ -n "$BASE" ]] || BASE="$(git -C "$TARGET" branch --show-current)"
[[ -n "$BASE" ]] || BASE=main

KEEP=""; [[ -f "$TARGET/factory/config.json" ]] && KEEP="$(cat "$TARGET/factory/config.json")"
FIRST=1; [[ -e "$TARGET/factory/bin/job" ]] && FIRST=0
mkdir -p "$TARGET/factory"
rm -rf "$TARGET/factory/bin"
cp -R "$HERE/kit/factory/." "$TARGET/factory/"
find "$TARGET/factory/bin" -name '__pycache__' -prune -exec rm -rf {} +
chmod +x "$TARGET/factory/checks.sh" "$TARGET/factory/bin/"*

python3 - "$TARGET/factory/config.json" "$BASE" "$KEEP" <<'PY'
import json, sys
path, base, keep = sys.argv[1:4]
cfg = json.load(open(path))
if keep:
    old = json.loads(keep)
    for k in ("concurrency", "max_rounds", "auto_merge", "merge_method", "ci_wait_minutes",
              "lease_minutes", "routing", "surface_reviewers", "verify", "reference"):
        if k in old:
            cfg[k] = old[k]
    cfg["protected_paths"] = sorted(set(cfg["protected_paths"]) | set(old.get("protected_paths", [])))
cfg["base_branch"] = base
open(path, "w").write(json.dumps(cfg, indent=2) + "\n")
PY

EXCLUDE="$(git -C "$TARGET" rev-parse --git-path info/exclude)"
[[ "$EXCLUDE" = /* ]] || EXCLUDE="$TARGET/$EXCLUDE"
mkdir -p "$(dirname "$EXCLUDE")"; touch "$EXCLUDE"
for line in "/factory/" "/.worktrees/" "/CLAUDE.local.md"; do
  grep -qxF "$line" "$EXCLUDE" || echo "$line" >> "$EXCLUDE"
done

NOTES="$TARGET/CLAUDE.local.md"; touch "$NOTES"
grep -q '^## Software factory' "$NOTES" || cat >> "$NOTES" <<'NOTE'

## Software factory

`/software-factory run` builds the open GitHub issues labeled `factory`:
intake → spec → build → gate (`mvn -o clean verify`) → review → local verify →
approve → merge, one PR per issue on branch `factory/<job-id>`, each in its
own worktree `.worktrees/<job-id>`. State is in `factory/jobs/`;
`factory/bin/job` is its only writer. `factory/`, `.worktrees/` and this file
are in `.git/info/exclude`. The reference for infrastructure code is the
factory's skeleton at `~/.config/software-factory/templates/java/` (read-only;
`skeleton/`, or `proxy-skeleton/` for a proxy).
Factory agents report through their files; the operator's run authorizes
commits and pushes on the job's own branch only.
NOTE

(cd "$TARGET" && git check-ignore -q factory/jobs/x && git check-ignore -q .worktrees/x) || fail "the .git/info/exclude entries aren't effective"
if (( FIRST )); then
  if [[ "$ORIGIN" == *github.com* ]]; then
    (cd "$TARGET" &&
      gh label create factory --color 5319E7 --description "Build this with the software factory" --force >/dev/null &&
      gh label create factory:needs-info --color FBCA04 --description "The factory asked the author a question" --force >/dev/null &&
      gh label create factory:needs-human --color D93F0B --description "The factory needs a human decision" --force >/dev/null) \
      || warn "could not create the factory labels (token needs Issues: write)"
  fi
  echo "attach: first time here, downloading the Maven dependencies once..."
  (cd "$TARGET" && mvn -B -ntp -q dependency:go-offline >/dev/null 2>&1) \
    || warn "mvn dependency:go-offline failed; the offline gate may fail until 'mvn verify' ran once here"
fi
echo "$STAMP" > "$TARGET/factory/.stamp"
echo "factory $([[ $FIRST == 1 ]] && echo attached to || echo upgraded in) $TARGET (base $BASE; nothing to commit)"
