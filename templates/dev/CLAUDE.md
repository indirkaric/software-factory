# development

Local infrastructure for every service of the platform, with Docker Compose. Not a service:
no Java code, no factory jobs. Talk to the user in Bosnian; files and commits in English.

## What runs

| Name (alias) | Image | Host port | Notes |
|---|---|---|---|
| `db` (database.factory.local) | postgres:16 | 5432 | `postgres`/`postgres`; one database + role `<db>_app` per service |
| `auth` (auth.factory.local) | Keycloak 24 | 8080 | realm `factory`, client `factory-app` (public, password grant), admin `admin`/`admin` |
| `aws` (aws.factory.local) | LocalStack 3.8 | 4566 | s3, sqs, ses; region `eu-west-1`; keys `test`/`test`; bucket `factory-local` |
| `azurite` (azure.factory.local) | Azurite | 10000-10002 | profile `azure` |
| `redis` (redis.factory.local) | redis:7 | 6379 | profile `redis` |

Keycloak users (password = user name) and groups: `admin` (Administrator: every
permission), `manager` (Manager), `user` (Employee), `basic` (none). A permission is a realm
role + a client scope + a scope mapping, all one name (`READ-USER`); services read it as
`SCOPE_READ-USER`. Tokens carry `iss=http://auth.factory.local:8080/realms/factory`,
`user_id` and `permission_groups`. Host ports come from `.env` (`DB_PORT`, `AUTH_PORT`, `AWS_PORT`).

## Files

- `docker-compose.yml`: infrastructure. `docker-compose.override.yml`: host ports.
- `docker-compose.services.yml`: Java services built from `../<name>`; one block per service,
  written by `scripts/add-service.sh`. `.env` sets `COMPOSE_FILE` to all three.
- `docker-mounts/postgres/init-scripts/01_create_databases.sql`: runs on an empty volume only.
- `docker-mounts/keycloak/factory-realm.json`: imported on Keycloak's first start.
- `docker-mounts/localstack/*.sh`: LocalStack ready hooks (buckets, queues).
- `scripts/realm.py`: permissions (realm roles, client scopes, groups).
- `scripts/aliases.sh`: the `dev-*` shell functions (`dev-help`); `scripts/install-aliases.sh`.
- `scripts/hosts.txt` + `scripts/setup-hosts.sh`: `*.factory.local` → 127.0.0.1 in /etc/hosts.

## Rules

- A new service is registered with `scripts/add-service.sh <name> <port> [--db <db>]`, never
  by hand-editing the services file.
- Permissions only through `scripts/realm.py` (`dev-perm add|sync|apply`): it edits the realm
  JSON and the running Keycloak alike; `dev-up` re-applies the JSON. Services list theirs in
  `<service>/permissions.txt`, which the software factory syncs here.
- A new bucket or queue: a new numbered hook in `docker-mounts/localstack/`.
- Never `docker compose down -v` without the user's yes: it deletes every local database.
- No end-to-end test suite here; services are checked by the software factory's local check.
- Markdown files stay under 200 lines.
