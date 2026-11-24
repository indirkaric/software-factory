# Java service template

The starting point of every Java service the software factory creates
(`software-factory new <name> java [proxy]`) and the one reference its agents copy
infrastructure from. Spring Boot 3.2, Java 17, Maven, package `com.indir.<name>`.

| Path | What | In a new service |
|---|---|---|
| `skeleton/` | infrastructure code + tests, `pom.xml`, `Dockerfile`, `docker-compose*.yml`, `permissions.txt`, `.github/workflows/ci.yml` | yes, filled |
| `CLAUDE.md` | architecture rules, always loaded by Claude Code | yes, + service facts |
| `.claude/skills/*` | one skill per layer, module checklists, storage, permissions | yes |
| `.claude/skills/inter-service-propagation/platform.md` | registry: every service's host, port, base URI | yes (snapshot) |
| `.claude/agents/*` | interactive helpers (creators, test writer, reviewers) | yes |
| `scripts/new_service.py` | fills the placeholders, wires `--calls`, registers the port | no |
| `REFERENCE.md` | where the skeleton came from and what was changed on purpose | no |

Roles: `persistence` (own Postgres database, Liquibase, JPA, read/write routing) and `proxy`
(no database; composes other services, like a UI backend). Code between `@persistence`
markers is removed for a proxy.

## Headers between services

| Header | Filter | Meaning |
|---|---|---|
| `service-communication-id` | `ServiceCommunicationIdFilter` | caller id `<Caller>.<Callee>`; only `TRUSTED_CLIENTS` get authorities |
| `correlation-id` | `CorrelationIdFilter` | read or generated per request, forwarded on every outbound call |
| `user-id` | `UserIdFilter` | base64 JSON `{"userId"}` of the user a service acts for |

Users authenticate with a Keycloak JWT (realm `factory`); scopes and realm roles both become
`SCOPE_<NAME>` authorities (`KeycloakRolesConverter`).

## Changing the skeleton

Edit `skeleton/`, then prove both roles still build:

```bash
python3 scripts/new_service.py --name sandbox --role persistence --domain Demo --no-register --target /tmp/sandbox && (cd /tmp/sandbox && mvn verify)
```

Repeat with `--role proxy`, note the change in `REFERENCE.md`, commit and push the factory
repo; other machines get it with `software-factory update`.
