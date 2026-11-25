# CLAUDE.md — {{ServiceName}}

> **Fill these in** — only when setting this template up by hand; `scripts/new_service.py` fills
> them and removes this note: `{{ServiceName}}`, `{{service-name}}`, `{{Domain}}`/`{{domain}}`,
> `{{port}}`, `{{schema}}`, `{{api-prefix}}`, `{{coverage}}`, `{{domain_package}}`,
> `{{domain-path}}`.

Spring Boot 3.2 / Java 17 microservice, one per repo. The architecture below is
**non-negotiable**; each layer's detail lives in one skill (table at the end), loaded only when
that layer is touched. The factory's skeleton is the reference for anything not covered here;
deviate only after calling it out.

## Package layout

Entities, repositories, mappers and DTOs are root-level and shared; a module holds exactly three
things. Every package has a `package-info.java`; domain packages are snake_case
(`purchase_order`). Spell `config/` and `annotations/` this way.

```
entity/<Entity>  entity/common/(Internal|Audit|External)Entity  entity/listener/BaseEntityListener
repository/I<Domain>Repository  impl/<Domain>Repository (Criteria API + <Entity>_ metamodel)
repository/projection/<Domain>Projection, <Domain>ItemProjection     records
mapper/I<Domain>Mapper                         MapStruct: projection → dto → model
dto/<Domain>DTO  dto/response/EntityCreatedResponse
service/<domain_package>/I<Domain>Service (reads) ICreate|IModify|IDelete<Domain>Service + impl/
service/validator/I<Verb><Domain>ServiceValidator + impl/   + Base<Domain>DataValidator
service/model/PagedList<T>(items, count)  <Domain>ListFilter
service/generator/IEntityIdGeneratorService    public entity ids
service/security/  service/bean/  service/audit/
annotations/@Trimmed @Uppercase @Lowercase @UppercaseList   util/CustomFieldProcessor applies them
util/Check  util/SortHelper (orders, LIKE escaping)  util/StringHelper
client/BaseClient  client/<downstream>/<Downstream>Client
config/  security/  exception/  constants/  db/config|model|constants
modules/<domain_package>/
  <Domain>Controller
  model/          Create|Modify<Domain>Request, <Domain>ListRequest (classes); responses (records)
  service/I<Domain>ResponseCreatorService, I<Domain>ListResponseCreatorService + impl/
event/                                         optional, see domain-events-conventions
```

**The reference module.** The service was generated with one complete module for its first
domain, `{{Domain}}` (`modules/{{domain_package}}/` and everything under it at the root, with a
test per class). It is the canonical flow, controller to repository:

```
GET    /{{api-prefix}}/v1/{{domain-path}}?ids&status&textSearch&page&size&sort
       Controller → ListResponseCreator → I{{Domain}}Service → Repository (Criteria: count + page) → 200 | 204
GET    /{id}   Controller → ResponseCreator → I{{Domain}}Service → findProjectionByEntityId → 200 | 404
POST   /       Controller → ICreate…Service → validator → id generator → save → 201 {"id"} | 400 | 422
PUT    /{id}   Controller → IModify…Service → load (404) → validator → set fields → 204 | 400 | 422
DELETE /{id}   Controller → IDelete…Service → load (404) → validator → delete → 204 | 422
```

Copy it for a new module (`new-crud-module`); rename or delete it when the domain turns out
different. Reads go through a `*ResponseCreatorService`; a write that returns only an id or
nothing calls its operation service directly. The controller never touches a repository,
entity, mapper or internal DTO. A module without local persistence follows
`proxy-bff-conventions`.

## Non-negotiable

- **`Check`** (`Check.isNotNull`/`isNotEmpty`) first in every constructor (per dependency) and
  every public method (per parameter) — controllers, services, validators, clients, filters.
  A missing call is a review blocker.
- **Nothing proxied is `final`**: controller methods (`@PreAuthorize`) and `@Transactional`
  classes are CGLIB-proxied; `final` breaks them on live traffic only.
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
| `entity-and-liquibase-conventions` | entities, audit fields, Liquibase, datasource routing |
| `repository-conventions` | repository contract, impl, projections |
| `service-layer-conventions` | operation services, validators, ResponseCreatorService, mapper, dto vs model |
| `controller-conventions` | endpoints, status codes, response shapes |
| `exception-handling-conventions` | exception types and HTTP mapping |
| `testing-conventions` | test style and coverage |
| `inter-service-propagation` | auth filters, `IScopeContext`, outbound headers, `platform.md` registry |
| `proxy-bff-conventions` | modules that only compose other services |
| `domain-events-conventions` | optional Kafka pattern |
| `storage-conventions` | S3 / Azure Blob behind one interface, presigned URLs, LocalStack |
| `permissions-and-tokens` | authority codes, users/groups/permissions, personal access tokens |
| `new-crud-module` / `new-proxy-module` | ordered checklists for a new module |

## Agent behavior

- **Language**: everything said to the user is Bosnian; code, Javadoc, comments, commits English.
- **Subagents** don't inherit this file: every agent prompt starts by reading it plus the skill
  files it needs (`Read .claude/skills/<name>/SKILL.md` — subagents can't invoke skills).
- **Other services** are known only from `.claude/skills/inter-service-propagation/platform.md`.
  Never read a sibling repo; if the registry lacks something, ask.
- **Git/infra**: no commit, push, `docker` or stack changes unless the user asks (the software
  factory's `/software-factory run` is that ask, for its own job branch).
- **Local run**: the `development` repo (next to this one) provides Postgres, Keycloak (realm
  `factory`, users `admin`/`manager`/`user`/`basic`, password = name) and LocalStack; `dev-up`
  starts them. `docker-compose.yml` runs this service in a container on that network.
- **Permissions**: a new one goes in `Constants.ApiScopes` and `permissions.txt` (see
  `permissions-and-tokens`). **Coverage**: JaCoCo ≥ 0.80 (instruction and line), never lower.
- **Markdown** files stay under 200 lines.
- **Subagents can't ask the user.** They end with one block and the main session acts on it:
  - `## TREBA ODLUKA` — blocked: the question, 2–3 options with consequences; nothing edited.
  - `## KLASE ZA TESTOVE` — every code-writing agent: `src/main/java/…/X.java — what to cover`.
  - `## VERDIKT: APPROVE | WARNING | BLOCK` — last line of every review agent (`BLOCK` = a
    non-negotiable rule broken, `WARNING` = style/Javadoc/missing test, `APPROVE` = clean).
