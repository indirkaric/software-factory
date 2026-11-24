# Design

Pipeline, state machine, gate and routing come from zazencodes' software-factory-claude-code-v2
(GPLv3). This file records what this factory does differently and why.

## Principles

- **Scripts are the spine.** `factory/bin/job` is the only writer of `job.json` and the only
  thing that talks to GitHub; it refuses illegal moves. Agents write one file each and reply
  with one line, so the orchestrator's context lasts a whole run (token economy).
- **One reference.** `templates/java` is the only example agents copy infrastructure from;
  other services are never read as examples.
- **Nothing per service to install or commit.** The skill and agents live in `~/.claude`; the
  runtime (`factory/`) is attached on first use and hidden through `.git/info/exclude`.
- **Every feature runs before it merges.** Unit tests (gate), reviews, then the branch is
  booted against the development stack and called over HTTP (verify), then GitHub Actions.

## Changes from the earlier version of this repo

| Area | Before | Now | Why |
|---|---|---|---|
| Install | `install.sh` per service, committed | `software-factory setup` once per machine | simple install; services stay clean |
| Names | `/factory`, agents `intake`, `builder`… | `/software-factory`, `sf-*` agents | no clash with other factories or a service's own agents |
| New projects | separate project-template | `new <name> java [proxy]` / `dev` from `templates/` | one versioned unit |
| CLAUDE.md | assumed present | `sf-context` writes it when missing; every new project has one | agents read one file instead of exploring |
| PR | builder ran `gh pr create` | `job advance … gate` opens the draft PR | fewer tools in an agent's hands |
| Local check | none | `verify` stage: `sf-verifier` + `factory/bin/verify` | "every feature checked locally" |
| Merge | `gh pr merge` right away | waits for GitHub Actions (`ci_wait_minutes`), then squash | free private repos have no branch protection |
| Parallel sessions | one orchestrator | per-job claims (`job claim`, lease) and a serialized `job create` | several Claude Code sessions per repo |
| `run <n>` | issue had to carry the label | the label is added | the operator chose the issue |
| Gate | builds every time | reuses the builder hook's run on the same commit | one `mvn verify` per round |
| Headers | company-prefixed header names | `service-communication-id`, `correlation-id`, `user-id` | no company names |
| Package | company package | `com.indir.<x>` | |
| CI | GitLab | `.github/workflows/ci.yml` | `mvn verify` on every push; migrations tested |

## The verify stage

`factory/bin/verify <job> up` starts the development repo's `db auth aws` (never stops or wipes
anything), creates an empty database `sfv_<job>`, builds the worktree's jar and runs it on a
free port with the datasource pointed at that database and the JWT keys at the local Keycloak.
`sf-verifier` (Sonnet) calls the spec's `## Local check` as `admin`/`manager`/`user`, writes
`round-N/verify.md` (`VERIFY: PASS | FAIL | SKIP`, tied to the commit), and always runs `down`,
which stops the app and drops the database. FAIL goes back to the builder; SKIP (no Docker, no
development repo) escalates, because nothing merges unchecked.

## Permissions and the realm

The old development realm modelled a permission as a realm role, a client scope of the same
name (in the token's `scope`), a scope mapping and a default scope of the app client, with
users getting roles through groups. `templates/dev` keeps that model (`factory-realm.json`,
groups Administrator/Manager/Employee, `user_id`/`permission_groups` claims) and adds
`scripts/realm.py` to change it in the json and the running Keycloak at once. A service
declares its permissions in `permissions.txt`, checked against `Constants.ApiScopes` by a unit
test; `verify up` syncs it into the realm, `job merge` → `verify publish` commits it in the
development repo (under the shared lock, `pull --rebase`, push).

## Parallel runs

One shared infrastructure, per-job everything else: port from a leased range, database per
job, the jar from the job's worktree. All lease changes happen under one `flock`, so two
sessions never pick the same port or race on the realm json. Apps run in their own process
group and are killed by group; a lease whose pid died, whose job left `verify`, or that is
older than `verify.max_minutes` is collected by `verify gc`. Infrastructure the factory
started is stopped when the last lease goes; infrastructure the user started is left alone.
Rejected alternatives: per-job compose stacks (minutes per start, gigabytes of RAM),
rewriting ports in code (dirty diffs), killing whatever holds a port (it may be the user's).

## Liquibase: what we found and what the template does now

Found in identity, artifact and data-storage: changeset ids equal to the version
(`Author:0.0.1-SNAPSHOT`), so uniqueness hangs on file paths; `logicalFilePath` copy-paste
clashes; Windows `\` include paths; rollbacks like `DELETE FROM identity.permission` that wipe
whole tables; `CREATE SCHEMA` without `IF NOT EXISTS`; a `DROP TABLE ... CASCADE` at the top
of a changeset; no preconditions; migrations never run in unit tests.

The template now: `liquibase/changes/NNNN-<what>.sql`, explicit ordered includes, unique ids
`<service>:NNNN-<what>`, one exact rollback per changeset (never deleting more than it added),
idempotent DDL, seed data only with a context, additive-only changes without a human, and two
automated checks: CI runs `liquibase:updateTestingRollback` on an empty Postgres, and the
verify stage migrates an empty database from zero.

Suggestions for the existing services: move to unique changeset ids for new files (old ones
stay as they are, they have run); fix the table-wiping rollbacks; replace `\` in includes;
add the CI rollback test; for renames and drops use expand/contract over two releases.

## MCP

`setup` registers one user-scope MCP server, `github` (official image, through
`~/.config/software-factory/mcp-github.sh`, token taken from `gh` at start, never stored in
Claude's config). It is for interactive questions; the pipeline itself uses `gh` so every
state change stays scripted. New projects get **no** `.mcp.json`: user scope already covers
every folder, and a project file would duplicate it and add an approval prompt. Add a project
`.mcp.json` only for something project-specific (e.g. a database MCP for one service).

## Not handled

- GitLab, Jira: GitHub issues and PRs only (`gh`).
- Changes spanning several services in one job: intake builds this service's part and names
  the rest, to be filed as an issue in the other repo.
- End-to-end test suites: the development repo has none on purpose.
