# Blueprint server authority

Blueprint packets are untrusted client input. Client GUI rank checks are UX only; every mutation must be authorized again on the dedicated server.

## Authority model

The current MCA `Village` model has no persistent owner UUID. This hardening therefore uses the existing per-village rank system rather than introducing an incompatible ownership schema.

Server-side rules:

- the requested village ID must match the nearest village resolved from the player's current server position;
- rank is calculated server-side with `Tasks.getRank(village, player)`;
- operators/admin overrides continue to resolve as `MONARCH` through the existing MCA override mechanism;
- taxes require `MERCHANT`;
- population threshold requires `NOBLE`;
- marriage threshold requires `MAYOR`;
- rename, remove building, force building type, auto-scan toggle, and full scan require `MAYOR`;
- local ADD/ADD_ROOM remain available from `PEASANT` so manual building discovery cannot create a circular rank-progression dependency;
- creation of the first settlement remains possible when no village exists yet.

## Input validation

- village rule ratios must be finite and inside `[0, 1]`;
- village names have control characters removed, are trimmed, and are capped at 32 Unicode code points;
- forced building types must exist in the server-side building-type registry;
- polymorph confirmations must originate near the player and use a server-known building type.

## Security boundary

The server never trusts:

- a client-provided rank;
- a client-provided village ID without locality validation;
- client-side disabled/hidden buttons;
- arbitrary building type strings;
- arbitrary remote scan coordinates.

## Remaining product question

Upstream issue #580 also asks for explicit village ownership/locking. This patch closes the concrete server-authority vulnerabilities without adding a new persistent ownership model. Explicit owner/co-owner ACLs can be designed separately if the server needs stronger social ownership semantics than MCA's rank system provides.
