#!/usr/bin/env python3
"""Instantiate a new microservice repo from ../skeleton and this template's Claude setup.

Deterministic on purpose: the infrastructure is copied, never re-generated, so every
service starts from the same verified code. Usage:

    python3 scripts/new_service.py --name invoice --role persistence --domain Invoice \
        [--calls identity] [--port 50001] [--package-prefix com.acme | --root-package com.acme.invoice] \
        [--db invoice] [--api-prefix invoice] [--coverage 0.80] [--target ../invoice] \
        [--registry <platform.md>]

Other services are known only through the registry (--registry; default this template's
empty .claude/skills/inter-service-propagation/platform.md). The new repo gets a snapshot of it
there, and the new service is appended to it. The script never reads a sibling repo. Changes needed outside the new repo are written to
<target>/PLATFORM_CHANGES.md, not applied.
"""
import argparse
import re
import shutil
import sys
from pathlib import Path

TEMPLATE = Path(__file__).resolve().parent.parent
SKELETON = TEMPLATE / "skeleton"
REGISTRY_REL = ".claude/skills/inter-service-propagation/platform.md"
REGISTRY = TEMPLATE / REGISTRY_REL
FIRST_PORT = 50001

# Paths (relative to the instantiated repo) that only a persistence service keeps.
PERSISTENCE_ONLY = [
    "src/main/java/ROOT/db",
    "src/main/java/ROOT/entity",
    "src/main/java/ROOT/service/audit",
    "src/test/java/ROOT/service/audit",
    "src/main/resources/liquibase",
    "src/main/java/ROOT/util/SortHelper.java",
    "src/test/java/ROOT/util/SortHelperTest.java",
] + [  # the reference module: {{Domain}} from entity to controller
    f"src/{kind}/java/ROOT/{rel}" for kind in ("main", "test") for rel in (
        "repository", "mapper", "modules", "service/validator", "service/generator", "service/{{domain_package}}",
        "service/model/{{Domain}}ListFilter.java", "dto/{{Domain}}DTO.java")
]
# Template-only Claude files that must not land in a generated service.
TEMPLATE_ONLY = [
    ".claude/settings.local.json",
]
BLOCK_PATTERNS = [
    re.compile(r"[ \t]*<!-- @persistence:start -->.*?<!-- @persistence:end -->[ \t]*\n", re.S),
    re.compile(r"# @persistence:start\n.*?# @persistence:end\n", re.S),
]
MARKERS = re.compile(r"[ \t]*(<!-- @persistence:(start|end) -->|# @persistence:(start|end))[ \t]*\n")
NAME = re.compile(r"^[a-z][a-z0-9]*(-[a-z0-9]+)*$")


def package_word(name):
    """A user name as a package segment: lowercase letters and digits, never starting with a digit."""
    import unicodedata
    ascii_name = unicodedata.normalize("NFKD", name or "").encode("ascii", "ignore").decode()
    word = re.sub(r"[^a-z0-9]", "", ascii_name.lower())
    return ("u" + word if word[:1].isdigit() else word)


