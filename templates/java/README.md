# Java service template

The starting point of every Java service the software factory creates
(`software-factory new <name> java [proxy]`) and the one reference its agents copy
infrastructure from. Spring Boot 3.2, Java 17, Maven, package `<BASE_PACKAGE>.<name>` (default `com.<user>`).

| Path | What | In a new service |
|---|---|---|
| `skeleton/` | persistence role: infrastructure code + tests, `pom.xml`, `Dockerfile`, `docker-compose*.yml`, `permissions.txt`, `.github/workflows/ci.yml` | yes, filled |
| `proxy-skeleton/` | proxy role: the same infrastructure without a database, plus `ClientResponseWrapper`, `ResponseUtil`, a proxied reference module and its own `CLAUDE.md` | yes, filled |
| `CLAUDE.md` | architecture rules of a persistence service, always loaded by Claude Code | yes (persistence), + service facts |
| `.claude/skills/*` | one skill per layer, module checklists, storage, permissions | yes |
| `.claude/skills/inter-service-propagation/platform.md` | registry snapshot (the live one: `<development repo>/platform.md`); empty here | yes (snapshot) |
| `.claude/agents/*` | interactive helpers (creators, test writer, reviewers) | yes |
| `scripts/new_service.py` | fills the placeholders, wires `--calls`, registers the port | no |
| `REFERENCE.md` | where the skeleton came from and what was changed on purpose | no |

A `persistence` service also gets a complete reference module for `--domain`: entity, Liquibase
table, Criteria API repository with a filtered, sorted, paged list (`PagedList`, projections),
validators, create/modify/delete/read services, MapStruct mapper, response creators, controller
with request normalization (`@Trimmed`, `@UppercaseList`), and a unit test for each, including a
MockMvc test of every status. Every later module is copied from it (`new-crud-module`).

Roles, one skeleton each: `persistence` (`skeleton/`: own Postgres database, Liquibase, JPA,
read/write routing) and `proxy` (`proxy-skeleton/`: no database; forwards to and composes other
services, like a UI backend).

A `proxy` gets a proxied reference module for `--domain`, forwarding to the first `--calls`
service (or a placeholder `backend`): `client/<downstream>/<Domain>Client extends BaseClient`
returns a `ClientResponseWrapper` (status and body as answered, `null` when unreachable) →
`service/<downstream>/<domain>/` one service per verb, `ResponseUtil.handleResponse` maps the body
or throws with the downstream's status (400/403/404/409/422, 503 when unreachable) →
`modules/<downstream>/<domain>/<Domain>Controller`. Its endpoints mirror the persistence reference
module, so a proxy over a generated service works as generated. Extra placeholders:
`{{downstream}}`, `{{Downstream}}`, `{{downstream_package}}`, `{{DOWNSTREAM}}`.

## Headers between services

| Header | Filter | Meaning |
|---|---|---|
| `service-communication-id` | `ServiceCommunicationIdFilter` | caller id `<Caller>.<Callee>`; only `TRUSTED_CLIENTS` get authorities |
| `correlation-id` | `CorrelationIdFilter` | read or generated per request, forwarded on every outbound call |
| `user-id` | `UserIdFilter` | base64 JSON `{"userId"}` of the user a service acts for |

Users authenticate with a Keycloak JWT (realm `factory`); scopes and realm roles both become
`SCOPE_<NAME>` authorities (`KeycloakRolesConverter`).

## Changing the skeleton

Edit `skeleton/` or `proxy-skeleton/` (shared infrastructure — security, `WebClientConfig`, `Check`,
exceptions, checkstyle — in both), then prove both roles still build:

```bash
python3 scripts/new_service.py --name sandbox --role persistence --domain Demo --no-register --target /tmp/sandbox && (cd /tmp/sandbox && mvn verify)
```

Repeat with `--role proxy --domain Demo` (with and without `--calls`), note the change in `REFERENCE.md`, commit and push the factory
repo; other machines get it with `software-factory update`.
