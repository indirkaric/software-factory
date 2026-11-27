# CLAUDE.md — {{ServiceName}}

> **Fill these in** — only when setting this template up by hand; `scripts/new_service.py` fills
> them and removes this note: `{{ServiceName}}`, `{{service-name}}`, `{{Domain}}`/`{{domain}}`,
> `{{port}}`, `{{api-prefix}}`, `{{coverage}}`, `{{domain_package}}`, `{{domain-path}}`,
> `{{downstream}}`/`{{Downstream}}`/`{{downstream_package}}`/`{{DOWNSTREAM}}`.

Spring Boot 3.2 / Java 17 proxy (backend-for-frontend), one per repo. The architecture below is
**non-negotiable**; each layer's detail lives in one skill (table at the end), loaded only when
that layer is touched. The factory's proxy skeleton is the reference for anything not covered here;
deviate only after calling it out.

## Package layout

A proxy (backend-for-frontend) has **no database**: no entity, repository, mapper or Liquibase.
Every endpoint forwards to a downstream service through a client and hands back its answer. When
a feature needs persistence, the endpoint belongs in the service that owns the data; this repo
only exposes it. Every package has a `package-info.java`; packages are snake_case.

```
client/BaseClient                      base URI + service-communication-id per downstream, exchange()
client/response/ClientResponseWrapper  (statusCode, body) as answered; null = unreachable
client/<downstream>/<Resource>Client   one per downstream resource, only the methods used
client/<downstream>/model/             downstream-only shapes, when the module's models don't fit
util/ResponseUtil                      handleResponse(wrapper, Type.class, serviceName): body or exception
service/<downstream>/<domain>/I<Domain>Service (reads) ICreate|IModify|IDelete<Domain>Service + impl/
modules/<downstream>/<domain>/
  <Domain>Controller
  model/          Create|Modify<Domain>Request, <Domain>ListRequest (classes); responses (records)
dto/response/EntityCreatedResponse  dto/UserInfoDTO
annotations/@Trimmed @Uppercase @Lowercase @UppercaseList   util/CustomFieldProcessor applies them
util/Check  util/StringHelper  config/  security/  exception/  constants/  service/security/
```

**The reference module.** The service was generated with one complete proxied module,
`{{Domain}}` forwarded to `{{downstream}}` (`modules/{{downstream_package}}/{{domain_package}}/`,
`client/{{downstream_package}}/{{Domain}}Client`, `service/{{downstream_package}}/{{domain_package}}/`,
a test per class). It is the canonical flow:

```
GET    /{{api-prefix}}/v1/{{domain-path}}?ids&status&textSearch&page&size&sort
       Controller → I{{Domain}}Service → {{Domain}}Client.getList → ResponseUtil → 200 | 204
GET    /{id}   Controller → I{{Domain}}Service.get → client.get → 200 | 404
POST   /       Controller → ICreate…Service → client.create → 201 {"id"} | 400 | 422
PUT    /{id}   Controller → IModify…Service → client.modify → 204 | 400 | 404 | 422
DELETE /{id}   Controller → IDelete…Service → client.delete → 204 | 404 | 409 | 422
any            {{downstream}} unreachable, or 503 after the WebClient retries → 503
```

`ResponseUtil` keeps the downstream's status: 400 `BadRequestException`, 401/403
`AccessDeniedException`, 404 `EntityNotFoundException`, 409 `ConflictException`, 422
`BusinessException`, unreachable/503 `ServiceUnavailableException`, anything else a 500. A 2xx
without a body is `null`: a list service turns it into an empty page, a read into a 404.

Copy the module for every new proxied endpoint (`new-proxy-module`); rename or delete it once a
real one exists. A client method takes the module's request model (and `Pageable`), builds the
query with `queryParam` (every value fully encoded) and returns the wrapper; it never maps or
throws. A service never inspects a status itself, `ResponseUtil` does. Before adding a client
method, grep `client/`: re-exposing an endpoint that is already proxied is the most common wasted
change. Composing several downstreams is an operation service calling several clients, with the
partial-failure behaviour explicit (`proxy-bff-conventions`).

## Non-negotiable

- **`Check`** (`Check.isNotNull`/`isNotEmpty`) first in every constructor (per dependency) and
  every public method (per parameter) — controllers, services, validators, clients, filters.
  A missing call is a review blocker.
- **Nothing proxied is `final`**: controller methods (`@PreAuthorize`) are CGLIB-proxied;
  `final` breaks them on live traffic only.
- **Tests ship with the change**, one test class per class with logic; `mvn verify` (also in GitHub Actions on every push) runs them plus the JaCoCo gate
  (`{{coverage}}` instruction coverage) and checkstyle.
- **Checkstyle** (`src/main/resources/checkstyle/`, from identity — the stricter one): Javadoc
  on public members, `Designed for extension.` in the Javadoc of overridable public methods,
  `final` parameters, 60-line methods, 120-char lines, a blank line after a closing `}` unless
  `else`/`catch`/`finally`/`}` follows, no star imports, no magic numbers.
- **Comments** only for a non-obvious reason, never narrating the next line.

## Skills

| Skill | Covers |
|---|---|
| `service-layer-conventions` | operation services, one per verb (validators and mappers: persistence only) |
| `controller-conventions` | endpoints, status codes, response shapes |
| `exception-handling-conventions` | exception types and HTTP mapping |
| `testing-conventions` | test style and coverage |
| `inter-service-propagation` | auth filters, `IScopeContext`, outbound headers, `platform.md` registry |
| `proxy-bff-conventions` | clients, `ResponseUtil`, composing several downstreams |
| `domain-events-conventions` | optional Kafka pattern |
| `storage-conventions` | S3 / Azure Blob behind one interface, presigned URLs, LocalStack |
| `permissions-and-tokens` | authority codes, users/groups/permissions, personal access tokens |
| `new-proxy-module` | ordered checklist for a new proxied module |

## Agent behavior

- **Language**: everything said to the user is Bosnian; code, Javadoc, comments, commits English.
- **Subagents** don't inherit this file: every agent prompt starts by reading it plus the skill
  files it needs (`Read .claude/skills/<name>/SKILL.md` — subagents can't invoke skills).
- **Other services** are known only from `.claude/skills/inter-service-propagation/platform.md`.
  Never read a sibling repo; if the registry lacks something, ask.
- **Git/infra**: no commit, push, `docker` or stack changes unless the user asks (the software
  factory's `/software-factory run` is that ask, for its own job branch).
- **Local run**: the `development` repo (next to this one) provides Postgres, Keycloak (realm
  `factory`, users `admin`/`manager`/`user`/`basic`, password = name) and the services this one
  calls; `dev-up` starts them. `docker-compose.yml` runs this service in a container on that network.
- **Permissions**: a new one goes in `Constants.ApiScopes` and `permissions.txt` (see
  `permissions-and-tokens`). **Coverage**: JaCoCo ≥ 0.80 (instruction and line), never lower.
- **Markdown** files stay under 200 lines.
- **Subagents can't ask the user.** They end with one block and the main session acts on it:
  - `## TREBA ODLUKA` — blocked: the question, 2–3 options with consequences; nothing edited.
  - `## KLASE ZA TESTOVE` — every code-writing agent: `src/main/java/…/X.java — what to cover`.
  - `## VERDIKT: APPROVE | WARNING | BLOCK` — last line of every review agent (`BLOCK` = a
    non-negotiable rule broken, `WARNING` = style/Javadoc/missing test, `APPROVE` = clean).
