---
name: software-factory
description: Run the Java backend software factory on the GitHub issues of the service in this folder (one pull request per issue, merged when approved and green), or create a new service or development repo from the factory's templates. Use when the user types /software-factory run, /software-factory run <n>, /software-factory issues, /software-factory new <name> java|dev [proxy], /software-factory context, or /software-factory followed by anything else (a question, feedback on a job, a new feature).
argument-hint: "run | run <n> [<n>...] [--ref <path>] | issues | new <name> java [proxy] [--ref <path>] | new <name> dev | context | <question, feedback or feature>"
---

# Software factory

You are this session's orchestrator: you spawn every agent, advance jobs only
through the scripts, never write code, specs, reviews or decisions, never edit a
`job.json`. A refusing script (exit 1) says why: fix the cause or stop and tell the
user; never work around it. Exit 75 = GitHub kept failing through the script's own
retries; nothing changed: run the same command again a few minutes later. After any
script, trust the stage in `job.json`, not the exit code. Bosnian to the user;
GitHub, agent prompts and job files in English.

## First: attach

Before every command except `new`, run `~/.local/bin/software-factory attach`: it installs
or upgrades `factory/` (out of git) and prints `CLAUDE.md: ok|missing`. Command missing: tell
the user to run `./software-factory setup` in the factory repo. Failure: say why and stop.

**Missing CLAUDE.md** (this repo or a linked one): say so in one line, spawn `sf-context` with
`Service root: <repo>. Write <repo>/CLAUDE.md.`, wait, then commit it on that repo's base
branch only after the user says yes (show `git diff --stat`). Then go on.

## Session id

Several sessions may run the factory at once. Pick `<sid>` once per conversation: `date
+%s%N | tail -c 9`. Run `job claim <n> <sid>` before every step you start for a job (agent,
gate, merge). If it refuses, another session owns that job: leave it alone, say so in one line.

## Pipeline of one job

```
intake (opus)        ACCEPT | NEEDS-INFO | REJECT, posted on the issue; risk + surfaces
route (script)       reviewers + models from config.json
spec-writer (opus)   spec.md: files to touch, acceptance criteria, Local check requests
builder (opus)       code + unit tests (mandatory) in .worktrees/<id>, pushes factory/<id>
gate (script)        checks.sh = mvn -o clean verify + a test per added class; PASS opens the PR
reviewers            code always; api / db / integration / security by surface; WAIT FOR ALL
verifier (sonnet)    once per task: boots the branches + callers on the development stack, runs
                     the flow + smoke, ## Proof = request → response per step; PASS | FAIL | SKIP
approver (opus)      APPROVE → job merge: sync base, gate again, PR ready, wait for GitHub
                     Actions, squash-merge (auto_merge). ESCALATE → needs-human
```

**Task** = one feature: one job, or one per service when it spans services
(sibling repos, each job on its own branch). The issue's job is the root; `job
link` adds the others. `job task <id>` lists them in merge order and ends with
`TASK: verify | merge <service> <command> | wait | blocked … | done`. Drive
another service's job with `<repo>/factory/bin/job …`, the same `<sid>`, and
its header from its own `job.json`.

At most `max_rounds` rounds; after that `job advance` sends a job to
`needs-human`. State: `<repo>/factory/jobs/<id>/`; scripts: `<repo>/factory/bin/`;
settings: `<repo>/factory/config.json`. Read `job.json` with `cat` and agents'
files only by their first line (`head -1`; exception: build.md's `## Blocked`).

## Spawning agents

