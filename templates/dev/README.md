# development

Local infrastructure for the platform, with Docker Compose. You start exactly the containers
you name and stop them the same way. Every Java service made with the software factory
registers itself here.

## What you can start

| Name | What | Address on your machine | Login |
|---|---|---|---|
| `db` | Postgres 16 | `localhost:5432` | `postgres` / `postgres` |
| `auth` | Keycloak 24 (realm `factory`) | http://localhost:8080 | admin console `admin` / `admin` |
| `aws` | LocalStack: S3, SQS, SES | http://localhost:4566 | keys `test` / `test`, region `eu-west-1` |
| `redis` | Redis 7 | `localhost:6379` | none |
| `azurite` | Azure Storage emulator | `localhost:10000-10002` | default Azurite account |
| `<service>` | a Java service from `../<service>` | its port in `platform.md` | Keycloak token |

Inside Docker they reach each other as `database.factory.local`, `auth.factory.local`,
`aws.factory.local`, `redis.factory.local`, `azure.factory.local`, `<service>.factory.local`.

## Once per machine

Start Docker (Rancher Desktop), then:

```bash
./scripts/install-aliases.sh   # dev-* commands in every new terminal (open a new one after)
dev-hosts                      # *.factory.local -> 127.0.0.1 in /etc/hosts (sudo)
```

`dev-hosts` only adds its own block between `# >>> factory development >>>` markers and
rewrites only that block on the next run; every other line of `/etc/hosts` stays as it is.

## Start and stop with docker compose

Run these in this folder (`.env` makes `docker compose` use all three compose files).

```bash
docker compose up -d db auth             # Postgres + Keycloak
docker compose up -d redis aws           # add more later, any time
docker compose up -d                     # everything, including every Java service
docker compose ps                        # what runs, and whether it is healthy
docker compose logs -f auth              # follow one container's logs
```

```bash
docker compose stop auth                 # stop one, keep it for a later start
docker compose stop                      # stop everything, keep the containers
docker compose down                      # stop and remove the containers; data stays
```

> **Never `docker compose down -v`** unless you want a clean slate: `-v` deletes every local
> database, Keycloak's state and Redis. `dev-reset` does the same, after asking.

Keycloak needs 30-60 s after start; `docker compose up -d --wait db auth` returns only when
both are healthy.

### Common sets

| You work on | Start |
|---|---|
| A service from the IDE | `docker compose up -d db auth` (+ `aws` / `redis` if it uses them) |
| Only the database | `docker compose up -d db` |
| One service in Docker | `dev-rebuild invoice` (builds its image from the checkout, restarts only it) |
| The whole platform | `dev-up-all` |

## The same with dev-* aliases

| Command | Does |
|---|---|
| `dev-up [svc..]` | `db auth aws` (+ the services named), waits for healthy, applies the realm's permissions |
| `dev-up-all` | everything, waits for healthy |
| `dev-rebuild <svc>` | rebuild `../<svc>` at its HEAD (image labeled with the commit) and restart only it; never a raw `docker compose -f …` |
| `dev-redis` / `dev-azure` | start Redis / Azurite |
| `dev-stop <svc>` | stop one container |
| `dev-down` | stop everything, data kept |
| `dev-reset` | remove everything **and delete all data** (asks first) |
| `dev-ps` / `dev-logs [svc]` | what runs / follow logs |
| `dev-psql [db]` | psql as `postgres` |
| `dev-token [user]` / `dev-curl <url>` | access token / curl with one (`DEV_USER=manager dev-curl ...`) |
| `dev-s3 <args>` | `aws s3` against LocalStack (bucket `factory-local`) |
| `dev-help` | this list |

The aliases work from any folder. Use `dev-up` rather than a plain `docker compose up` after
the realm changed (new permissions): it re-applies `factory-realm.json` to Keycloak.

## Run a service

```bash
dev-rebuild invoice                                # in Docker
dev-curl http://localhost:50001/invoice/v1/...   # call it with an admin token
```

Or start it from the IDE (`mvn spring-boot:run` in `../invoice`) with `db` and `auth` up: it
reaches them through the `*.factory.local` names.

## Test users and permissions

| User | Password | Group | Gets |
|---|---|---|---|
| admin | admin | Administrator | every permission |
| manager | manager | Manager | e.g. MANAGER, READ-USER, EDIT-USER, EDIT-FILE |
| user | user | Employee | e.g. READ-USER, READ-MY-PAT, READ-FILE |
| basic | basic | none | nothing (for 403 checks) |

A permission (`READ-USER`) is a realm role plus a client scope of the same name, so it shows
up in the token's `scope` claim and in `realm_access.roles`; services read it as
`SCOPE_READ-USER`. Tokens also carry `user_id` and `permission_groups`.

```bash
dev-perm                       # every permission and its groups
dev-perm add READ-INVOICE --groups Manager,Employee
dev-perm sync ../invoice       # everything in ../invoice/permissions.txt
dev-token manager              # an access token
```

`docker-mounts/keycloak/factory-realm.json` is the truth: `dev-perm` edits it and the running
Keycloak, `dev-up` re-applies it after every start. The software factory syncs a service's
`permissions.txt` during its local check and commits the realm here after the merge.

## Ports

Another stack already on 5432/8080/4566/6379? Set `DB_PORT`, `AUTH_PORT`, `AWS_PORT`,
`REDIS_PORT` in `.env`; nothing else needs to change.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Cannot connect to the Docker daemon` | start Rancher Desktop |
| `auth` stays `starting` | wait a minute; `docker compose logs -f auth` |
| A service can't resolve `*.factory.local` from the IDE | `dev-hosts` |
| New permission missing in tokens | `dev-up` (re-applies the realm) |
| A new database from `dev-add` is missing | the init script runs on an empty volume only; `dev-add` creates it in a running Postgres, so start `db` and run it again |

## Add a service by hand

```bash
dev-add invoice 50001 --db invoice
```

`software-factory new <name> java` does this for you.
