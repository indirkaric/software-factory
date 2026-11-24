# Software Factory — step-by-step guide

From an empty machine to the first PR the factory built and merged. Each step says **what**
you do and **why**. (Bosanski: [VODIC.md](VODIC.md))

## 0. What is what

| Part | Where | What it does |
|---|---|---|
| Claude Code | the `claude` command | runs the agents |
| software-factory | `~/software-factory` (this repo) | the `software-factory` command, templates, agents |
| development | `~/Desktop/projects/development` | local Postgres, Keycloak, LocalStack + `dev-*` aliases |
| a service (e.g. `invoice`) | `~/Desktop/projects/invoice` + its own GitHub repo | where the factory works issues |
| GitHub | github.com | code, issues, PRs, Actions (CI) |

## Phase A — once per machine

### A1. Tools

```bash
sudo apt install git gh maven python3 docker.io docker-compose-v2
```

Java 17 (e.g. `sdk install java 17-tem`) and Claude Code. **Why:** the factory builds with
Maven, talks to GitHub through `gh`, and checks every feature locally in Docker.

### A2. GitHub token

Make a fine-grained token with the table in [GITHUB-PERMISSIONS.md](GITHUB-PERMISSIONS.md)
(Administration, Contents, Pull requests, Issues, Workflows: read/write; Actions: read).
**Why:** the same token creates repos, pushes, opens PRs and merges them.

### A3. Clone and set up

```bash
git clone https://github.com/indirkaric/software-factory.git ~/software-factory
```

```bash
~/software-factory/software-factory setup
```

Answers: "Use it? [Y/n]" (keep the token you cloned with; `n` pastes a new one) and the
projects folder (Enter = `~/Desktop/projects`). Setup installs `/software-factory` and the
agents into Claude Code and the `github` MCP. **Restart Claude Code.** Check:

```bash
software-factory doctor ~/software-factory
```

## Phase B — the development repo (once)

In Claude Code (any folder): `/software-factory new development dev`. Then:

```bash
cd ~/Desktop/projects/development && ./scripts/install-aliases.sh
```

In a new terminal:

```bash
dev-hosts
```

```bash
dev-up
```

`dev-hosts` maps `*.factory.local` to 127.0.0.1 (sudo). `dev-up` starts Postgres (:5432),
Keycloak (:8080, admin/admin) and LocalStack (:4566). `dev-help` lists every command. Stop any
other stack that uses the same ports first, or move this one with `DB_PORT`, `AUTH_PORT`,
`AWS_PORT` in `development/.env`.

## Phase C — a new service

In Claude Code: `/software-factory new invoice java` (or `... java proxy` for a BFF without a
database; add `--ref <path>` to give every job a local project as example). It generates the
service (`<base package>.invoice`, port 50001+) with its `CLAUDE.md`,
runs `mvn verify`, creates the private repo and pushes `main` with auto-merge and squash on,
and registers the service in the development repo (compose, database, host alias, registry
`platform.md`). Run it locally with `dev-run invoice`, or `mvn spring-boot:run` from the IDE.

## Phase D — every day

1. `cd ~/Desktop/projects/invoice && claude`
2. Describe the work: `/software-factory add a status to invoices: DRAFT, SENT, PAID`. The
   factory drafts the issue, shows it, opens it on your yes and runs it. Or file the issue on
   GitHub yourself and run `/software-factory run 1` (or `/software-factory-run 1`).
3. Watch one line per step:
   `intake → spec → build → gate (mvn verify) → review → verify (local) → approve → merge`.
   `verify` boots your branch on the development stack (empty database, Liquibase from zero),
   takes a Keycloak token and calls the endpoints. The merge waits for GitHub Actions.
4. When it needs you:
   - `factory:needs-info`: a question on the issue; answer there or with
     `/software-factory answer for #3: ...`, then `/software-factory run`.
   - `factory:needs-human`: a question on the PR; merge it on GitHub to approve, or
     `/software-factory for #3: PAID never goes back, add a test` for another round.
   - Reject: `/software-factory reject #3, we'll do it differently`.
5. In parallel: open another Claude Code session in the same service and run another issue.
   Each job has its own worktree (`.worktrees/<job>`), branch (`factory/<job>`), and during
   the local check its own port and database; `factory/bin/verify ps` lists them.
   A new permission (`READ-USER`) goes in the service's `permissions.txt`; the factory adds it
   to `development`'s realm and commits it there after the merge (`dev-perm` lists them).
6. Another service has to change too: the factory asks "change `users` too, on a new
   branch?". On yes it opens an issue and a job there, tests both services together (plus
   the services that call them, from `main`) and merges the called one first.
7. Dashboard: `cd factory && python3 -m http.server` → http://localhost:8000/dashboard.html.

## An existing project

Open Claude Code in it and run `/software-factory run`. Without a `CLAUDE.md`, the factory
writes one from the code first (and asks before committing it). The repo needs a GitHub
`origin` and a `pom.xml`.

## Update

```bash
software-factory update
```

## Troubleshooting

| Symptom | Fix |
|---|---|
| `could not create ... Administration: write` | new token (A2), `software-factory setup`, answer `n` |
| `VERIFY: SKIP` | Docker isn't running or there's no development repo: `dev-up` |
| the gate fails "offline" | start the session with `FACTORY_MVN_ONLINE=1 claude` |
| port already in use | stop the other stack or change the port in `docker-compose.override.yml` |
| token about to expire | `software-factory doctor` warns 14 days ahead |
