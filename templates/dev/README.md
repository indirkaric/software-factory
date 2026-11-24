# development

Local infrastructure for the platform: Postgres, Keycloak, LocalStack (AWS) and, on demand,
Azurite and Redis. Every Java service made with the software factory registers itself here.

## Start

```bash
./scripts/install-aliases.sh   # once: dev-* commands in every new terminal
dev-hosts                      # once: *.factory.local -> 127.0.0.1 (sudo)
dev-up                         # Postgres :5432, Keycloak :8080, LocalStack :4566 (ports in .env)
```

`dev-help` lists every command. Keycloak admin console: http://localhost:8080 (`admin`/`admin`).

## Run a service

```bash
dev-run invoice                # mvn package ../invoice, build the image, start it
dev-curl http://localhost:50001/invoice/v1/...   # with an admin token
```

Or start it from the IDE (`mvn spring-boot:run` in the service): it reaches the infrastructure
through the `*.factory.local` names.

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

Another stack on 5432/8080/4566? Set `DB_PORT`, `AUTH_PORT`, `AWS_PORT` in `.env`.

## Add a service by hand

```bash
dev-add invoice 50001 --db invoice
```

`software-factory new <name> java` does this for you.