def default_prefix(owner=""):
    """com.<user>: the machine's user, else the GitHub account (setup's), else com.example."""
    import getpass
    conf = Path.home() / ".config/software-factory/config.env"
    if not owner and conf.exists():
        owner = next((l.split("=", 1)[1].strip() for l in conf.read_text().splitlines()
                      if l.startswith("GITHUB_OWNER=")), "")
    try:
        machine = getpass.getuser()
    except Exception:  # no user name in the environment
        machine = ""
    generic = {"", "root", "user", "admin", "ubuntu", "runner", "vscode", "codespace", "dev", "developer"}
    for name in (machine, owner):
        word = package_word(name)
        if word not in generic and word not in {"abstract", "class", "package", "import", "new", "default"}:
            return f"com.{word}"
    return "com.example"


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--name", required=True, help="kebab-case service name, e.g. invoice")
    parser.add_argument("--port", type=int, help="default: lowest free port from the registry")
    parser.add_argument("--calls", default="", help="comma-separated downstream services from the registry")
    parser.add_argument("--role", required=True, choices=["persistence", "proxy"])
    parser.add_argument("--domain", required=True, help="PascalCase first domain, e.g. Invoice")
    parser.add_argument("--root-package", help="default: <package-prefix>.<name with underscores>")
    parser.add_argument("--package-prefix", help="default: com.<your user name> (default_prefix)")
    parser.add_argument("--registry", type=Path, help="platform.md to read and append to (default: the template's)")
    parser.add_argument("--db", help="database and schema name, default: name with underscores")
    parser.add_argument("--api-prefix")
    parser.add_argument("--coverage", default="0.80")
    parser.add_argument("--target")
    parser.add_argument("--no-register", action="store_true", help="don't add the service to the registry (dry runs)")
    args = parser.parse_args()

    if not NAME.match(args.name):
        sys.exit(f"--name must be kebab-case: {args.name}")
    if not re.match(r"^[A-Z][A-Za-z0-9]{0,19}$", args.domain):
        sys.exit(f"--domain must be PascalCase, at most 20 characters: {args.domain}")
    if not re.match(r"^0(\.\d+)?$|^1(\.0+)?$", args.coverage) or float(args.coverage) < 0.80:
        sys.exit(f"--coverage must be between 0.80 and 1: {args.coverage}")
    snake = args.name.replace("-", "_")
    args.root_package = args.root_package or f"{args.package_prefix or default_prefix()}.{snake}"
    if not re.match(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$", args.root_package):
        sys.exit(f"--root-package is not a valid package: {args.root_package}")
    args.db = args.db or snake
    args.api_prefix = args.api_prefix or args.name
    args.target = Path(args.target or TEMPLATE.parent / args.name).resolve()
    args.calls = [c.strip() for c in args.calls.split(",") if c.strip()]
    return args


def read_registry():
    """Rows of the platform table plus the extra taken alias names."""
    text = REGISTRY.read_text(encoding="utf-8")
    rows = {}
    for line in text.splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) == 6 and cells[2].isdigit():
            rows[cells[0]] = {"alias": cells[1], "port": int(cells[2]), "base": cells[3], "unknown": cells[4]}
    taken = re.search(r"Reserved names \(not services\): (.*?)\.\n", text, re.S)
    names = set(rows) | ({n.strip() for n in taken.group(1).replace("\n", " ").split(",")} if taken else set())
    return rows, names


def check_against_registry(args, rows, names):
    if args.name in names:
        sys.exit(f"Name already taken in {REGISTRY.name}: {args.name}")
    used = {row["port"] for row in rows.values()}
    if args.port is None:
        args.port = next(p for p in range(FIRST_PORT, 65536) if p not in used)
    if not 1024 < args.port < 65536 or args.port in used:
        sys.exit(f"Port not available: {args.port}")
    for callee in args.calls:
        if callee not in rows or not rows[callee]["base"].startswith("http"):
            sys.exit(f"--calls: {callee} has no base URI in {REGISTRY.name}")


def label(name):
    return "-".join(part.capitalize() for part in name.split("-"))


def wire_calls(args, values, rows):
    """Base URIs in application.yaml and outbound client ids in Constants."""
    if not args.calls:
        return
    yaml = args.target / "src/main/resources/application.yaml"
    entries = "".join(f'  {c}:\n    base-uri: "{rows[c]["base"]}"\n' for c in args.calls)
    text = yaml.read_text(encoding="utf-8")
    yaml.write_text(re.sub(r"# One entry per downstream.*?client: \{\}\n", "client:\n" + entries, text, flags=re.S),
                    encoding="utf-8")
    constants = args.target / "src/main/java" / values["{{rootPath}}"] / "constants/Constants.java"
    ids = "".join(f'    public static final String {c.upper().replace("-", "_")}_CLIENT = '
                  f'"{values["{{ServiceName}}"]}.{label(c)}";\n\n' for c in args.calls)
    text = constants.read_text(encoding="utf-8")
    anchor = "    /** Inbound caller ids"
    constants.write_text(text.replace(anchor, "    /** Outbound caller ids, one per downstream. */\n" + ids + anchor, 1),
                         encoding="utf-8")


