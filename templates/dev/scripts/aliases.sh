# Development shortcuts. Load them with: source <this repo>/scripts/aliases.sh
# (scripts/install-aliases.sh adds that line to ~/.bashrc and ~/.zshrc). `dev-help` lists them.
DEV_HOME="$(cd "$(dirname "${BASH_SOURCE[0]:-${(%):-%x}}")/.." && pwd)"
_dc() { (cd "$DEV_HOME" && docker compose "$@"); }
_port() { local v; v="$(grep -m1 "^$1=" "$DEV_HOME/.env" 2>/dev/null | cut -d= -f2)"; echo "${v:-$2}"; }

dev-up()      { _dc up -d --wait db auth aws "$@" && python3 "$DEV_HOME/scripts/realm.py" apply; }  # infra (+ services named); realm json → Keycloak
dev-up-all()  { _dc up -d --wait; }                         # infrastructure + every service
dev-down()    { _dc stop; }                                  # stop, keep data
dev-reset()   { read -rp "Delete ALL local data (databases, Keycloak, S3)? [y/N] " a && [[ "$a" == y ]] && _dc down -v; }
dev-ps()      { _dc ps; }
dev-logs()    { _dc logs -f --tail=200 "$@"; }
dev-psql()    { _dc exec db psql -U postgres "${1:-postgres}"; }
dev-build()   { local s="${1:?service}"; (cd "$DEV_HOME/../$s" && mvn -q -B -DskipTests package) && _dc build "$s"; }
dev-run()     { local s="${1:?service}"; dev-build "$s" && _dc up -d --wait "$s"; }
dev-stop()    { _dc stop "${1:?service}"; }
dev-hosts()   { "$DEV_HOME/scripts/setup-hosts.sh"; }
dev-add()     { "$DEV_HOME/scripts/add-service.sh" "$@"; }
dev-token()   { curl -s -d grant_type=password -d client_id=factory-app -d "username=${1:-admin}" -d "password=${1:-admin}" \
                  "http://localhost:$(_port AUTH_PORT 8080)/realms/factory/protocol/openid-connect/token" | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])'; }
dev-curl()    { curl -s -H "Authorization: Bearer $(dev-token "${DEV_USER:-admin}")" "$@"; }
dev-s3()      { AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test aws --region eu-west-1 --endpoint-url "http://localhost:$(_port AWS_PORT 4566)" s3 "$@"; }
dev-azure()   { _dc up -d azurite; }
dev-redis()   { _dc up -d --wait redis; }
dev-perm()    { python3 "$DEV_HOME/scripts/realm.py" "${@:-list}"; }   # Keycloak permissions
dev-help() {
  cat <<'HELP'
dev-up [svc..]   start Postgres, Keycloak, LocalStack (and the named services)
dev-up-all       infrastructure + every service in docker-compose.services.yml
dev-run <svc>    mvn package ../<svc>, build its image, start it
dev-stop <svc>   stop one service          dev-down   stop everything (data kept)
dev-reset        stop and DELETE all data  dev-ps     what runs
dev-logs [svc]   follow logs               dev-psql [db]   psql as postgres
dev-token [user] access token (admin|manager|user|basic, password = name)
dev-curl <url>   curl with a token (DEV_USER=manager dev-curl ...)
dev-s3 <args>    aws s3 against LocalStack (bucket factory-local)
dev-azure        start Azurite              dev-redis   start Redis
dev-perm [list]  permissions and groups; dev-perm add READ-X --groups Manager
dev-perm sync ../<svc>   add a service's permissions.txt to the realm (live too)
dev-hosts        map *.factory.local to 127.0.0.1 in /etc/hosts (sudo)
dev-add <name> <port> [--db <db>]   register a service (software-factory new does it)
Keycloak admin: http://localhost:8080 (admin/admin); ports in .env
HELP
}
