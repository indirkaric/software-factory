# Software Factory for Java backends

> 🇧🇦 Bosanski: [README.bs.md](README.bs.md) · step by step: [GUIDE.md](GUIDE.md) ·
> korak po korak: [VODIC.md](VODIC.md)

A software factory for [Claude Code](https://claude.com/claude-code). You file a GitHub issue;
agents decide whether it's worth building, write a spec, write the code and the unit tests,
run `mvn verify`, review the change, **boot it locally against Postgres, Keycloak and
LocalStack and call the endpoints**, and merge the pull request once GitHub Actions is green.
You decide only what the agents escalate.

It also **creates new projects** with one command:

```
/software-factory new identity java           Spring Boot service with its own Postgres database
/software-factory new ui-service java proxy   Spring Boot proxy/BFF: no database, calls other services
/software-factory new development dev         local stack: docker compose, Keycloak, LocalStack, Postgres, aliases
```

Every new project gets a `CLAUDE.md`, a private GitHub repo under your token's account, and
(java) a GitHub Actions pipeline that runs the unit tests on every push. Package `com.indir.<name>`.

## Install (once per machine)

You need `git`, `gh`, `python3` ≥ 3.10, Java 17, Maven, Docker and Claude Code.

```bash
git clone https://github.com/indirkaric/software-factory.git ~/software-factory
```

```bash
~/software-factory/software-factory setup
```

Setup checks the tools, keeps the GitHub token you cloned with (or asks for a new one),
checks its permissions ([GITHUB-PERMISSIONS.md](GITHUB-PERMISSIONS.md)), asks for your projects
folder (default `~/Desktop/projects`), and installs `/software-factory`, `/software-factory-run`
and the `sf-*` agents into Claude Code for every folder, plus the `github` MCP server. Restart
Claude Code. Later: `software-factory update` pulls and reinstalls.

Nothing is installed per service: the first `/software-factory` in a repo attaches it
(`factory/` is kept out of git) and writes a `CLAUDE.md` if the repo has none.

## Use

```
/software-factory new development dev   once: the local stack (then: dev-up)
/software-factory new invoice java      a service, registered in the development repo
/software-factory add a status to invoices: DRAFT, SENT, PAID   → opens an issue and builds it
/software-factory run                   every open issue labeled `factory`
/software-factory run 1                 issue #1 now (same as /software-factory-run 1)
/software-factory issues                open issues and their jobs
/software-factory context               (re)write CLAUDE.md from the code
```

Open several Claude Code sessions in the same repo and run different issues in each: every
job has its own git worktree and branch, and a session claims a job before it works on it.

## Pipeline

```
issue ─► intake ─► spec ─► build ─► gate ─► review ─► verify ─► approve ─► merge
         (opus)   (opus)  (opus)   mvn     code + api/db/  boots the   (opus)   waits for
                                   verify  integration/    branch on            GitHub Actions,
                                           security        the dev stack        squash-merges
```

At most 3 rounds; anything that fails the last round, or that the approver won't merge on
its own (security, non-additive migrations, new plugins, CI changes, breaking API), waits for
you as `factory:needs-human` with a precise question on the PR.

## What a new Java service has

Spring Boot 3.2, Java 17: layered packages (controller → response creator → operation service
→ validator → repository), `Check` guards, global exception handler, Keycloak JWT (realm roles
become `SCOPE_` authorities), service-to-service headers `service-communication-id`,
`correlation-id`, `user-id`, `BaseClient` over WebClient, read/write datasource routing,
audit entities, Liquibase, checkstyle, JaCoCo ≥ 80 % (instruction and line; the gate refuses
less), `Dockerfile`, `docker-compose.yml` + `docker-compose.override.yml`, `permissions.txt`
and `.github/workflows/ci.yml`
(`mvn verify`, plus a Liquibase update → rollback → update test for persistence services).
The conventions live in `templates/java/CLAUDE.md` and `.claude/skills/*`, including S3/Azure
storage and users/groups/permissions/personal access tokens.

## Permissions

A service lists the Keycloak permissions it checks in `permissions.txt` (`READ-USER
Manager,Employee`; a unit test fails when the code checks one that isn't listed). The factory
adds them to the development repo's realm (`factory-realm.json`: realm role + client scope +
group, like a real realm) during the local check, and commits that realm after the merge.
Projects live side by side in one folder (`~/Desktop/projects/<service>`, `.../development`).

## Features across services

When a feature needs another service to change too (`portal` needs a new endpoint in `users`),
the spec names it under `## Other services` and the factory **asks you**. On yes it opens an
issue and a job in `../users` on its own branch `factory/<id>`, and both jobs form one task.
The task is tested as a whole: when every job has passed its reviews, `verify` boots each
service from its branch, wires them to each other, also boots the services that call them
(from their `main`, so a breaking change shows), runs the feature's flow and a smoke pass of
every GET endpoint. A failure goes back only to the service at fault. Merges follow the task
order: the called service first, the caller last.

## Parallel sessions and ports

Every session can run its own job. Each job has its own worktree, branch, port (51000-51999,
passed as `--server.port`, never written into code) and database `sfv_<service>_<job>`; all
share one Postgres/Keycloak/LocalStack. Leases in `~/.cache/software-factory/run` make the
choice race-free. `verify down` (and `job advance`, and `verify gc` at the end of a run) stop
the app, free the port and drop the database; if the factory started the infra, the last one
stops it again. A foreign program on an infra port is never killed: move the stack with
`DB_PORT`/`AUTH_PORT`/`AWS_PORT` in `development/.env`. `factory/bin/verify ps` shows what runs.

## Repository layout

```
software-factory          the command: setup, update, new, doctor, attach
setup.sh new-service.sh   what it runs; lib/attach.sh, lib/doctor.sh
kit/.claude/              /software-factory skills and sf-* agents (installed into ~/.claude)
kit/factory/              runtime copied into a service: bin/job gate route verify, checks.sh, config.json, dashboard
kit/hooks/gate-hook       the builder's Stop hook
templates/java/           skeleton, CLAUDE.md, skills, registry (platform.md), generator
templates/dev/            development repo: compose, realm, LocalStack hooks, dev-* aliases
```

More: [DESIGN.md](DESIGN.md) (how it works, migrations advice, MCP), and the dashboard:
`cd factory && python3 -m http.server`, then http://localhost:8000/dashboard.html.

## License

[GPLv3](LICENSE). Based on zazencodes' software-factory-claude-code-v2.
