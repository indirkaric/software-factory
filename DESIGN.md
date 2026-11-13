# Design

The pipeline, state machine and scripts come from zazencodes'
software-factory-claude-code-v2. Read its DESIGN.md for the reasoning behind
issues-as-queue, one orchestrator, scripts as the spine and PR-per-issue. This
file records what changed for a Java/Spring Boot backend built from
project-template.

## Unchanged

- `factory/bin/job`, `gate`, `route`: the transitions, the gate's protected
  paths and the main-checkout `checks.sh`, routing by risk and surfaces, merge
  through `gh pr merge`, cost from subagent transcripts.
- The orchestrator skill's flow and every agent's one-file / one-line contract.
- The dashboard.

## Changed

| Area | Original | Here | Why |
|---|---|---|---|
| Checks | placeholder filled at install | `mvn -o clean verify` | project-template's gate is tests + JaCoCo + checkstyle in one Maven run; offline like the template |
| Reviewers | code, security, ux, ui | code, api, db, integration, security | a backend has no pages; its risk sits in the REST contract, the schema and cross-service calls |
| Surfaces | `ui`, `security` | `api`, `db`, `integration`, `security` | they map one-to-one to template skills |
| Conventions | the agents' generic checklists | the agents read the service's CLAUDE.md, the layer skills, `code-reviewer.md` and `integration-flow-auditor.md` | one source of truth: change a rule in the template and the factory follows it |
| "Agent behavior" | n/a | the factory's formats replace `## TREBA ODLUKA` / `## VERDIKT`; `/factory run` authorizes commits on `factory/*` only | the template's interactive agents can ask the main session; factory agents report through files |
| Ports, browser | dev server + `agent-browser` per job | removed | persistence services need Postgres; verification is by unit tests |
| Approver escalations | generic | security/, non-additive Liquibase, `pom.xml` deps, CI/Docker, `platform.md`, breaking API | what a backend team must always see |
| Protected paths | factory files | + `src/main/resources/checkstyle/*` | a branch must not relax the rules it is judged by |
| Unknown model price | the job script dies | warns and leaves it out of the cost | a new model id shouldn't stop the pipeline |
| Install | agent copies from raw GitHub URLs | `install.sh` | deterministic, works with a private repo, fixes Java's `bin` gitignore |
| Defaults | concurrency 3, merge | concurrency 2, squash | Maven builds are heavier; squash keeps service history one commit per issue |

## Not handled

- New microservices: project-template's `/new-microservice` creates them; intake
  rejects such issues and says so.
- GitLab: the factory needs GitHub issues, labels and PRs (`gh`).
- Running the service: nothing is booted against Postgres or the local stack.
