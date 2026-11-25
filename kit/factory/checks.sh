#!/usr/bin/env bash
# The repo's checks. Exit 0 only when the branch is fit to review.
#
# The gate runs this script from the main checkout with the job's worktree as
# the working directory, so a branch can't change the checks it is judged by.
# Keep it deterministic and non-interactive: nothing that rewrites files.
#
# For a service generated from the factory skeleton, `mvn verify` runs the unit
# tests, the JaCoCo coverage gate and checkstyle (see the service's CLAUDE.md).
#
# Offline (-o) by default, like the template. If a job adds a dependency that
# isn't in ~/.m2 yet, the gate fails with "offline mode" in checks.log: run
# `mvn dependency:go-offline` once in that worktree, or set FACTORY_MVN_ONLINE=1
# in the environment of the session that runs /software-factory.
set -euo pipefail

MVN_FLAGS=(-B -ntp)
if [[ "${FACTORY_MVN_ONLINE:-0}" != "1" ]]; then
  MVN_FLAGS+=(-o)
fi

# Each worktree gets its own target/; ~/.m2 is shared and read-mostly.
mvn "${MVN_FLAGS[@]}" clean verify

# Coverage floor: the JaCoCo check must exist and never ask for less than 80 %, so a branch
# can't pass by lowering it (instruction and line coverage, see the skeleton's pom.xml).
python3 - <<'PY'
import re, sys
pom = open("pom.xml", encoding="utf-8").read()
props = dict(re.findall(r"<([\w.-]+)>([^<]*)</\1>", re.search(r"<properties>(.*?)</properties>", pom, re.S).group(1))) \
    if "<properties>" in pom else {}
mins = [props.get(m.group(1), m.group(0)) if m.group(1) else m.group(2)
        for m in re.finditer(r"<minimum>\s*(?:\$\{([\w.-]+)\}|([\d.]+%?))\s*</minimum>", pom)]
if "jacoco-maven-plugin" not in pom or "<goal>check</goal>" not in pom or not mins:
    sys.exit("checks: pom.xml has no JaCoCo check goal with a <minimum>; services need coverage >= 0.80")
for value in mins:
    ratio = float(value.rstrip("%")) / (100 if value.endswith("%") else 1)
    if ratio < 0.80:
        sys.exit(f"checks: JaCoCo minimum {value} is below 0.80")
print(f"checks: coverage floor ok ({', '.join(mins)})")
PY

# Tests ship with the code: every class with logic the branch adds (service, validator, response
# creator, controller, client, filter, helper) needs its <Class>Test next to it under src/test/java.
python3 - "$(dirname "$0")/config.json" <<'PY'
import json, re, subprocess, sys
from pathlib import Path
base = json.load(open(sys.argv[1], encoding="utf-8")).get("base_branch", "main")
mb = subprocess.run(["git", "merge-base", "HEAD", f"origin/{base}"], capture_output=True, text=True)
if mb.returncode != 0:
    sys.exit(0)  # no base to compare with (a fresh repo): nothing was added on a branch
added = subprocess.run(["git", "diff", "--name-only", "--diff-filter=A", mb.stdout.strip(), "HEAD",
                        "--", "src/main/java"], capture_output=True, text=True, check=True).stdout.split()
logic = re.compile(r"/(service|modules|client|security|util|processor|annotations/validator)/")
data = re.compile(r"(/model/|/I[A-Z]\w*\.java$|package-info\.java$|Constants\.java$)")
missing = [f for f in added if logic.search(f) and not data.search(f) and not
           Path(f.replace("src/main/java", "src/test/java", 1)[:-len(".java")] + "Test.java").exists()]
if missing:
    sys.exit("checks: classes added without their unit test:\n" + "\n".join(f"  {f}" for f in missing))
print(f"checks: every added class with logic has a test ({len(added)} added files)")
PY
