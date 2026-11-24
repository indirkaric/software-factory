# Reference provenance

`skeleton/` is the starting point of every new service. It was distilled once from the two
reference services, so neither Claude nor the script ever reads them again. This file records
what came from where and what was deliberately changed. Read it only when changing the skeleton.

| Area | From | Changed on purpose |
|---|---|---|
| `pom.xml` | identity | Dropped batch, zxing, aws-rds, json-patch, snakeyaml, jpamodelgen. JPA/Liquibase sit between `@persistence` markers. JaCoCo excludes only wiring and data holders. |
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
| Liquibase | `0.0.1-SNAPSHOT/` folders, ids = the version | `changes/NNNN-*.sql`, unique ids, `IF NOT EXISTS`, rollback per changeset |
| `storage-conventions`, `permissions-and-tokens` | data-storage, identity | SDK v2, explicit presign TTL, unknown caller gets nothing, PAT lookup id + scopes enforced |

Verified: both roles build with `mvn -o verify` (tests, JaCoCo 0.80, checkstyle). The proxy role
boots, with health/swagger 200, 401 without auth and 404 for an unknown path. The persistence
role has not been booted against Postgres.
