# Install the Java backend software factory

You are a coding agent installing this factory into the user's Java/Spring Boot
service. The factory's source is the folder the user pointed you at (a clone of
this repository). Work in the service's root. Talk to the user in Bosnian.

## 0. Check the service

Stop and tell the user if any of these fail:

- It's a git repo with an `origin` on GitHub, and `gh auth status` shows a user
  who can push to it and manage its labels. If `gh` is missing, tell the user to
  run `sudo apt install gh` and `gh auth login` themselves.
- Its default branch exists on `origin`.
- It has a `pom.xml` and a `CLAUDE.md` (a service generated from
  project-template has both).
- `claude --version` is 2.1.257 or later, `python3 --version` is 3.10 or later,
  and `mvn` is on the PATH.

## 1. Install

Run the installer from the factory folder. Without `--force` it refuses to
overwrite existing files; if it lists clashes, show them to the user and ask
before rerunning with `--force`.

```bash
<factory>/install.sh <service root> --labels --verify
```

`--verify` runs `factory/checks.sh` (`mvn -o clean verify`). If it fails on the
current branch, the factory can never pass a job: find out why (a missing
dependency in the offline cache, a failing test on main) and tell the user.
Don't edit `checks.sh` to make it pass unless the service genuinely builds
differently; then keep it non-interactive, non-rewriting and failing non-zero.

## 2. Adapt (only what the service needs)

- `factory/config.json`: `merge_method` must be one the repo allows
  (`gh api repos/{owner}/{repo} --jq '{allow_merge_commit, allow_squash_merge, allow_rebase_merge}'`).
  Leave the rest unless the user asks.
- `intake.md`: if the service's CLAUDE.md doesn't say what the service is for,
  add one line under **Decide** saying so.
- `approver.md`: add anything this service's team must always see to the
  ESCALATE list.

Keep every agent's structure, file names, first-line formats and one-line
replies. Agent frontmatter may hold only `name`, `description`, `tools`,
`model`, and `hooks` on the builder.

## 3. Verify

1. `factory/bin/job queue` runs without an error.
2. `git status --porcelain --untracked-files=all` lists `.claude/skills/factory/`,
   the nine factory agents and every file under `factory/` (including
   `factory/bin/`) as committable.

## 4. Hand over

Stay on the user's branch. Commit the install with a one-line message and show
the diff. Ask before pushing: jobs branch from `origin/<base_branch>`, so the
install must be there before the first run.

Then tell the user: label a small issue `factory` and run `/factory run`;
every job ends as a PR; merge one to approve it, or give feedback with
`/factory <text>`; start with `concurrency: 1`.
