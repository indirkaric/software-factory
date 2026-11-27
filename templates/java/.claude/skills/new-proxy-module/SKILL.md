---
name: new-proxy-module
description: Ordered checklist for a new proxied (BFF-style) module that forwards to or composes other microservices instead of local persistence. Use when starting one from zero in the main conversation; proxy-service-creator follows the same steps as a subagent.
---

Copy the generated proxied module (`client/<downstream>/`, `service/<downstream>/`,
`modules/<downstream>/`) and rename it; rules per step:
`proxy-bff-conventions`, `inter-service-propagation`, `controller-conventions`. If the data needs a
local table, it belongs in the service that owns it: add the endpoint there, then proxy it here.

1. Grep `client/` and `modules/`: is the downstream endpoint already proxied? Then reuse it.
2. Per new downstream: the `platform.md` row exists → `client.<name>.base-uri` in
   `application.yaml` → a `<NAME>_CLIENT` constant (`<ThisService>.<Downstream>`). If the row says
   `nothing`, note the needed allowlist in the report.
3. `client/<downstream>/<Resource>Client extends BaseClient`: only the methods this module needs,
   each returning `exchange(...)`.
4. `modules/<downstream>/<domain>/model/`: request classes, response records.
5. `service/<downstream>/<domain>/I<Verb><Domain>Service` + `impl/`: client call →
   `ResponseUtil.handleResponse`; composition and partial-failure handling when several downstreams.
6. `modules/<downstream>/<domain>/<Domain>Controller`, one service per endpoint, `503` documented.
7. `mvn -o -q compile` and `mvn -o checkstyle:check`, then tests for steps 3, 5 and 6 (as the
   reference module's) and `mvn -o verify`.
