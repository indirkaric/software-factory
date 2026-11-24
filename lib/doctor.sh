#!/usr/bin/env bash
# Check the GitHub token and, given a repo, what the factory needs there.
#
#   lib/doctor.sh --token-only     who the token is, its kind and expiry, classic scopes
#   lib/doctor.sh <repo dir>       + per-repo probes: issues, PRs, admin, actions, workflows
#
# Probes don't change anything: reads, and a PATCH that writes back the value
# the repo already has. Missing permissions are listed with how to fix them.
set -uo pipefail

ok() { echo "    ok       $*"; }
bad() { echo "    MISSING  $*"; MISSING=1; }
MISSING=0

echo "==> token"
headers="$(gh api -i user 2>/dev/null)" || { echo "    gh is not logged in: gh auth login"; exit 1; }
login="$(gh api user --jq .login)"
kind="classic"; gh auth token | grep -q '^github_pat_' && kind="fine-grained"
expiry="$(sed -n 's/^[Gg]ithub-[Aa]uthentication-[Tt]oken-[Ee]xpiration: //p' <<<"$headers" | tr -d '\r')"
echo "    user $login, $kind token${expiry:+, expires $expiry}"
if [[ -n "$expiry" ]]; then
  days=$(( ( $(date -d "$expiry" +%s) - $(date +%s) ) / 86400 ))
  (( days < 14 )) && echo "    WARNING  the token expires in $days days: make a new one and run software-factory setup"
fi
if [[ "$kind" == classic ]]; then
  scopes="$(sed -n 's/^[Xx]-[Oo][Aa]uth-[Ss]copes: //p' <<<"$headers" | tr -d '\r')"
  for s in repo workflow; do [[ ",$scopes," == *", $s,"* || ",$scopes," == *",$s,"* ]] && ok "scope $s" || bad "scope $s"; done
fi
[[ "${1:-}" == --token-only ]] && { (( MISSING )) && echo "    see GITHUB-PERMISSIONS.md"; exit $MISSING; }

dir="$(cd "${1:-.}" && pwd)"
url="$(git -C "$dir" remote get-url origin 2>/dev/null)"
repo="$(sed -E 's#^(https://|git@)github.com[:/]##; s#\.git$##' <<<"$url")"
[[ "$repo" == */* && "$url" == *github.com* ]] || { echo "    $dir has no GitHub origin"; exit 1; }
echo "==> $repo"
probe() { gh api "$@" >/dev/null 2>&1; }
probe "repos/$repo/issues?per_page=1" && ok "issues: read" || bad "Issues: read and write"
if probe -X POST "repos/$repo/labels" -f name=sf-probe -f color=ededed; then
  probe -X DELETE "repos/$repo/labels/sf-probe"; ok "issues: write (labels, comments)"
else bad "Issues: write"; fi
probe "repos/$repo/pulls?per_page=1" && ok "pull requests: read" || bad "Pull requests: read and write"
probe "repos/$repo/actions/runs?per_page=1" && ok "actions: read (CI status)" || bad "Actions: read (the factory can't see CI; it then merges on the local gate)"
squash="$(gh api "repos/$repo" --jq .allow_squash_merge 2>/dev/null)"
if probe -X PATCH "repos/$repo" -F "allow_squash_merge=${squash:-true}"; then ok "administration: write (create repos, merge settings)"
else bad "Administration: read and write (needed by 'new' to create repos and turn on auto-merge)"; fi
probe "repos/$repo/contents/.github" -X GET && ok "contents: read" || echo "    note     no .github folder in $repo"
echo "    not probed: Contents write and Workflows write (a push tests them; both are needed)"
(( MISSING )) && echo "    see GITHUB-PERMISSIONS.md"
exit $MISSING
