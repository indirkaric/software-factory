#!/usr/bin/env python3
"""Keycloak permissions of the local realm (docker-mounts/keycloak/factory-realm.json).

A permission is what a service checks with @PreAuthorize("hasAuthority('SCOPE_READ-USER')").
In the realm it is four things, all named the same (the pattern of the old development realm):
a realm role, a client scope that puts the name into the token's `scope` claim, a scope
mapping (only holders of the role get the scope) and a default scope of the app client.
Users get roles through groups: Administrator (every permission), Manager, Employee.

  realm.py list                                  permissions and their groups
  realm.py add <PERM> [--groups Manager,Employee] [--description text]
  realm.py sync <service dir>                    add every line of <service>/permissions.txt
  realm.py apply [PERM ...]                      push the file into the running Keycloak

`add` and `sync` edit the json (idempotent) and, when Keycloak runs, apply too, because
Keycloak imports the file only on its first start; `dev-up` runs `apply` after every start,
so the json stays the one truth. Exit 0 also when nothing changed.
"""

import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

DEV = Path(__file__).resolve().parents[1]
REALM_FILE = DEV / "docker-mounts/keycloak/factory-realm.json"
ADMIN_GROUP = "Administrator"
NAME = re.compile(r"^[A-Z][A-Z0-9]*(-[A-Z0-9]+)*$")


def env(key, default):
    path = DEV / ".env"
    if path.exists():
        for line in path.read_text().splitlines():
            if line.startswith(f"{key}="):
                return line.split("=", 1)[1].strip()
    return os.environ.get(key, default)


KC = f"http://localhost:{env('AUTH_PORT', '8080')}"


def load():
    return json.loads(REALM_FILE.read_text())


def save(realm):
    REALM_FILE.write_text(json.dumps(realm, indent=2) + "\n")


def client(realm):
    return next(c for c in realm["clients"] if c["clientId"] == "factory-app")


def group(realm, name):
    g = next((g for g in realm["groups"] if g["name"] == name), None)
    if g is None:
        g = {"name": name, "path": f"/{name}", "realmRoles": [], "subGroups": []}
        realm["groups"].append(g)
    return g


def permissions(realm):
    return {s["clientScope"]: s["roles"] for s in realm.get("scopeMappings", [])}


def add(realm, perm, groups=(), description=""):
    """Add one permission to the json; returns True when something changed."""
    if not NAME.match(perm):
        sys.exit(f"realm: '{perm}' is not a permission name (UPPER-KEBAB, e.g. READ-USER)")
    before = json.dumps(realm, sort_keys=True)
    roles = realm.setdefault("roles", {}).setdefault("realm", [])
    if not any(r["name"] == perm for r in roles):
        roles.append({"name": perm, "description": description})
    scopes = realm.setdefault("clientScopes", [])
    if not any(s["name"] == perm for s in scopes):
        scopes.append({"name": perm, "description": description, "protocol": "openid-connect",
                       "attributes": {"include.in.token.scope": "true",
                                      "display.on.consent.screen": "false"}})
    mappings = realm.setdefault("scopeMappings", [])
    if not any(m.get("clientScope") == perm for m in mappings):
        mappings.append({"clientScope": perm, "roles": [perm]})
    defaults = client(realm).setdefault("defaultClientScopes", [])
    if perm not in defaults:
        defaults.append(perm)
    for name in {ADMIN_GROUP, *groups}:
        g = group(realm, name)
        if perm not in g["realmRoles"]:
            g["realmRoles"].append(perm)
    return json.dumps(realm, sort_keys=True) != before


def parse_permissions_txt(path):
    """<service>/permissions.txt: `PERMISSION  Group,Group  # comment`, # starts a comment."""
    out = []
    for line in path.read_text().splitlines():
        line = line.split("#", 1)[0].strip()
        if line:
            parts = line.split()
            out.append((parts[0], [g for g in ",".join(parts[1:]).split(",") if g]))
    return out


# --- the running Keycloak -------------------------------------------------------------

