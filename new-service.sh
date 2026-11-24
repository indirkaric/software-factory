#!/usr/bin/env bash
# Create a new project from the factory's templates and put it on GitHub.
# Usually run through `software-factory new` or `/software-factory new`.
#
#   ./new-service.sh --name invoice --kind java|proxy|dev [--domain Invoice] [--calls a,b]
#       [--port 50001] [--dir ~/Desktop/projects] [--ref <project path>] [--public] [--offline]
#
#   java     Spring Boot service with its own Postgres database
#   proxy    Spring Boot proxy/BFF, no database, calls other services
#   dev      development repo: docker compose with Postgres, Keycloak, LocalStack, aliases
#   --ref       optional: a local project the factory's agents read as an example in every
#               job of the new service (config.json "reference"; `job create --ref` overrides)
#   --offline   local only: no GitHub repo, no push, no registry change (for trials)
#
# java/proxy: generate from templates/java (package BASE_PACKAGE.<name>, default com.<user>), mvn verify, git init,
# private GitHub repo under the token's account (auto-merge and squash on, branches deleted on
# merge), push main, attach the factory, register the service in the development repo and in
# the registry: <development repo>/platform.md, or ~/.config/software-factory/platform.md
# until a development repo exists. The factory repo itself is never changed.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
usage() { sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }
fail() { echo "new: $*" >&2; exit 1; }
step() { echo; echo "==> $*"; }
cfg() { sed -n "s/^$1=//p" "$HOME/.config/software-factory/config.env" 2>/dev/null | tail -1 || true; }

NAME=""; KIND=""; DOMAIN=""; CALLS=""; PORT=""; REF=""; DIR="$(cfg PROJECTS_DIR)"; VIS="$(cfg REPO_VISIBILITY)"; ONLINE=1
while (( $# )); do
  case "$1" in
    --name) NAME="$2"; shift 2 ;;
    --kind) KIND="$2"; shift 2 ;;
    --domain) DOMAIN="$2"; shift 2 ;;
    --calls) CALLS="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --dir) DIR="$2"; shift 2 ;;
    --ref) REF="$2"; shift 2 ;;
    --public) VIS=public; shift ;;
    --offline) ONLINE=0; shift ;;
    -h|--help) usage 0 ;;
    *) fail "unknown argument: $1" ;;
  esac
done
[[ -n "$NAME" && -n "$KIND" ]] || usage 1
[[ "$NAME" =~ ^[a-z][a-z0-9]*(-[a-z0-9]+)*$ ]] || fail "name must be kebab-case: $NAME"
DIR="${DIR:-$HOME/Desktop/projects}"; VIS="${VIS:-private}"
mkdir -p "$DIR"; DIR="$(cd "$DIR" && pwd)"; TARGET="$DIR/$NAME"
[[ ! -e "$TARGET" ]] || fail "$TARGET already exists"
if [[ -n "$REF" ]]; then
  [[ "$KIND" != dev ]] || fail "--ref is for java services"
  REF="${REF/#\~/$HOME}"; [[ -d "$REF" ]] || fail "--ref $REF is not a folder"; REF="$(cd "$REF" && pwd)"
fi
CONF="$HOME/.config/software-factory"; TEMPLATE_REG="$HERE/templates/java/.claude/skills/inter-service-propagation/platform.md"
DEV_NAME="$(cfg DEV_REPO)"; DEV="$DIR/${DEV_NAME:-development}"
# The registry of this user's (team's) services: in the development repo once it exists.
if [[ -x "$DEV/scripts/add-service.sh" ]]; then REG="$DEV/platform.md"; else REG="$CONF/platform.md"; fi
if [[ ! -f "$REG" && "$KIND" != dev ]]; then
  if (( ! ONLINE )); then REG="$TEMPLATE_REG"   # read only: --offline doesn't register
  elif [[ "$REG" == "$DEV/platform.md" && -f "$CONF/platform.md" ]]; then mv "$CONF/platform.md" "$REG"
  else mkdir -p "$(dirname "$REG")"; cp "$TEMPLATE_REG" "$REG"; fi
fi
OWNER=""
if (( ONLINE )); then
  OWNER="$(gh api user --jq .login)" || fail "gh is not logged in: run software-factory setup"
  ! gh repo view "$OWNER/$NAME" >/dev/null 2>&1 || fail "GitHub repo $OWNER/$NAME already exists"
fi

publish() {  # create the GitHub repo from $TARGET and push main
  step "GitHub: $OWNER/$NAME ($VIS)"
  gh repo create "$OWNER/$NAME" "--$VIS" --source "$TARGET" --remote origin --push \
    || fail "could not create $OWNER/$NAME: the token needs Administration: write (GITHUB-PERMISSIONS.md)"
  gh api -X PATCH "repos/$OWNER/$NAME" -F allow_auto_merge=true -F delete_branch_on_merge=true \
    -F allow_squash_merge=true -F allow_merge_commit=false -F allow_rebase_merge=false >/dev/null \
    || echo "    warning: could not set the merge options (Administration: write)"
  git -C "$TARGET" remote set-head origin main >/dev/null 2>&1 || true
  echo "    https://github.com/$OWNER/$NAME"
}