def register(args, values):
    base = f"http://{args.name}.factory.local:{args.port}/{args.api_prefix}/"
    row = f"| {args.name} | {args.name}.factory.local | {args.port} | {base} | nothing | {args.role} |"
    text = REGISTRY.read_text(encoding="utf-8")
    last = [line for line in text.splitlines() if line.startswith("|") and line.rstrip().endswith("|")][-1]
    REGISTRY.write_text(text.replace(last, last + "\n" + row, 1), encoding="utf-8")


def platform_changes(args, values, rows):
    """Changes other repos need, as issues to open there; the development repo is wired by new-service.sh."""
    svc = values["{{ServiceName}}"]
    out = [f"# Changes outside `{args.name}`\n",
           "The development repo (compose entry, database, host alias) is registered by",
           "`software-factory new`. What is left needs an issue in each named repo.\n"]
    for callee in args.calls:
        if rows[callee]["unknown"] == "nothing":
            out.append(f"""## {callee}: trust `{svc}.{label(callee)}`, otherwise every call is a 403

`constants/Constants.java`: add `"{svc}.{label(callee)}"` to `TRUSTED_CLIENTS` (or grant it
only the authorities the called endpoints need in `ServiceCommunicationIdFilter`).
""")
    out.append(f"""## A proxy that will call `{args.name}`

`client.{args.name}.base-uri: "http://{args.name}.factory.local:{args.port}/{args.api_prefix}/"` and a
client id `<Caller>.{svc}`; add it to `{args.name}`'s `Constants.TRUSTED_CLIENTS`.
""")
    (args.target / "PLATFORM_CHANGES.md").write_text("\n".join(out), encoding="utf-8")


def placeholders(args):
    pascal = "".join(part.capitalize() for part in args.name.split("-"))
    return {
        "{{rootPackage}}": args.root_package,
        "{{rootPath}}": args.root_package.replace(".", "/"),
        "{{service-name}}": args.name,
        "{{ServiceName}}": pascal,
        "{{db}}": args.db,
        "{{schema}}": args.db,
        "{{port}}": str(args.port),
        "{{api-prefix}}": args.api_prefix,
        "{{coverage}}": args.coverage,
        "{{Domain}}": args.domain,
        "{{domain}}": args.domain[0].lower() + args.domain[1:],
        "{{domain_table}}": snake_case(args.domain),
        "{{domain_package}}": snake_case(args.domain),
        "{{domain-path}}": plural(snake_case(args.domain).replace("_", "-")),
    }


def snake_case(pascal):
    """PurchaseOrder -> purchase_order."""
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", pascal).lower()


def plural(word):
    """purchase-order -> purchase-orders, category -> categories, box -> boxes."""
    if re.search(r"[^aeiou]y$", word):
        return word[:-1] + "ies"
    if re.search(r"(s|x|z|ch|sh)$", word):
        return word + "es"
    return word + "s"


def fill(text, values):
    for key, value in values.items():
        text = text.replace(key, value)
    return text


