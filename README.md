# Software Factory for Java Backends

> 🇧🇦 Bosanski: [README.bs.md](README.bs.md)

A software factory for Claude Code that works through the GitHub issues of a
Spring Boot service. Label an issue `factory` and run `/factory run`. Agents
then decide whether the issue is worth building, write a spec, write the code
and tests following your service's conventions, run `mvn verify`, review the
change, and open a pull request that closes the issue. The approver merges the
PRs it's sure about and hands the rest to you.

It's based on [zazencodes software-factory-claude-code-v2](https://github.com/zazencodes/zazencodes-season-3/tree/main/src/software-factory-claude-code-v2)
(GPLv3), adapted for Java/Spring Boot backends. It's built for services
generated from a layered `project-template`: a `CLAUDE.md` with the
architecture rules, plus one skill per layer.

## How it fits together

```
project-template  ──/new-microservice──▶  new service (../invoice)
                                            │  git init + GitHub repo
                                            ▼
software-factory  ──./install.sh──────▶  service + factory
                                            │  issue labeled `factory`
                                            ▼
                                        /factory run → PR → merge
```

- **project-template** creates a *new service*: the skeleton, the
  infrastructure and the first module.
- **software-factory** builds *features in an existing service*, one issue at
  a time.

The factory agents don't invent architecture. The builder and reviewers read
the service's `CLAUDE.md` and its layer skills (`controller-conventions`,
`service-layer-conventions`, `entity-and-liquibase-conventions`,
`testing-conventions`, …). They also read the service's own
`code-reviewer.md` and `integration-flow-auditor.md` checklists when those
exist. Change a rule in the template and the factory follows it.

## Pipeline

```
issue labeled `factory`
        │
        ▼
intake (fable) ── NEEDS-INFO → asks the author on the issue, waits for a reply
        │         REJECT     → says why on the issue, removes the label
        ▼ ACCEPT (maybe rescoped); rates risk + surfaces (api, db, integration, security)
route (script)    picks reviewers and models from config.json
        ▼
spec-writer (opus) → spec.md: endpoints, tables, classes per layer, acceptance criteria
        ▼
builder (opus) → code + tests, commit, push, draft PR "Closes #n"
        ▼            ▲
gate (script) ─FAIL──┤  factory/checks.sh = mvn -o clean verify (tests, JaCoCo, checkstyle)
        ▼ PASS       │
reviewers ─CHANGES───┘  code always; + api / db / integration / security by surface
        ▼ all PASS
approver (opus) ── APPROVE  → sync with main, gate again, gh pr merge
                └─ ESCALATE → PR ready for you, issue labeled factory:needs-human
```

| Reviewer | Runs when | Model | Looks at |
|---|---|---|---|
| `code` | always | haiku (low risk) / sonnet | `Check` calls, `final`, layering, one interface per operation, tests, Javadoc |
| `api` | surface `api` | sonnet | endpoints, status codes, `@Valid`, OpenAPI, backward compatibility |
| `db` | surface `db` | sonnet | Liquibase (additive only), entity ↔ schema, indexes, N+1, `readOnly` |
| `integration` | surface `integration` | sonnet | `BaseClient`, `Sqa-*` headers, `platform.md`, filters, events |
| `security` | surface `security` | opus | `@PreAuthorize`, ownership checks, input reaching SQL, data leaks, secrets |

The approver **always escalates** these changes to a human:

- changes to `security/`;
- non-additive Liquibase migrations;
- new dependencies in `pom.xml`;
- CI, Docker or deploy changes;
- changes that need another service to change (`platform.md`);
- breaking API changes.

## Requirements

