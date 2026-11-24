---
name: permissions-and-tokens
description: Use when adding or changing authorities on endpoints, users/groups/permissions, or personal access tokens (PAT), in an identity-like service or in a service that checks them.
---

The permission model, distilled from the `identity` service with its gaps fixed.

## Authorities

- A permission is an UPPER-KEBAB `VERB-NOUN` code: `READ-INVOICE`, `EDIT-INVOICE`,
  `ADD-GROUP`, `READ-MY-PAT`. Spring sees it as `SCOPE_<CODE>`.
- Keycloak grants it as a realm role plus a client scope of the same name (only holders get
  the scope); `KeycloakRolesConverter` maps both to `SCOPE_<CODE>`. Users get permissions
  through groups: `Administrator` (all), `Manager`, `Employee`.
- **Every permission the code checks is a line in `permissions.txt`** (`READ-INVOICE
  Manager,Employee`); `PermissionsDeclaredTest` fails otherwise. The software factory adds the
  file to the development repo's realm (`scripts/realm.py sync`); never edit the realm here.
- Endpoints use constants, never inline strings:
  `public static final String READ_INVOICE = "hasAuthority('SCOPE_READ-INVOICE')";` in
  `Constants.Authority`, then `@PreAuthorize(Constants.Authority.READ_INVOICE)`.
- Every endpoint has a `@PreAuthorize`, except the whitelisted health and OpenAPI paths.

## Users, groups, permissions (when a service owns them)

Tables `user`, `group`, `permission` (`entity_id` = the code), `group_permission`,
`user_group`. A user's authorities are the union of its groups' permissions; there is no
role table. Seed permissions in a changeset with a `context`, and roll back only those rows.

## Service-to-service

`service-communication-id: <Caller>.<Callee>`. Only ids in `Constants.TRUSTED_CLIENTS` get
authorities; an unknown id gets none (never a default "all").

## Personal access tokens

- Header `Authorization: PersonalToken <raw>`. The raw token is shown once, at creation.
- Raw token: 32+ bytes from `SecureRandom`, Base64-URL, prefixed with a public lookup id
  (`<id>.<secret>`); store `id` (unique, indexed) and a BCrypt hash of the secret, never the
  raw token. Look up by id, then `matches` the hash; no prefix-of-the-secret lookups.
- A PAT has `expires_at`, `is_active`, `last_used_at` and `scopes`. Authentication returns
  the **intersection** of the token's scopes and the user's current permissions.
- A proxy authenticates PATs by calling the owning service's authenticate endpoint and caches
  the result briefly (e.g. 60 s, keyed by the token id), so not every request costs a BCrypt.
- Tests: creation returns the raw token once; expired, inactive, wrong secret → 401; scopes
  narrow the authorities.