def http(method, url, token=None, body=None, form=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    data = None
    if form is not None:
        data = urllib.parse.urlencode(form).encode()
    elif body is not None:
        data, headers["Content-Type"] = json.dumps(body).encode(), "application/json"
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        return e.code, None


def own_keycloak_running():
    """Only this repo's `auth` container counts; another stack's Keycloak on the port is never touched."""
    import subprocess
    try:
        r = subprocess.run(["docker", "compose", "port", "auth", "8080"],
                           cwd=DEV, capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.SubprocessError):
        return False
    # Running, and published on the port this repo's .env names (not someone else's Keycloak).
    return r.returncode == 0 and r.stdout.strip().endswith(f":{KC.rsplit(':', 1)[1]}")


def admin_token():
    if not own_keycloak_running():
        return None
    try:
        status, body = http("POST", f"{KC}/realms/master/protocol/openid-connect/token",
                            form={"grant_type": "password", "client_id": "admin-cli",
                                  "username": "admin", "password": "admin"})
    except OSError:
        return None
    return body["access_token"] if status == 200 else None


def apply(realm, only=None):
    token = admin_token()
    if not token:
        print("realm: Keycloak isn't running; the json is updated (dev-up and the factory apply it on start)")
        return False
    base = f"{KC}/admin/realms/{realm['realm']}"
    _, live_client = http("GET", f"{base}/clients?clientId=factory-app", token)
    if not live_client:
        print(f"realm: the running Keycloak has no realm {realm['realm']} with client factory-app; skipped")
        return False
    cid = live_client[0]["id"]
    _, live_scopes = http("GET", f"{base}/client-scopes", token)
    scope_ids = {s["name"]: s["id"] for s in live_scopes}
    _, live_groups = http("GET", f"{base}/groups", token)
    group_ids = {g["name"]: g["id"] for g in live_groups}
    perms = permissions(realm)
    for perm in sorted(only or perms):
        if perm not in perms:
            sys.exit(f"realm: {perm} isn't in {REALM_FILE.name}; add it first")
        desc = next((r.get("description", "") for r in realm["roles"]["realm"] if r["name"] == perm), "")
        http("POST", f"{base}/roles", token, {"name": perm, "description": desc})
        _, role = http("GET", f"{base}/roles/{urllib.parse.quote(perm)}", token)
        if perm not in scope_ids:
            http("POST", f"{base}/client-scopes", token,
                 {"name": perm, "protocol": "openid-connect",
                  "attributes": {"include.in.token.scope": "true", "display.on.consent.screen": "false"}})
            _, live_scopes = http("GET", f"{base}/client-scopes", token)
            scope_ids = {s["name"]: s["id"] for s in live_scopes}
        sid = scope_ids[perm]
        http("POST", f"{base}/client-scopes/{sid}/scope-mappings/realm", token, [role])
        http("PUT", f"{base}/clients/{cid}/default-client-scopes/{sid}", token)
        for g in realm["groups"]:
            if perm in g["realmRoles"]:
                if g["name"] not in group_ids:
                    http("POST", f"{base}/groups", token, {"name": g["name"]})
                    _, live_groups = http("GET", f"{base}/groups", token)
                    group_ids = {x["name"]: x["id"] for x in live_groups}
                http("POST", f"{base}/groups/{group_ids[g['name']]}/role-mappings/realm", token, [role])
    print(f"realm: applied {len(only or perms)} permission(s) to the running Keycloak")
    return True


def main(argv):
    if not argv or argv[0] in ("-h", "--help"):
        sys.exit(__doc__.strip())
    realm, cmd = load(), argv[0]
    if cmd == "list":
        member = {p: [g["name"] for g in realm["groups"] if p in g["realmRoles"]] for p in permissions(realm)}
        for perm, groups in sorted(member.items()):
            print(f"{perm:32} {','.join(groups)}")
    elif cmd == "add" and len(argv) >= 2:
        groups, desc, rest = [], "", argv[2:]
        while rest:
            if rest[0] == "--groups" and len(rest) > 1:
                groups = [g for g in rest[1].split(",") if g]
            elif rest[0] == "--description" and len(rest) > 1:
                desc = rest[1]
            else:
                sys.exit("realm: add <PERM> [--groups A,B] [--description text]")
            rest = rest[2:]
        changed = add(realm, argv[1], groups, desc)
        save(realm)
        print(f"realm: {argv[1]} {'added' if changed else 'already there'}")
        apply(realm, [argv[1]])
    elif cmd == "sync" and len(argv) == 2:
        txt = Path(argv[1]).expanduser() / "permissions.txt"
        if not txt.exists():
            print(f"realm: no {txt}, nothing to sync")
            return
        wanted = parse_permissions_txt(txt)
        added = [p for p, groups in wanted if add(realm, p, groups)]
        save(realm)
        print(f"realm: {len(wanted)} permission(s) in {txt}, {len(added)} new: {' '.join(added) or '-'}")
        apply(realm, [p for p, _ in wanted])
    elif cmd == "apply":
        apply(realm, argv[1:] or None)
    else:
        sys.exit(__doc__.strip())


if __name__ == "__main__":
    main(sys.argv[1:])
