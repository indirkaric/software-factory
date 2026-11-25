--liquibase formatted sql

--changeset {{service-name}}:0002-create-{{domain_table}}-table
CREATE TABLE IF NOT EXISTS {{schema}}.{{domain_table}}
(
    id          int8 GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_id   varchar(120)  NOT NULL,
    name        varchar(255)  NOT NULL,
    description varchar(1000),
    status      int2          NOT NULL DEFAULT 1,
    created_by  varchar(255)  NOT NULL,
    created_utc timestamp     NOT NULL,
    updated_by  varchar(255),
    updated_utc timestamp
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_{{domain_table}}_entity_id ON {{schema}}.{{domain_table}} (entity_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_{{domain_table}}_name ON {{schema}}.{{domain_table}} (lower(name));
--rollback DROP TABLE IF EXISTS {{schema}}.{{domain_table}};
