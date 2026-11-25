---
name: repository-conventions
description: Use when creating or modifying the repository layer (I<Domain>Repository interface, its Criteria API impl, a projection or a list filter) in this repo.
---

The reference is the skeleton's `{{Domain}}` module (`repository/`, `repository/impl/`,
`repository/projection/`, `service/model/<Domain>ListFilter`): copy its shape, rename, then adapt.

## Contract: `repository/I<Domain>Repository`

- Hand-written, root-level, never under `modules/`. It does **not** extend `JpaRepository`:
  callers see only domain-named methods.
- Reads return projections, writes load the entity:
  - `Page<<Domain>ItemProjection> findPagedList(<Domain>ListFilter filter, Pageable pageable)`
  - `Optional<<Domain>Projection> findProjectionByEntityId(String entityId)` (detail)
  - `Optional<<Domain>> findByEntityId(String entityId)` (only for modify/delete)
  - `boolean existsByEntityId(String)`, `boolean existsBy<Field>(…, String excludedEntityId)`
  - `void save(<Domain>)` (persist a new row), `void delete(<Domain>)`
- `Optional` for "maybe one", never `null`. No update method: an entity loaded in a
  `@Transactional` write service is managed and flushed on commit.
- The filter is a record in `service/model/` (`<Domain>ListFilter(ids, status, textSearch)`),
  built by the list response creator. The repository never imports an API `model/` class.

## Impl: `repository/impl/<Domain>Repository` — Criteria API

- `@Repository`, constructor-injected `EntityManager` with `Check.isNotNull`.
- Attributes only through the generated JPA metamodel (`<Domain>_.name`,
  `ExternalEntity_.entityId`, `AuditEntity_.createdUtc`), never string names. The metamodel
  comes from `hibernate-jpamodelgen` in the compiler's annotation processors.
- Projections are records built with `criteriaBuilder.construct(<X>Projection.class, …)`. The
  record's components match the selected attributes in order and type (`Short` for `int2`,
  `Date` for `timestamp`). Select exactly the columns the read needs.
- Paged list, in this order:
  1. `long total = count(filter)`: `select(cb.count(root))` with the same predicates.
  2. `total == 0` → `new PageImpl<>(List.of(), pageable, 0)` without running the page query.
  3. Page query: `select(construct…)`, `.where(predicates(cb, root, filter))`,
     `.orderBy(SortHelper.getOrders(cb, root, pageable, SORTABLE))`, then
     `setFirstResult((int) pageable.getOffset())` and `setMaxResults(pageable.getPageSize())`.
  4. `return new PageImpl<>(items, pageable, total)`.
- One private `predicates(cb, root, filter)` builds a `List<Predicate>` used by both the page
  and the count query, so they can never filter differently. One `if` per optional field:
  - list of ids → `root.get(ExternalEntity_.entityId).in(ids)` (only when not empty)
  - exact value → `cb.equal(root.get(<Domain>_.status), status)`
  - text search → `cb.or(cb.like(cb.lower(…name), pattern, SortHelper.LIKE_ESCAPE), …)` with
    `pattern = SortHelper.containsPattern(text)`, which lower-cases and escapes `%`, `_`, `\`.
- Sorting: `SORTABLE` is a `Set` of metamodel name constants (`<Domain>_.NAME`). `SortHelper`
  maps `id` to `entityId`, rejects anything not in the set with a 400, and always appends
  `entityId` so pages are stable.
- Joins: `root.join(<Domain>_.parent, JoinType.LEFT)` and select/filter on the join's
  attributes; a to-many join in a list needs `query.distinct(true)` and a `countDistinct`.
- Single row: `getResultStream().findFirst()`. Existence: select `entityId`,
  `setMaxResults(1)`, `!getResultList().isEmpty()`. Case-insensitive uniqueness:
  `cb.equal(cb.lower(…), value.toLowerCase(Locale.ROOT))`, plus `notEqual(entityId, excluded)`
  when modifying.
- Never concatenate request values into JPQL or SQL. Native SQL only when Criteria can't express
  it, with bound parameters, and the reason in a comment.
- `Check.isNotNull`/`isNotEmpty` on every public method's parameters. Every public method's
  Javadoc says `Designed for extension.`.
- The repository package is excluded from JaCoCo; it is proved by the factory's local check
  against Postgres, so every query a feature adds must be reached by a call in the spec's
  `## Local check`.
