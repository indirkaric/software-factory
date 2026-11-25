---
name: testing-conventions
description: Use when writing, completing, or fixing unit tests for a Service, Validator, Mapper, Controller, or client wrapper class in this repo.
---

Unit tests and a green `mvn verify` are mandatory for every change: no class with logic ships
without its test, and nothing is done until the whole build passes.

- JUnit 5 with `@ExtendWith(MockitoExtension.class)`, `@Mock` per collaborator, and the class under
  test built by hand in `@BeforeEach void setUp()`. Never `@InjectMocks`.
- AssertJ only (`assertThat`, `assertThatThrownBy`, `assertThatCode`), not JUnit assertions.
- `<ClassUnderTest>Test` in the same package under `src/test/java`. Method names are
  `should<Behavior>()`. `var` keeps long generic declarations under 120 characters.
- Strict stubs: stub only what the test uses (no shared stubbing in `setUp` that some tests skip).

## Per layer (the reference module has one of each)

- Operation service: happy path (an `ArgumentCaptor` on what is saved), validator rejection →
  nothing saved/changed, 404 branch, `Check` failures.
- Validator: each rule accepting and rejecting, `Check` failures.
- Response creator: the read result is mapped; for a list, the filter passed on (captor) and the
  total count kept.
- Mapper: `Mappers.getMapper(I<Domain>Mapper.class)`; `entityId` becomes `id`, list rows leave
  detail fields null.
- Controller: standalone MockMvc (`MockMvcBuilders.standaloneSetup(controller)
  .setControllerAdvice(new GlobalExceptionHandler()).setCustomArgumentResolvers(new
  PageableHandlerMethodArgumentResolver())`). One test per documented status: send the JSON
  request body, assert the status and the JSON response body (`content().json(...)`,
  `jsonPath`), and that normalization (`@Trimmed`, upper-cased ids) reached the service. 204 for
  an empty list. Security isn't applied here; the local check proves `@PreAuthorize`.
- Servlet code: `MockHttpServletRequest`/`Response`/`MockFilterChain`. Client wrappers: a `WebClient`
  built with `.exchangeFunction(...)` that captures the `ClientRequest`; assert URL, body and
  `service-communication-id` (`BaseClientTest`, `WebClientConfigTest`).
- Repositories are excluded from JaCoCo; the factory's local check against Postgres proves them.

## Running

- Checkstyle covers tests too: `final` parameters, the blank line after `}`, no magic numbers
  (use constants or 0/1/2), 120 characters.
- `mvn -o -q test -Dtest=A,B` while iterating, then `mvn -o verify` (tests + JaCoCo ≥ 0.80 +
  checkstyle). If coverage is short, add the missing branches. Never add exclusions.