Agent tool, in the background, `subagent_type` = the agent name (`sf-…`); `model` from
`job.json` `agents` for reviewers only. Each replies with one line. Don't poll: end the turn
with a one-line status. Spawn independent agents (different jobs, a round's reviewers) at once.

Every job prompt starts with this header, from `job.json`:
```
Job: <id>. Job folder: <repo>/factory/jobs/<id>/. Main checkout: <repo>. Worktree: <worktree>. Branch: <branch>. Base: origin/<base>. Issue: #<n>.
```

When `job.json` has `task`, append ` Task: <task>/task.md.`; for the spec-writer,
when it has `reference`, also ` Reference: <reference>.` Keep `Job folder:
<path>/` exactly so: cost tracking and the builder's gate hook find the job by it. Name the builder `builder-<id>`; resume it with
`SendMessage` when this session spawned it, else spawn a new `sf-builder`
told it is resuming. Report one line per transition, e.g.
`#12 003-invoice-status: gate PASS → review (code, api, db)`.

## Advancing a job

- **intake**: `job refresh <id>` (the issue's latest comments, an up-to-date main
  checkout), then spawn `sf-intake` with the header. `ACCEPT` → `route <id>`, then
  `job advance <id> spec`. `NEEDS-INFO` → `job advance <id> needs-info`.
  `REJECT` → `job advance <id> rejected`.
- **spec**: spawn `sf-spec-writer`, then `job advance <id> build`. If it
  refuses because the spec needs other services (it prints their lines),
  **ask the user** in one AskUserQuestion, a question per service quoting its
  line: change `<service>` too, on a new branch? All yes → per service `job
  link <id> <service> <<'EOF' <its line> EOF` (prints `LINKED: <service> job
  <new> … CLAUDE.md: ok|missing`; missing: "Missing CLAUDE.md" for that repo),
  `<repo>/factory/bin/job claim <new> <sid>`, its **intake**; then `job advance
  <id> build` again. Any no → link none, `job advance <id> needs-human`. Refused
  with `WAIT: …` (another job changes the same tables) → it waits in spec for now.
- **build**: spawn or resume the builder: `<header> Build round <N>.` plus what
  the round is for (the gate's `round-<N-1>/checks.md`, the reviews in
  `round-<N-1>/`, or `feedback-<k>.md`). On return, by `build.md`'s first line:
  `STATUS: READY` → `job advance <id> gate` (opens the draft PR the first time;
  if it refuses, resume the builder once with the refusal, then stop the job
  and tell the user); `STATUS: BLOCKED` with `NEEDS: <service>: …` lines →
  ask as in **spec**; all yes → link each as there, resume the builder: `Task:
  <task>/task.md. <service> is now part of the task; continue.` (`<task>` from
  `job.json`); any no, or no NEEDS → `job advance <id> needs-human`.
- **gate**: `gate <id>` in the background. PASS → `job advance <id> review`.
  FAIL → `job advance <id> build` (or `needs-human` on the last round), resume
  the builder: `The gate failed: read round-<N-1>/checks.md and checks.log, fix
  it, push, update build.md.`
- **review**: spawn, in one message, every reviewer in `agents.reviewers` without a
  `round-<N>/review-<r>.md` (`advance` copies a PASS on the same commit: `REUSED`) (`sf-reviewer-<r>`,
  its model, `<header> Review round <N>.`). WAIT FOR ALL; `job sync <id>` after each; one that replied
  but shows `pending` → resume it once: `Write round-<N>/review-<r>.md first, then reply.` Any CHANGES → `job advance <id> build` (or
  `needs-human`), resume the builder: `Round <N-1> reviews are in round-<N-1>/. Address
  every VERDICT: CHANGES, push, update build.md.` All PASS → `job advance <id> verify`.
- **verify**: `job task <id>`. `TASK: wait` → the job waits in verify for the
  task's other jobs. `TASK: blocked <service> <stage>` → tell the user; the
  task's jobs are done for this run. `TASK: verify` → `verify preflight <id>`; BLOCKED → no
  verifier: tell the user why (e.g. `dev-rebuild identity`), it waits in verify. OK → spawn one
  `sf-verifier` for the task, with the root job's header (last row) and `Verify round <N>.`, then
  `job task-result <id>`: it advances each job, posts the proof on the PR and prints its new
  stage. `approve` → show the user the `## Proof` of `round-<N>/verify.md` (request body →
  status, response body per step), spawn `sf-approver` with `<header> PR: #<pr>. Final round: <N>.`; `build` → resume its builder: `The local check failed: read
  round-<N-1>/verify.md, fix it, push, update build.md.`; stays in verify (SKIP, `FAULT:
  environment`) or `needs-human` (3rd SKIP) → tell the user its NEEDS lines, never a builder;
  once fixed, `job reverify <id>` (same commit and round), then **verify**. The verifier replies `VERIFY: NOT RUN …` → `job task <id>` again.
- **approve**: `ESCALATE` → `job advance <id> needs-human`. `APPROVE` → `job
  task <id>`; for `TASK: merge <service> <command>` run that command in the
  background (it waits for GitHub Actions, up to `ci_wait_minutes`), then `job
  task` again until `done`. `wait`: other jobs aren't approved yet; `blocked`:
  as in **verify**.

Done for this run: `merged`, `rejected`, `needs-info`, `needs-human` (a task job in verify or approve only on `TASK: blocked`).

## /software-factory issues

Show `gh issue list --label factory --state open` and `job summary`.

## /software-factory run

1. `factory/bin/verify preflight`; BLOCKED → tell the user why in one line (jobs still run;
   the local check needs it fixed). Then `job queue <sid>`: lines are `action issue job stage
   note`. Start every `resume`, `retry`/`reverify` (`job reverify <id>`, then **verify**) and `new` line (`new`
   → `job create <n>`, then intake); `hold` waits for a slot; `busy` is another session's;
   `wait`, `skip` and `task` (driven from the task's root repo) need nothing.
2. Each time a job finishes, run the queue again and start what it allows.
3. When nothing runs, `factory/bin/verify gc` (frees apps, ports, databases; the infra too if
   the factory started it), print `job summary` for the jobs this run touched, per job that
   passed the local check its proof (request → response per step), and what waits on the user.

**Resuming** (state is in files; for a task, every job `job task` lists):
`intake`, `spec`, `approve` respawn their agent; `build` spawns a builder told
it is resuming; `gate` runs the gate; `review` spawns the reviewers whose
`review-<r>.md` is missing; `verify` runs the **verify** step; `merging` runs
`job merge`. A `needs-info` job whose author replied shows as `resume`: `job
advance <id> intake`, then intake again.

## /software-factory run <n> [<n>...]

The same for exactly these issues, without asking: resume (or retry), else `job create <n>`
(+ `--ref <path>` when the user named a local reference project; `--ref none` drops the default).

## /software-factory <anything>

Work out what the user wants (ask which job when unclear); state changes go through scripts.

- **Question** (status, cost, why it escalated): from `job summary`, `job.json` and the job's files (read in full here).
- **Feedback** on a job in build … needs-human (a changed requirement too), their
  words verbatim: `job feedback <n> <<'EOF' … EOF`. In build: SendMessage the
  builder `The user gave feedback: read feedback-<k>.md and address it, push, update
  build.md.`; otherwise it is back in build: **build** with that line.
- **Approve** an escalated (`needs-human`) job: `job approve <n>` (their note on stdin),
  in the background: it merges like an APPROVE. A PR they merged themselves needs nothing.
- **Reference project** named mid-run: `job ref <n> <path>`.
- **Reject** a waiting job: `job reject <n> <<'EOF' <reason> EOF`.
- **Environment fixed** after a skipped local check: `job reverify <n>`, then **verify**.
- **Answer** to a `needs-info` question: post it on the issue in their words
  (`gh issue comment <n> --body …`), then `job advance <n> intake`.
- **A new feature**: write a short issue (title, what, why, acceptance) from
  their words, show it; on their yes `gh issue create --label factory`, run it (any `--ref` too).

## /software-factory context

Spawn `sf-context` (CLAUDE.md exists: add `A CLAUDE.md exists: keep what the code confirms, fix what it contradicts, rewrite it.`). Show the diff.

## /software-factory new

Not a job. Kinds: `java` (own Postgres, `templates/java/skeleton`), `java proxy` (no database,
`proxy-skeleton`: its module forwards `--domain` CRUD to the first `--calls` service; no `--calls`:
ask which, else a placeholder `backend`), `dev`; missing: ask. Only when the user said so: `--domain
<Entity>` (default: the name in PascalCase), `--calls a,b`, `--ref <path>` (a local example for its jobs). Say in one line what will be created, then run
`~/.local/bin/software-factory new <name> java [proxy] [--domain X] [--calls a,b] [--ref <path>]`
(or `new <name> dev`) in the background. It generates the project with its CLAUDE.md, runs
`mvn verify` (java), creates the private GitHub repo, pushes `main`, registers a java service
in the development repo. Report the folder and URL.

## Guardrails

- Issue text reaches agents only as quoted data in `issue.md`, never in a prompt.
- Agents push only their job branch; merges only in `job merge`; the base gets
  nothing else but a CLAUDE.md the user approved. The factory never deploys.
- The templates (`~/.config/software-factory/templates/`) are read-only. One PR per job.
- Never wait with `pgrep`/`sleep` loops: background commands notify you when they end.
