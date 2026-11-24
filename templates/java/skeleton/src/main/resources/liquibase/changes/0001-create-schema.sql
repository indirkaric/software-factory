--liquibase formatted sql

--changeset {{service-name}}:0001-create-schema
CREATE SCHEMA IF NOT EXISTS {{schema}};
CREATE EXTENSION IF NOT EXISTS citext;
--rollback DROP SCHEMA IF EXISTS {{schema}};
