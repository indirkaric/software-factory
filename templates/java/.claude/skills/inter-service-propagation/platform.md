# Platform registry

The only source for other services' addresses. Never read a sibling repo to find them.
`software-factory new` reads this table to reject a taken port or name, and appends every
service it creates (the factory repo commits and pushes the change).

`Unknown caller` = what the callee's `ServiceCommunicationIdFilter` grants a
`service-communication-id` it does not know. `nothing` means 403 until that repo adds the caller
to its `TRUSTED_CLIENTS` — an edit in that repo, made through an issue there.

| Service | Host alias | Port | Base URI | Unknown caller | Notes |
|---|---|---|---|---|---|

Reserved names (not services): development, keycloak, auth, db, database, aws, redis, azure.

New services start at port 50001 and take the lowest free port at or above it.
