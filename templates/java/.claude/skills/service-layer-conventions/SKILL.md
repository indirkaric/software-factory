---
name: service-layer-conventions
description: Use when creating or modifying the layer between the repository and the controller — operation services split by verb, validators, the ResponseCreatorService seam, PagedList, entity id generation, the MapStruct mapper, or deciding dto vs model placement.
---

The reference is the skeleton's `{{Domain}}` module: `service/{{domain_package}}/`,
`service/validator/`, `mapper/I{{Domain}}Mapper`, `dto/{{Domain}}DTO`,
`modules/{{domain_package}}/service/`. Copy its shape, rename, then adapt.

## The flow

```
Controller ─GET─────> <Domain>ListResponseCreatorService ─> I<Domain>Service.getPagedList ─> repository.findPagedList
           ─GET /id─> <Domain>ResponseCreatorService ─────> I<Domain>Service.getByEntityId ─> findProjectionByEntityId
           ─POST────> ICreate<Domain>Service ─> validator ─> IEntityIdGeneratorService ─> repository.save
           ─PUT─────> IModify<Domain>Service ─> findByEntityId (404) ─> validator ─> set fields (flushed on commit)
           ─DELETE──> IDelete<Domain>Service ─> findByEntityId (404) ─> validator ─> repository.delete
```

## Operation services: one interface per operation

- `service/<domain_package>/` holds the interfaces, `impl/` the classes:
  `I<Domain>Service` (reads), `ICreate<Domain>Service`, `IModify<Domain>Service`,
  `IDelete<Domain>Service`. Never one fat service with every verb.
- `@Service`, constructor injection only, `private final` fields, `Check.isNotNull(dep, "dep")`
  per dependency in the constructor, `Check` per parameter in every public method, Javadoc with
  `@param`/`@return` and `Designed for extension.` on every public method.
- Write services are `@Transactional` at class level; the read service is
  `@Transactional(readOnly = true)` — that annotation is also the routing decision to the
  read-only datasource. The class is never `final` (CGLIB proxy).
- Create: `Check` → validator → `entityIdGenerator.generate(Constants.DEFAULT_ENTITY_ID_SIZE,
  repository::existsByEntityId)` → new entity, set fields and `Constants.Status.ACTIVE` →
  `repository.save` → return the id.
- Modify/delete: `Check` → load with `findByEntityId(...).orElseThrow(() -> new
  EntityNotFoundException("<Domain> with id { id: X } doesn't exist."))` → validator → change.
  Nothing is changed before the validator has run.
- Reads: `getPagedList(filter, pageable)` returns `PagedList<<Domain>DTO>` (`items`, `count`)
  from the repository's `Page`; `getByEntityId` maps the projection or throws the 404.

## Validators

- `service/validator/`, one interface + impl per write operation:
  `ICreate<Domain>ServiceValidator.validate(request)`,
  `IModify<Domain>ServiceValidator.validate(request, existing)`,
  `IDelete<Domain>ServiceValidator.validate(existing)`.
- Rules shared by two operations go up into `abstract Base<Domain>DataValidator` (protected
  constructor taking the repository, protected `validate<Rule>` methods); a rule only one
  operation has stays in that validator. Never copy a rule into two validators.
- Field formats (`@NotBlank`, `@Size`, `@Min`) live on the request model (400 via `@Valid`).
  Validators hold business rules that need data (uniqueness, state transitions, references) and
  throw `BusinessException` (422) with a message naming the value: `"… { name: X } already exists."`.

## Response creators: the read seam

`modules/<domain_package>/service/I<Domain>ResponseCreatorService` and
`I<Domain>ListResponseCreatorService` (+ `impl/`). The list creator turns the API
`<Domain>ListRequest` into the service's `<Domain>ListFilter`, calls the read service and maps
`PagedList` into `<Domain>ListResponse(mapper.toItemResponseList(page.items()), page.count())`.
A creator never touches a repository. A module without local persistence follows
`proxy-bff-conventions` instead.

## Mapper (MapStruct)

- Root-level `mapper/I<Domain>Mapper`, `@Mapper(componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE)`, Javadoc per method.
- Projection → DTO (`toDto`, `toItemDto`, `toItemDtoList`) and DTO → API model
  (`@Mapping(target = "id", source = "entityId") toResponse`, `toItemResponse`, lists). Records
  on both sides are fine. Request → entity is done by hand in the create service.
- Derived fields: `@Mapping(target = "…", expression = "java(Helper.x(...))")` delegating to one
  static helper, never the same computation in two places.

## dto vs model

- `modules/<domain_package>/model/`: the API. Requests are Lombok `@Data` classes (normalized in
  place); responses are records: `<Domain>Response`, `<Domain>ItemResponse`,
  `<Domain>ListResponse(data, count)`.
- Root `dto/<Domain>DTO`: internal record between read service and response creator. Never
  returned by a controller (only `dto/response/EntityCreatedResponse` is). No entity ever leaves
  the service layer.
- `service/model/`: `PagedList<T>` and the `<Domain>ListFilter` records.
