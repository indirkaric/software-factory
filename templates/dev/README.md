# development

Local infrastructure for the platform: Postgres, Keycloak, LocalStack (AWS) and, on demand,
Azurite and Redis. Every Java service made with the software factory registers itself here.

## Start

```bash
./scripts/install-aliases.sh   # once: dev-* commands in every new terminal
dev-hosts                      # once: *.indir.local -> 127.0.0.1 (sudo)
dev-up                         # Postgres :5432, Keycloak :8080, LocalStack :4566
```

`dev-help` lists every command. Keycloak admin console: http://localhost:8080 (`admin`/`admin`).

## Run a service

```bash
dev-run invoice                # mvn package ../invoice, build the image, start it
dev-curl http://localhost:50001/invoice/v1/...   # with an admin token
```

Or start it from the IDE (`mvn spring-boot:run` in the service): it reaches the infrastructure
through the `*.indir.local` names.

## Test users

| User | Password | Roles |
|---|---|---|
| admin | admin | ADMIN, MANAGER, USER |
| manager | manager | MANAGER, USER |
| user | user | USER |

`dev-token manager` prints an access token.

## Add a service by hand

```bash
dev-add invoice 50001 --db invoice
```

`software-factory new <name> java` does this for you.