- [Claude Code](https://claude.com/claude-code) 2.1.257 or later (intake runs on Fable).
- `python3` 3.10+, Java 17 and Maven.
- [GitHub CLI](https://cli.github.com/), authenticated with push and label rights:

  ```bash
  gh auth login
  ```

- The service is a **git repo with a GitHub `origin`**, and its default branch
  is pushed. Services generated from the template start without git:

  ```bash
  git init -b main && git add -A && git commit -m "Initial service"
  ```

  ```bash
  gh repo create <owner>/<service> --private --source . --push
  ```

## Install into a service

Clone the factory once:

```bash
git clone https://github.com/indirkaric/software-factory.git
```

Install it into a service:

```bash
./software-factory/install.sh <path-to-service> --labels --verify
```

The installer:

- copies `kit/` into the service: `.claude/skills/factory`, nine agents and `factory/`;
- sets `base_branch` from the service's `origin`;
- adds `/factory/jobs/`, `/.worktrees/` and `!/factory/bin/` to `.gitignore`
  (Java `.gitignore` files usually ignore `bin`);
- appends a "Software factory" section to the service's `CLAUDE.md`;
- with `--labels`, creates the `factory`, `factory:needs-info` and
  `factory:needs-human` labels;
- with `--verify`, runs `factory/checks.sh`. It must pass on the default
  branch, or no job can ever pass the gate.

It refuses to overwrite existing files unless you pass `--force`. It never
commits, so commit and push the install yourself:

```bash
git add .claude factory .gitignore CLAUDE.md && git commit -m "Install software factory" && git push
```

The push is required, because every job branches from `origin/<base_branch>`.

You can also let Claude do the install. Open Claude Code in the service and say:
*"Install the software factory from `<path>/software-factory` following INSTALL.md"*.

## Usage

Open Claude Code **in the service's root**, not in this repo.

**1. File an issue** on GitHub with the `factory` label, for example:

> **Add status to invoices**
> An invoice has a status: DRAFT, SENT or PAID. New endpoint
> `PATCH /invoice/{id}/status`. Only DRAFT → SENT → PAID is allowed.

Or file it from Claude Code with `/factory add a status to invoices: DRAFT,
SENT, PAID…`. The factory opens the issue and runs it.

**2. Run the factory:**

```
/factory run          every open `factory` issue, `concurrency` at a time; also resumes
/factory run 12       only issue #12
/factory <anything>   questions, feedback, a new feature
```

**3. Watch it.** The session prints one line per step, for example
`001-add-status #12: gate PASS → review (code, api, db)`. To open the
dashboard, run:

```bash
cd factory && python3 -m http.server
```

Then open <http://localhost:8000/dashboard.html>. Each job is a card that
shows its gate and review results per round, its cost and its time. Click a
card to read every file the job wrote.

**4. Decide.** Every job ends as a PR:

- **merged**: the approver was sure, and the factory merged the PR.
- **factory:needs-human**: the PR waits for you, with a comment that says
  exactly what to decide. Approve it by **merging it on GitHub**. To ask for
  changes, tell the factory, for example `/factory for #12: PAID must never go
  back, add a test`. Your words go to the builder as `feedback-1.md`, and a new
  round starts.
- **factory:needs-info**: intake asked the author a question on the issue.
  Answer it in an issue comment (or with `/factory answer for #12: …`), then
  run `/factory run` again.
- To drop a job: `/factory reject #12, we'll do it differently`.

**The factory never** creates new microservices (that's the template's
`/new-microservice`), touches other repos, runs `docker`, deploys, or pushes
anything except `factory/*` branches.

## Configuration: `factory/config.json`

| Key | Default | Meaning |
|---|---|---|
| `concurrency` | 2 | jobs running at once (start with 1) |
| `max_rounds` | 3 | build → gate → review rounds before it asks you |
| `merge_method` | squash | must be allowed in the repo's settings |
| `routing` | low → haiku, medium/high → sonnet | code reviewer model by risk |
| `surface_reviewers` | api/db/integration → sonnet, security → opus | extra reviewers per surface |
| `protected_paths` | factory files + checkstyle config | the gate fails a branch that modifies them |

`checks.sh` runs Maven **offline** (`-o`), like the template does. If a job
adds a dependency that isn't in `~/.m2` yet, start the session with
`FACTORY_MVN_ONLINE=1`.

## Repository layout

```
install.sh                       installs the kit into a service
INSTALL.md                       instructions for an installing agent
DESIGN.md                        how it works and what changed from the original
kit/                             what gets copied into a service
  .claude/skills/factory/SKILL.md   the orchestrator (/factory)
  .claude/agents/
    intake.md               fable    worth building? at what scope? risk + surfaces
    spec-writer.md          opus     spec.md, layer by layer
    builder.md              opus     code, tests, commits, PR, build.md
    reviewer-code.md        routed   conventions, correctness, tests
    reviewer-api.md         sonnet   REST contract
    reviewer-db.md          sonnet   Liquibase, JPA, transactions
    reviewer-integration.md sonnet   other services, Sqa-* headers
    reviewer-security.md    opus     authorization, input, secrets
    approver.md             opus     APPROVE or ESCALATE
  factory/
    config.json  checks.sh  dashboard.html
    bin/job      the only writer of job.json; refuses illegal moves; merges
    bin/gate     runs checks.sh in a job's worktree → round-N/checks.md
    bin/route    risk + surfaces → the job's reviewers
```

## Using it with a different project template

The agents expect a `CLAUDE.md` and layer skills under `.claude/skills/`. The
skill names they look for are the ones listed above. If your services use
other names or rules:

- edit the skill lists in `kit/.claude/agents/*.md`;
- edit the checklists in `reviewer-*.md`;
- edit the ESCALATE list in `approver.md`.

If your build isn't `mvn verify`, change `kit/factory/checks.sh`. Keep each
agent's file names, first-line formats and one-line replies: the scripts
depend on them.

## Cost and safety

- The factory opens and merges real PRs and runs on your Claude plan. Start
  with `concurrency: 1` and read every PR until you trust it.
- Intake runs on Fable once per issue. Depending on your plan, Fable can bill
  to usage credits.
- Cost per job on the dashboard is the API-equivalent price, read from Claude
  Code's transcripts. On a subscription it shows where the usage goes, not
  what you're billed.
- Only issues labeled `factory` run, and only people with triage or write
  access can add labels. Agents get the issue text as quoted data, never as
  instructions.

## License

[GPLv3](LICENSE), like the original.
