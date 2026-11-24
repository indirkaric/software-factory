# software-factory

The factory itself: a Claude Code skill + agents that build GitHub issues of Java/Spring Boot
services into merged PRs, and templates for new projects. Talk to the user in Bosnian; code,
docs and commits in English (the Bosnian docs are README.bs.md and VODIC.md).

## Layout

- `software-factory`: the CLI (setup, update, new, doctor, attach). `setup.sh`,
  `new-service.sh`, `lib/attach.sh`, `lib/doctor.sh` do the work.
- `kit/.claude/skills/software-factory/SKILL.md`: the orchestrator. `kit/.claude/agents/sf-*.md`:
  one agent per step. Installed into `~/.claude` by setup/update.
- `kit/factory/`: copied into each service on attach: `bin/job` (state machine, GitHub),
  `bin/gate`, `bin/route`, `bin/verify` (local check), `checks.sh`, `config.json`, dashboard.
- `templates/java/`: skeleton (placeholders `{{...}}`, `ROOT` = root package), CLAUDE.md,
  skills, `platform.md` registry, `scripts/new_service.py`. `templates/dev/`: development repo.

## Rules

- Agents keep their one-file / one-line-reply contracts; scripts parse those first lines.
- Markdown files stay under 200 lines. No former-employer names anywhere (package `com.indir`).
- After changing `templates/java/skeleton`, generate both roles and run `mvn verify`
  (`templates/java/README.md`). After changing `kit/factory/bin/*`, `python3 -m py_compile`.
- `platform.md` is changed by `new` only (it commits and pushes it).
