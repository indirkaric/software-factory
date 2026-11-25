#!/usr/bin/env bash
# Register a Java service in this development repo (idempotent).
#
#   scripts/add-service.sh <name> <port> [--db <database>]
#
# - docker-compose.services.yml: the service, built from ../<name>, on <port>,
#   alias <name>.factory.local (start it by name)
# - with --db: database + role <db>_app (password <db>_app) in the init script,
#   and in the running Postgres when it is up
# - scripts/hosts.txt: <name>.factory.local
set -euo pipefail
DEV="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NAME="${1:?name}"; PORT="${2:?port}"; shift 2
DB=""; [[ "${1:-}" == --db ]] && DB="${2:?database}"
[[ "$NAME" =~ ^[a-z][a-z0-9-]*$ ]] || { echo "add-service: bad name $NAME" >&2; exit 1; }

python3 - "$DEV/docker-compose.services.yml" "$NAME" "$PORT" "$DB" <<'PY'
import sys
path, name, port, db = sys.argv[1:5]
text = open(path).read()
if f"\n  {name}:\n" in text:
    sys.exit(0)
var = name.upper().replace("-", "_")
deps = "      db:\n        condition: service_healthy\n" if db else ""
block = f"""  {name}:
    image: ${{{var}_IMAGE:-factory/{name}:local}}
    build: ${{PROJECTS_DIR:-..}}/{name}
    restart: unless-stopped
    depends_on:
{deps}      auth:
        condition: service_healthy
    ports: ["{port}:{port}"]
    healthcheck:
      test: ["CMD-SHELL", "wget -q --spider http://localhost:{port}/actuator/health || exit 1"]
      interval: 15s
      retries: 20
    networks:
      factory:
        aliases: [{name}.factory.local]
"""
text = text.replace("services: {}\n", "services:\n")
open(path, "w").write(text.rstrip("\n") + "\n" + block)
PY

if [[ -n "$DB" ]]; then
  SQL="$DEV/docker-mounts/postgres/init-scripts/01_create_databases.sql"
  BLOCK="CREATE DATABASE \"$DB\";
CREATE ROLE ${DB}_app WITH LOGIN PASSWORD '${DB}_app';
ALTER DATABASE \"$DB\" OWNER TO ${DB}_app;"
  grep -q "CREATE DATABASE \"$DB\";" "$SQL" || printf '\n-- %s\n%s\n' "$NAME" "$BLOCK" >> "$SQL"
  if (cd "$DEV" && docker compose ps --status running -q db 2>/dev/null | grep -q .); then
    while IFS= read -r stmt; do
      (cd "$DEV" && docker compose exec -T db psql -U postgres -q -c "$stmt" >/dev/null 2>&1) || true
    done <<<"$BLOCK"
    echo "add-service: database $DB ready in the running Postgres"
  fi
fi
grep -qxF "$NAME.factory.local" "$DEV/scripts/hosts.txt" || echo "$NAME.factory.local" >> "$DEV/scripts/hosts.txt"
[[ -f "$DEV/../$NAME/permissions.txt" ]] && python3 "$DEV/scripts/realm.py" sync "$DEV/../$NAME"
echo "add-service: $NAME on port $PORT${DB:+, database $DB} (dev-run $NAME; dev-hosts to refresh /etc/hosts)"
