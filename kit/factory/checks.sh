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
