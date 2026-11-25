---
name: controller-conventions
description: Use when creating or modifying a REST Controller in this repo — endpoint annotations, request binding and normalization, status codes, and the final-method CGLIB gotcha.
---

The reference is the skeleton's `modules/{{domain_package}}/{{Domain}}Controller` and its
`{{Domain}}ControllerTest`.

## Shape

- `modules/<domain_package>/<Domain>Controller.java` (`<domain_package>` is snake_case:
  `purchase_order`), `@RestController`,
  `@RequestMapping(path = {"/{{api-prefix}}/v1/<plural-kebab>"}, produces = APPLICATION_JSON_VALUE)`,
  `@Tag`. No `server.servlet.context-path`: the prefix is in the mapping.
- Per endpoint: Javadoc with `Designed for extension.`, `@Operation(summary, security =
  @SecurityRequirement(name = Constants.BEARER_KEY_VALUE))`, `@ApiResponses` listing every status
  it can produce (also the thrown 400/404/422, with `RestApiResponse`/`BusinessExceptionResponse`
  schemas), `@PreAuthorize(Constants.Authority.…)` or a new scope, and a `ResponseEntity<T>`.
- Method body, always in this order: `Check` every parameter →
  `CustomFieldProcessor.processAnnotatedFields(request)` for every request model → one service
  call → wrap in `ResponseEntity`.

## Who the controller calls

- Reads (`GET` list and detail) → a `*ResponseCreatorService` in the module, which composes the
  read service and the mapper into the API model.
- Writes that return only an id or nothing (`POST` → `EntityCreatedResponse`, `PUT`/`DELETE` →
  204) → the operation service (`ICreate…`, `IModify…`, `IDelete…`) directly. A write that must
  answer with a composed body goes through a response creator too.
- Never a repository, entity, mapper or internal `dto` type.

## Request binding

- Bodies: `@Valid @RequestBody` on a Lombok `@Data` class (not a record: the field processor
  mutates it). Field rules are `jakarta.validation` annotations with a `message` and constants
  for sizes (`@Size(max = Constants.MAX_NAME_LENGTH)`); a violation is a 400 with
  `{"messages": ["name: Name is required."]}`.
- Normalization annotations from `annotations/`: `@Trimmed` (trim + collapse spaces),
  `@Uppercase`, `@Lowercase`, `@UppercaseList` (ids).
- List filters are **query parameters** bound to a `<Domain>ListRequest` class with
  `@ParameterObject`, plus `@ParameterObject @PageableDefault(size = Constants.DEFAULT_PAGE_SIZE)
  Pageable` (`?page=0&size=50&sort=name,desc`). Never a body on `GET`.
- Path ids: `@PathVariable("id") String id`, passed on as `id.toUpperCase(Locale.ROOT)` (generated
  ids are upper-case).

## Responses

- No generic envelope. `201` + `EntityCreatedResponse(id)`; `200` + `<Domain>Response` record;
  list `200` + `<Domain>ListResponse(List<<Domain>ItemResponse> data, long count)`, or `204`
  when `data` is empty; `PUT`/`DELETE` `204`.
- `entityId` is exposed as `id`; the database id never leaves the service.
- Errors are thrown, never built here: `EntityNotFoundException` 404, `BusinessException` 422,
  `IllegalArgumentException` (from `Check`, `SortHelper`) 400.

## Never `final`

Controller methods are never `final`: Spring's method-security proxy (CGLIB) must override them
for `@PreAuthorize`. A `final` method NPEs on every injected field on live traffic only, which
mocked unit tests never show. Same reason a `@Transactional` class is never `final`.

## Test

`<Domain>ControllerTest`: standalone MockMvc with `GlobalExceptionHandler` and
`PageableHandlerMethodArgumentResolver`, one test per status the endpoint documents, asserting the
JSON response body for the given JSON request body (see `testing-conventions`).
