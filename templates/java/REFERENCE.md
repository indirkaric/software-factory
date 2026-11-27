# Reference provenance

`skeleton/` is the starting point of every new service. It was distilled once from the two
reference services, so neither Claude nor the script ever reads them again. This file records
what came from where and what was deliberately changed. Read it only when changing the skeleton.

| Area | From | Changed on purpose |
|---|---|---|
| `pom.xml` | identity | Dropped batch, zxing, aws-rds, json-patch, snakeyaml (jpamodelgen came back for the reference module). JPA/Liquibase sit between `@persistence` markers. JaCoCo excludes only wiring and data holders. |
| checkstyle | identity (stricter than artifact: 60-line methods, `InnerAssignment`, `Designed for extension.`) | — |
| `Dockerfile` | identity (slim alpine, non-root) | artifact's installs Chrome and mount-s3, so it was not used |
| `.github/workflows/ci.yml` | new | `mvn verify` on every push/PR; persistence adds `liquibase:updateTestingRollback` on an empty Postgres |
| security filters | artifact | Not `@Component` (artifact registers them both as beans and in the chain, so Boot runs each twice). `UserIdFilter` gets `IScopeContext` injected instead of a static lookup. A malformed header is ignored, not a 500. Unknown client → no authorities (identity grants defaults). The client-id allowlist is a `Set`, not an if-chain. |
| `ScopeContext` | artifact | Plain `ThreadLocal` for the user id (artifact's `InheritableThreadLocal` leaks into pooled threads), plus `clearRequestUserId()` |
| `WebClientConfig` | artifact (timeouts, retry, buffer) | `correlation-id`/`user-id` added per request. Artifact uses `.defaultHeaders`, which freezes the value at startup. |
| `BaseClient` | artifact | Binds base URI + `service-communication-id` once per downstream. Error swallowing is opt-in (artifact swallows 404/500/503 by default). |
| `GlobalExceptionHandler` | artifact | Adds `MethodArgumentNotValidException` → 400, and Spring MVC's `ErrorResponse` keeps its status (artifact turns an unknown path into 500). |
| datasource routing | artifact | The interceptor also matches class-level `@Transactional`. Artifact matches only the method level, so class-annotated write services read the read-only pool. |
| `entity/common`, `BaseEntityListener`, `AuditContext` | artifact | Without the LLM-specific update rule. Listener typed to `AuditEntity`. |
| `platform.md` | — | Empty here; `software-factory new` appends to the development repo's copy |
| header names | company-prefixed | `service-communication-id`, `correlation-id`, `user-id`; filters renamed to match |
| `KeycloakRolesConverter` | new | realm roles become `SCOPE_` authorities too, so the dev realm needs no client scopes |
| reference module `{{Domain}}` | flow of identity (groups) and the UI backend (a filtered list), artifact repositories | One module, controller to repository, persistence role only. Criteria API with `criteriaBuilder.construct` into record projections (sources: Lombok classes + `multiselect`). Count first, page query only when count > 0 (UI backend). One predicate builder for page and count (identity). Sort whitelist, `id`→`entityId`, `entityId` tiebreaker (identity's `getOrderingByName`); sources accept any property. LIKE input escaped. Constructor-injected `EntityManager` without `@PersistenceContext` on a final field. `Optional` instead of `null`. Reads through response creators, id/no-body writes call the operation service (identity's `GroupController`). |
| `annotations/`, `CustomFieldProcessor` | identity, the UI backend | Walks superclasses, skips statics, `Locale.ROOT`; a reflection failure is an `IllegalStateException`, not a 422. |
| `PagedList`, `SortHelper`, `EntityIdGeneratorService` | identity / the UI backend | `PagedList` is a record holding an immutable copy. Ids from `SecureRandom` (sources: `Math.random`). |
| `hibernate-jpamodelgen` | identity, the UI backend | Annotation-processor path only, between `@persistence` markers. |
| Liquibase | `0.0.1-SNAPSHOT/` folders, ids = the version | `changes/NNNN-*.sql`, unique ids, `IF NOT EXISTS`, rollback per changeset |
| `storage-conventions`, `permissions-and-tokens` | data-storage, identity | SDK v2, explicit presign TTL, unknown caller gets nothing, PAT lookup id + scopes enforced |

| `proxy-skeleton/` | `skeleton/` minus persistence, the UI backend's proxy flow | Its own skeleton instead of stripping `@persistence` markers. Client → `ClientResponseWrapper` → `ResponseUtil` → service per verb → controller, as in the UI backend, but: one `exchange()` in `BaseClient` instead of a full WebClient chain per method; the base URI and `service-communication-id` bound once (the UI backend sets the header per call); query values as URI variables (fully encoded, empty ones skipped); the wrapper is a record with `HttpStatusCode`; `ResponseUtil` also maps 400/401/403/409, reads both error body shapes, ignores unknown fields, and never instantiates a response for a 204 (records). Headers, packages and filters stay the factory's. No PAT/shared-link filters, audit aspect or local tables. |

Verified: both roles build with `mvn -o verify` (tests, JaCoCo 0.80, checkstyle), also with a
20-character `--domain` (the script's limit, so generated lines stay under 120 characters). The proxy role
(`proxy-skeleton/`, also with `--calls` against a registered service) boots, with health/swagger
200 and 401 without auth. The persistence
role has not been booted against Postgres.

Dockerfile (both roles): multi-stage. A `maven:3.9-amazoncorretto-17` build stage runs
`mvn package` on the checkout (pom.xml and `dependency:go-offline` first, `~/.m2` as a BuildKit
cache mount), so an image never carries a stale `target/*.jar`; `ARG GIT_SHA` →
`LABEL org.opencontainers.image.revision` (set by `dev-rebuild`, read by `verify preflight`).
`.dockerignore` sends only `pom.xml` and `src/`. Verified: both roles build and the image holds the jar.
