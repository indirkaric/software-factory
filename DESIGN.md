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
| New projects | separate project-template | `new <name> java [proxy]` / `dev` from `templates/` (skeleton or proxy-skeleton) | one versioned unit |
| CLAUDE.md | assumed present | `sf-context` writes it when missing; every new project has one | agents read one file instead of exploring |
| PR | builder ran `gh pr create` | `job advance … gate` opens the draft PR | fewer tools in an agent's hands |
| Local check | none | `verify` stage: `sf-verifier` + `factory/bin/verify` | "every feature checked locally" |
| Merge | `gh pr merge` right away | waits for GitHub Actions (`ci_wait_minutes`), then squash | free private repos have no branch protection |
| Parallel sessions | one orchestrator | per-job claims (`job claim`, lease) and a serialized `job create` | several Claude Code sessions per repo |
| `run <n>` | issue had to carry the label | the label is added | the operator chose the issue |
| Gate | builds every time | reuses the builder hook's run on the same commit | one `mvn verify` per round |
| Headers | company-prefixed header names | `service-communication-id`, `correlation-id`, `user-id` | no company names |
| Package | company package | `<BASE_PACKAGE>.<x>`, default `com.<user>` | nobody's name is built in |
| Registry | in the factory repo, pushed by `new` | `<development repo>/platform.md` | the factory repo stays read-only for its users |
| CI | GitLab | `.github/workflows/ci.yml` | `mvn verify` on every push; migrations tested |

## The verify stage

`factory/bin/verify <job> up` starts the development repo's `db auth aws` (never wipes
anything), and for every service of the job's task creates an empty database, builds the
worktree's jar and runs it on a free port with the datasource pointed at that database and
the JWT keys at the local Keycloak. `sf-verifier` (Sonnet) calls each spec's `## Local check`
as `admin`/`manager`/`user`/`basic`, runs `verify smoke`, writes the report `up` named
(`VERIFY: PASS | FAIL | SKIP`); `job task-result` turns it into each job's
`round-N/verify.md`, tied to the commit. `down` stops the apps and drops the databases. FAIL goes back to the builder; SKIP (no Docker, no
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

## Tasks across services

A feature that needs another service becomes a task: the root job plus one job per other
service (`job link`, only after the user says yes), each in its own repo and branch, with
`task.json`/`task.md` in the root job's folder. Jobs run their own pipeline up to `verify`
and wait there for each other; one `sf-verifier` then checks the task: every branch booted
and wired (`client.<name>.base-uri` → the other app's run port), callers of the changed
services booted from their base branch, the flow, and a smoke pass (parameterless GETs from
`/v3/api-docs`, 5xx = broken). `job task-result` sends only the `FAULT` services back to build.
`job merge` refuses until every job is approved, and merges callees before callers.
Rejected: one job editing several repos (one branch and PR per repo is what reviewers and
GitHub understand), and checking each service alone (it misses contract breaks).

## Reference project

`--ref <path>` (per service on `new`, per job on `run`) names a local project as an example,
for example an older service that already does something similar. It bends "one reference"
only for the feature: the spec writer reads it and lists the relevant files as `reference`
lines; the builder reads only those. Infrastructure still comes from the skeleton, the
service's CLAUDE.md wins, and no name, package, header or URL is carried over.

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
- End-to-end test suites: the development repo has none on purpose.