if [[ "$KIND" == dev ]]; then
  step "development repo $TARGET from templates/dev"
  cp -R "$HERE/templates/dev" "$TARGET"
  # the registry so far (services made before the development repo) moves into it; then
  # every service in it gets its compose entry
  if [[ ! -f "$CONF/platform.md" ]]; then cp "$TEMPLATE_REG" "$TARGET/platform.md"
  elif (( ONLINE )); then mv "$CONF/platform.md" "$TARGET/platform.md"
  else cp "$CONF/platform.md" "$TARGET/platform.md"; fi
  python3 - "$TARGET/platform.md" <<'PY' |
import sys
for line in open(sys.argv[1]):
    c = [x.strip() for x in line.strip().strip("|").split("|")]
    if len(c) == 6 and c[2].isdigit():
        print(c[0], c[2], "db" if "persistence" in c[5] else "-")
PY
  while read -r svc port db; do
    "$TARGET/scripts/add-service.sh" "$svc" "$port" $([[ "$db" == db ]] && echo --db "${svc//-/_}") >/dev/null
    echo "    registered $svc ($port)"
  done
  git -C "$TARGET" init -q -b main && git -C "$TARGET" add -A && git -C "$TARGET" commit -q -m "Development stack from the software factory"
  (( ONLINE )) && publish
  if [[ "$NAME" != "$(cfg DEV_REPO)" && -f "$HOME/.config/software-factory/config.env" ]]; then
    python3 - "$CONF/config.env" "$NAME" <<'PY'
import re, sys
p, name = sys.argv[1:]
text = open(p).read()
open(p, "w").write(re.sub(r"(?m)^DEV_REPO=.*$", "DEV_REPO=" + name, text))
PY
    echo "    DEV_REPO=$NAME in ~/.config/software-factory/config.env"
  fi
  step "done"
  echo "next: cd $TARGET && ./scripts/install-aliases.sh && dev-hosts && dev-up   (dev-help lists the commands)"
  exit 0
fi

ROLE=persistence; [[ "$KIND" == proxy ]] && ROLE=proxy
DOMAIN="${DOMAIN:-$(python3 -c 'import sys; print("".join(w.capitalize() for w in sys.argv[1].split("-")))' "$NAME")}"
step "generate $NAME ($ROLE, domain $DOMAIN${CALLS:+, calls $CALLS})"
PKG="$(cfg BASE_PACKAGE)"
ARGS=(--name "$NAME" --role "$ROLE" --domain "$DOMAIN" --target "$TARGET" --registry "$REG")
[[ -n "$PKG" ]] && ARGS+=(--package-prefix "$PKG")   # unset: com.<your user name>
[[ -n "$CALLS" ]] && ARGS+=(--calls "$CALLS")
[[ -n "$PORT" ]] && ARGS+=(--port "$PORT")
(( ONLINE )) || ARGS+=(--no-register)
python3 "$HERE/templates/java/scripts/new_service.py" "${ARGS[@]}"
PORT="$(sed -n 's/^  port: \([0-9]*\)$/\1/p' "$TARGET/src/main/resources/application.yaml" | head -1)"

step "mvn verify (unit tests, coverage, checkstyle)"
(cd "$TARGET" && mvn -B -ntp -q verify) || fail "mvn verify fails on the new service; see the output above"

step "git"
git -C "$TARGET" init -q -b main
git -C "$TARGET" add -A
git -C "$TARGET" commit -q -m "Initial $NAME service from the software factory skeleton"
(( ONLINE )) && publish
"$HERE/software-factory" attach "$TARGET" >/dev/null && echo "    factory attached (labels, factory/ excluded from git)"
if [[ -n "$REF" ]]; then
  python3 - "$TARGET/factory/config.json" "$REF" <<'PY'
import json, sys
p, ref = sys.argv[1:]
cfg = json.load(open(p)); cfg["reference"] = ref
open(p, "w").write(json.dumps(cfg, indent=2) + "\n")
PY
  echo "    reference project for every job: $REF"
fi
if [[ -x "$DEV/scripts/add-service.sh" ]]; then
  step "development repo $DEV"
  if [[ "$ROLE" == persistence ]]; then "$DEV/scripts/add-service.sh" "$NAME" "$PORT" --db "${NAME//-/_}"
  else "$DEV/scripts/add-service.sh" "$NAME" "$PORT"; fi
  if (( ONLINE )) && [[ -z "$(git -C "$DEV" status --porcelain -- . ':!docker-compose.services.yml' ':!docker-mounts' ':!scripts/hosts.txt' ':!platform.md')" ]]; then
    git -C "$DEV" add docker-compose.services.yml docker-mounts scripts/hosts.txt platform.md
    git -C "$DEV" commit -q -m "Add $NAME" && (git -C "$DEV" push -q 2>/dev/null && echo "    committed and pushed" || echo "    committed (push it yourself)")
  else
    echo "    changed, not committed (the repo has other changes): review and commit it"
  fi
else
  echo "    no development repo at $DEV: run 'software-factory new development dev' (it imports every registered service)"
  echo "    registry: $REG"
fi

step "done"
echo "service: $TARGET (port $PORT, $ROLE)"
(( ONLINE )) && echo "github:  https://github.com/$OWNER/$NAME"
echo "next:    open Claude Code in $TARGET and run /software-factory <feature> or /software-factory run"
