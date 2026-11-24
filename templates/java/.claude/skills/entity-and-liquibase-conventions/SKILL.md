---
name: entity-and-liquibase-conventions
description: Use when creating or modifying a JPA entity, its Liquibase changeset, its audit fields, or the datasource configuration in this repo.
---

## Entities

- `entity/<Name>.java` extends `entity/common/ExternalEntity`: internal `id`, public `entity_id`,
  and the audit columns. Use `AuditEntity` only for rows with no public id, such as join tables.
- Lombok `@Data`, `@EqualsAndHashCode(callSuper = true, exclude = {<collections>})`,
  `@ToString(callSuper = true, exclude = {<collections>})`,
  `@EntityListeners(BaseEntityListener.class)`, `@Table(name = "...", schema = "{{schema}}")`.
- Never expose the internal `id` over the API. Lookups and responses use `entityId`.
- `BaseEntityListener` fills `createdBy`/`createdUtc`/`updatedBy`/`updatedUtc` from
  `AuditContext`. Don't set them by hand.

## Liquibase

- One formatted-SQL file per change in `liquibase/changes/`, numbered:
  `0002-create-invoice-table.sql`, `0003-add-status-to-invoice.sql`. `liquibase/changelog.xml`
  includes each one, in order, with an explicit `<include>` (no `includeAll`).
- Each file: `--liquibase formatted sql`, then one or more
  `--changeset {{service-name}}:<NNNN>-<what>` (the id is unique on its own, never a version
  like `0.0.1-SNAPSHOT`), each with its own `--rollback` that undoes exactly that changeset.
- A rollback never deletes more than the changeset added: `DELETE ... WHERE entity_id IN (...)`
  for seed rows, never `DELETE FROM <table>`; `DROP TABLE` only of the table it created.
- Idempotent DDL where Postgres allows it: `CREATE ... IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`.
- Tables live in schema `{{schema}}`: `id int8 GENERATED ALWAYS AS IDENTITY`,
  `entity_id varchar NOT NULL` with a unique index, then the four audit columns.
- Additive only: new tables, new nullable or defaulted columns, new indexes. Renames, drops and
  type changes are a two-step change over two releases and need a human (the approver escalates).
- Seed or test data only with a context: `--changeset {{service-name}}:0004-seed-x context:dev`.
- Paths use `/`, never `\`. Never `ddl-auto: update`. Never edit a merged changeset.
- CI runs `liquibase:updateTestingRollback` on an empty Postgres; the factory's local check
  migrates an empty database from zero. Both must pass.

## Datasource

`database.readwrite` / `database.readonly` in `application.yaml`, not `spring.datasource`.
`DataSourceRouting` uses the read-write pool only inside a read-write transaction.
`TransactionDefinitionInterceptor` detects `@Transactional` on the method **or** the class.
Read services must therefore be `@Transactional(readOnly = true)`: that annotation is the routing
decision.
