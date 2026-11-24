# development

Local infrastructure for every service of the platform, with Docker Compose. Not a service:
no Java code, no factory jobs. Talk to the user in Bosnian; files and commits in English.

## What runs

| Name (alias) | Image | Host port | Notes |
|---|---|---|---|
| `db` (database.indir.local) | postgres:16 | 5432 | `postgres`/`postgres`; one database + role `<db>_app` per service |
| `auth` (auth.indir.local) | Keycloak 24 | 8080 | realm `indir`, client `indir-app` (public, password grant), admin `admin`/`admin` |
| `aws` (aws.indir.local) | LocalStack 3.8 | 4566 | s3, sqs, ses; region `eu-west-1`; keys `test`/`test`; bucket `indir-local` |
| `azurite` (azure.indir.local) | Azurite | 10000-10002 | profile `azure` |
| `redis` (redis.indir.local) | redis:7 | 6379 | profile `redis` |

Keycloak users (password = user name): `admin` (ADMIN, MANAGER, USER), `manager` (MANAGER,
USER), `user` (USER). Services map realm roles to `SCOPE_<ROLE>` authorities. Tokens always
carry `iss=http://auth.indir.local:8080/realms/indir`.

## Files

- `docker-compose.yml`: infrastructure. `docker-compose.override.yml`: host ports.
- `docker-compose.services.yml`: Java services built from `../<name>`; one block per service,
  written by `scripts/add-service.sh`. `.env` sets `COMPOSE_FILE` to all three.
- `docker-mounts/postgres/init-scripts/01_create_databases.sql`: runs on an empty volume only.
- `docker-mounts/keycloak/indir-realm.json`: imported on Keycloak's first start.
- `docker-mounts/localstack/*.sh`: LocalStack ready hooks (buckets, queues).
- `scripts/aliases.sh`: the `dev-*` shell functions (`dev-help`); `scripts/install-aliases.sh`.
- `scripts/hosts.txt` + `scripts/setup-hosts.sh`: `*.indir.local` → 127.0.0.1 in /etc/hosts.

## Rules

- A new service is registered with `scripts/add-service.sh <name> <port> [--db <db>]`, never
  by hand-editing the services file.
- Changing the realm JSON only affects a fresh Keycloak database (`dev-reset`), so prefer adding
  roles and users through the admin console for a running stack, and in the JSON for everyone.
- A new bucket or queue: a new numbered hook in `docker-mounts/localstack/`.
- Never `docker compose down -v` without the user's yes: it deletes every local database.
- No end-to-end test suite here; services are checked by the software factory's local check.
- Markdown files stay under 200 lines.
