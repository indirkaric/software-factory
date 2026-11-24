#!/usr/bin/env bash
# One-time setup on a machine (run through `./software-factory setup`).
#
#   ./setup.sh [--token-file <file>] [--projects <dir>] [--package com.example] [--no-mcp]
#
# - checks the tools (git, gh, python3, java 17, mvn, claude; docker for local checks)
# - GitHub token: keeps the one gh is logged in with (the one you cloned with), or
#   takes a new one (asked hidden, or --token-file) and logs gh in with it
# - checks what the token may do (see GITHUB-PERMISSIONS.md)
# - writes ~/.config/software-factory/config.env (no token in it: gh keeps the token): the
#   projects folder and the base package of new services (<package>.<service>)
# - registers the `github` MCP server for Claude Code (user scope; needs docker)
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOKEN_FILE=""; PROJECTS=""; PACKAGE=""; MCP=1
while (( $# )); do
  case "$1" in
    --token-file) TOKEN_FILE="$2"; shift 2 ;;
    --projects) PROJECTS="$2"; shift 2 ;;
    --package) PACKAGE="$2"; shift 2 ;;
    --no-mcp) MCP=0; shift ;;
    -h|--help) sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 1 ;;
  esac
done
fail() { echo "setup: $*" >&2; exit 1; }
CONF="$HOME/.config/software-factory"

echo "==> tools"
missing=()
need() { if [[ "$2" == 1 ]]; then echo "    ok       $1"; else echo "    MISSING  $1  -> $3"; missing+=("$1"); fi; }
has() { command -v "$1" >/dev/null 2>&1 && echo 1 || echo 0; }
need git "$(has git)" "sudo apt install git"
need "gh (GitHub CLI)" "$(has gh)" "https://cli.github.com (sudo apt install gh)"
need "python3 >= 3.10" "$(python3 -c 'import sys; print(int(sys.version_info >= (3, 10)))' 2>/dev/null || echo 0)" "install Python 3.10+"
java_major="$(java -version 2>&1 | sed -nE '1s/.*version "([0-9]+)(\.[0-9]+)*.*/\1/p')"
need "java >= 17" "$([[ -n "$java_major" && "$java_major" -ge 17 ]] && echo 1 || echo 0)" "sdk install java 17-tem (sdkman)"
need mvn "$(has mvn)" "sudo apt install maven"
need "claude (Claude Code)" "$(has claude)" "https://docs.claude.com/claude-code"
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  echo "    ok       docker compose"
else
  echo "    MISSING  docker compose  -> features can't be checked locally without it (Docker Engine / Desktop)"
  missing+=(docker)
fi
(( ${#missing[@]} == 0 )) || fail "install what is MISSING above, then run setup again"

echo "==> GitHub token"
current=""
gh auth status >/dev/null 2>&1 && current="$(gh api user --jq .login 2>/dev/null || true)"
use_new=0
if [[ -n "$TOKEN_FILE" ]]; then
  use_new=1
elif [[ -n "$current" ]]; then
  if [[ -t 0 ]]; then
    read -rp "    gh is logged in as '$current' (the token you cloned with). Use it? [Y/n] " a
    [[ "${a:-y}" =~ ^[Nn] ]] && use_new=1
  fi
else
  use_new=1
fi
if (( use_new )); then
  if [[ -n "$TOKEN_FILE" ]]; then
    token="$(grep -oE '(github_pat_|gh[pousr]_)[A-Za-z0-9_]+' "$TOKEN_FILE" | head -1)"
  else
    [[ -t 0 ]] || fail "no terminal to ask for the token: pass --token-file <file>"
    echo "    Make one at https://github.com/settings/personal-access-tokens (permissions: GITHUB-PERMISSIONS.md)"
    read -rsp "    GitHub token: " token; echo
  fi
  [[ -n "$token" ]] || fail "no token"
  printf '%s\n' "$token" | gh auth login --hostname github.com --git-protocol https --with-token || fail "gh rejected the token"
  unset token
fi
gh auth setup-git >/dev/null 2>&1 || true   # git push/pull over https use gh's token
LOGIN="$(gh api user --jq .login)" || fail "the token can't read your GitHub user"
echo "    user: $LOGIN (git pushes and new repos go to this account)"
"$HERE/lib/doctor.sh" --token-only || true

PROJECTS="${PROJECTS:-$(sed -n 's/^PROJECTS_DIR=//p' "$CONF/config.env" 2>/dev/null)}"
PROJECTS="${PROJECTS:-$HOME/Desktop/projects}"
if [[ -t 0 ]]; then read -rp "==> folder for your projects [$PROJECTS]: " p; PROJECTS="${p:-$PROJECTS}"; fi
PROJECTS="${PROJECTS/#\~/$HOME}"; mkdir -p "$PROJECTS"
DEV="$(sed -n 's/^DEV_REPO=//p' "$CONF/config.env" 2>/dev/null)"; DEV="${DEV:-development}"
PACKAGE="${PACKAGE:-$(sed -n 's/^BASE_PACKAGE=//p' "$CONF/config.env" 2>/dev/null)}"
# default: com.<machine user>, else com.<GitHub login> (the same rule as new_service.py)
PACKAGE="${PACKAGE:-$(python3 -B -c 'import sys; sys.path.insert(0, sys.argv[1]); import new_service as n; print(n.default_prefix(sys.argv[2]))' "$HERE/templates/java/scripts" "$LOGIN")}"
if [[ -t 0 ]]; then read -rp "==> base package of new services (<package>.<service>) [$PACKAGE]: " p; PACKAGE="${p:-$PACKAGE}"; fi
[[ "$PACKAGE" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$ ]] || fail "not a Java package: $PACKAGE"

mkdir -p "$CONF"
cat > "$CONF/config.env" <<EOF
# Written by software-factory setup. No secrets here: gh keeps the token.
GITHUB_OWNER=$LOGIN
PROJECTS_DIR=$PROJECTS
DEV_REPO=$DEV
BASE_PACKAGE=$PACKAGE
REPO_VISIBILITY=private
EOF
echo "==> wrote $CONF/config.env"

(( MCP )) || exit 0
cat > "$CONF/mcp-github.sh" <<'EOF'
#!/usr/bin/env bash
# GitHub MCP server for Claude Code, with gh's token (never stored in Claude's config).
set -euo pipefail
export GITHUB_PERSONAL_ACCESS_TOKEN="$(gh auth token)"
exec docker run -i --rm -e GITHUB_PERSONAL_ACCESS_TOKEN -e GITHUB_TOOLSETS=repos,issues,pull_requests,actions \
  ghcr.io/github/github-mcp-server
EOF
chmod 700 "$CONF/mcp-github.sh"
claude mcp remove -s user github >/dev/null 2>&1 || true
claude mcp add -s user github -- "$CONF/mcp-github.sh" >/dev/null && echo "==> MCP 'github' registered (user scope, every project)"