def service_facts(args, values):
    return f"""
## Service facts

| | |
|---|---|
| Port | `{args.port}` |
| API prefix | `/{args.api_prefix}/v1` |
| Host alias | `{args.name}.factory.local` |
| Root package | `{args.root_package}` |
| Role | `{args.role}` |
| Database / schema | {"`" + args.db + "` (role `" + args.db + "_app`)" if args.role == "persistence" else "none (proxy)"} |
| Inbound filters | service-communication-id → correlation-id → user-id → Keycloak JWT (realm `factory`) |
| Inbound callers | `{values["{{ServiceName}}"]}` accepts `Constants.TRUSTED_CLIENTS` only (`UI.{values["{{ServiceName}}"]}`) |
| Outbound clients | {', '.join('`' + c + '`' for c in args.calls) or 'none'} — base URIs in `application.yaml`, ids in `Constants`; each wrapper extends `client/BaseClient` |
| Coverage gate | `{args.coverage}` instruction and line coverage (never below 0.80) |
| Permissions | `permissions.txt` (the factory adds them to the development realm) |
| CI | `.github/workflows/ci.yml`: `mvn verify` on every push and PR{" + Liquibase update/rollback test" if args.role == "persistence" else ""} |
| Local run | `development` repo: `dev-up`, then `mvn spring-boot:run` (see README.md) |

Infrastructure comes from the software factory's skeleton
(`~/.config/software-factory/templates/java/skeleton`); copy shapes from there, not from other services.
"""


def main():
    global REGISTRY
    args = parse_args()
    if args.registry:
        REGISTRY = args.registry.expanduser().resolve()
        if not REGISTRY.exists():
            sys.exit(f"--registry {REGISTRY} doesn't exist")
    if args.target.exists() and any(args.target.iterdir()):
        sys.exit(f"Target is not empty, refusing to overwrite: {args.target}")
    rows, names = read_registry()
    check_against_registry(args, rows, names)
    values = placeholders(args)

    shutil.copytree(SKELETON, args.target, dirs_exist_ok=True)
    shutil.copy2(TEMPLATE / "CLAUDE.md", args.target / "CLAUDE.md")
    shutil.copytree(TEMPLATE / ".claude", args.target / ".claude", dirs_exist_ok=True)
    shutil.copy2(REGISTRY, args.target / REGISTRY_REL)  # the registry as it is now

    for rel in TEMPLATE_ONLY + (PERSISTENCE_ONLY if args.role == "proxy" else []):
        path = args.target / rel
        if path.is_dir():
            shutil.rmtree(path)
        elif path.exists():
            path.unlink()

    for path in [p for p in args.target.rglob("*") if p.is_file()]:
        text = path.read_text(encoding="utf-8")
        if args.role == "proxy":
            for pattern in BLOCK_PATTERNS:
                text = pattern.sub("", text)
        text = MARKERS.sub("", fill(text, values))
        if path.name == "CLAUDE.md" and path.parent == args.target:
            text = re.sub(r"\n> \*\*Fill these in.*?\n\n", "\n", text, count=1, flags=re.S)
            text = text.rstrip() + "\n" + service_facts(args, values)
        path.write_text(text, encoding="utf-8")

    for root in ("src/main/java", "src/test/java"):
        src = args.target / root / "ROOT"
        if src.exists():
            dest = args.target / root / values["{{rootPath}}"]
            dest.parent.mkdir(parents=True, exist_ok=True)
            src.rename(dest)
    for path in sorted(args.target.rglob("*{{*"), key=lambda p: len(p.parts), reverse=True):
        path.rename(path.with_name(fill(path.name, values)))

    wire_calls(args, values, rows)

    left = [str(p.relative_to(args.target)) for p in args.target.rglob("*")
            if "{{" in p.name or (p.is_file() and re.search(r"(?<!\$)\{\{", p.read_text(encoding="utf-8")))]
    if left:
        sys.exit("Unfilled placeholders in: " + ", ".join(left))

    platform_changes(args, values, rows)
    if not args.no_register:
        register(args, values)
    print(f"Created {args.target} ({args.role}, port {args.port}, package {args.root_package})")
    print(f"Registry: {'skipped' if args.no_register else REGISTRY}; outside changes: {args.target / 'PLATFORM_CHANGES.md'}")
    print(f"Next: cd {args.target} && mvn verify")


if __name__ == "__main__":
    main()
