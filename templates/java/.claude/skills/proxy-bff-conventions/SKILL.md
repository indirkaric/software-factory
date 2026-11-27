---
name: proxy-bff-conventions
description: Use when building or changing an endpoint that forwards to or composes other microservices instead of reading/writing local persistence (a proxy/BFF module): clients, ClientResponseWrapper, ResponseUtil, partial failures.
---

The reference is the proxied module a proxy service was generated with (and the factory's
`~/.config/software-factory/templates/java/proxy-skeleton`): `client/<downstream>/<Domain>Client` →
`service/<downstream>/<domain>/` → `modules/<downstream>/<domain>/<Domain>Controller`.

**Client** — one per downstream resource: `client/<downstream>/<Resource>Client extends BaseClient`,
`@Component`, base URI from `client.<downstream>.base-uri`, id `Constants.<DOWNSTREAM>_CLIENT`
(`inter-service-propagation`), and `public static final String SERVICE_NAME` for messages.
- Only the methods a module uses. Each `Check`s its parameters and returns
  `exchange(webClient().<verb>().uri(...)...)`: a `ClientResponseWrapper(statusCode, body)`, the raw
  error body for a 4xx/5xx, `null` when unreachable. It never maps the body and never throws for a status.
- Paths start with `/v1/...`; ids go in as URI variables (`.uri(ITEM_PATH, id)`); query parameters
  through `queryParam(builder, variables, name, value)` and `builder.build(variables)`, so every value
  is encoded and empty filters are left out. One `sort=<property>,<direction>` per `Pageable` order.
- Never a raw `WebClient` chain in a service, never `subscribe()`: `exchange` blocks on the request
  thread, where the scope context (`correlation-id`, `user-id`) lives.

**Service** — one per verb (`I<Domain>Service` for reads, `ICreate|IModify|IDelete<Domain>Service`),
in `service/<downstream>/<domain>/` + `impl/`. Each calls the client and hands the wrapper to
`ResponseUtil.handleResponse(wrapper, Type.class, Client.SERVICE_NAME)` (`null` type for no body).
- `ResponseUtil` owns the status mapping: 400 → `BadRequestException`, 401/403 →
  `AccessDeniedException`, 404 → `EntityNotFoundException`, 409 → `ConflictException`, 422 →
  `BusinessException` (the downstream's messages kept), unreachable/503 →
  `ServiceUnavailableException` (503), anything else → 500. A service doesn't check status codes.
- A 2xx without a body comes back `null`: a list becomes an empty page (the controller answers 204),
  a read becomes `EntityNotFoundException`, a create without an id is an `IllegalStateException`.
- Composing several downstreams: one operation service calling the clients in dependency order.
  Partial failure is explicit: fail the request, or return what is there with the gap marked. Never
  let a silent empty result stand in for a failure.

**Models** — request models (classes, normalized by `CustomFieldProcessor`, validated with `@Valid`)
and response records in `modules/<downstream>/<domain>/model/`, shaped like the downstream's
contract. `ResponseUtil` ignores unknown fields, so a downstream may add fields. A shape only a
client needs goes in `client/<downstream>/model/`.

**Controller** — as `controller-conventions`, plus a `503` `@ApiResponse` on every endpoint.

**Tests** — client: a `WebClient` with a stub `exchangeFunction`, asserting method, URL, query and
the `service-communication-id` header. Service: mock the client, return wrappers for 2xx, 204, the
error statuses and `null`. Controller: standalone MockMvc with `GlobalExceptionHandler`.

Long-running downstream work: a trigger endpoint returns an id and a status endpoint polls it. No
WebSocket or SSE unless asked.
