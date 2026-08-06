# M11 Phase E — E4/E5 authenticated transport validation

## Status

PASS on 2026-08-05.

This document records deterministic CI evidence for authenticated text transport and two-session Operator Lore transport. It does not replace the irreducible installed visual client canaries, request a version, create a tag or publish a release.

## E4 — authenticated text turn

Exact validated head:

```text
5eafb58b6e3a7963048a5b62c92af3cc1e28e54b
```

Mandatory workflows:

| Gate | Run | Result |
| --- | ---: | --- |
| VillAIgence CI | 1631 / `31041799377` | PASS |
| Java Pull Request CI with Gradle | 1017 / `31041799448` | PASS |
| Repository security policy | 1167 / `31041799335` | PASS |
| VillAIgence GitHub Release dry-run | 243 / `31041799440` | PASS |

The release workflow ran in dry-run mode and did not publish a GitHub release.

### Proven contract

The vanilla chat packet Mixin retains the authenticated `ServerPlayer` supplied by the connection and resolves the target NPC from live server state before dispatch. No player UUID or NPC UUID is accepted from the chat message.

The Minecraft-bound bridge creates a session from:

```text
player.getUUID()
villager.getUUID()
```

It delegates to a package-private loader-independent core. The deterministic acceptance test proves:

- one eligible normalized text message invokes the provider exactly once;
- the provider receives the authenticated player and server-resolved NPC session;
- one successful turn creates exactly one Memory 2.0 `DIALOGUE` event;
- the event owner is the exact NPC UUID;
- event participants are the exact NPC UUID and authenticated player UUID;
- one non-blank answer is delivered exactly once through the same session;
- an empty provider result creates no response and no dialogue event;
- missing authenticated or resolved identity is rejected before provider execution;
- command-like and blank input remain ineligible;
- the production Mixin no longer owns asynchronous provider or response-delivery orchestration.

### Layer boundary

Minecraft types remain only in `AuthenticatedTextTurn`. `AuthenticatedTextTurnCore` contains no Minecraft or loader dependency, which prevents common-test classloading from silently relying on a Fabric runtime.

## E5 — authenticated two-session Operator Lore

Exact validated head:

```text
2310f8032cf18502705b5cf186ec849bc4b14e8c
```

Mandatory workflows:

| Gate | Run | Result |
| --- | ---: | --- |
| VillAIgence CI | 1637 / `31042390141` | PASS |
| Java Pull Request CI with Gradle | 1023 / `31042390080` | PASS |
| Repository security policy | 1179 / `31042389957` | PASS |
| VillAIgence GitHub Release dry-run | 249 / `31042389847` | PASS |

The release workflow ran in dry-run mode and did not publish a GitHub release.

### Proven contract

Both Operator Lore C2S packets now delegate to one Minecraft bridge. Packet handlers no longer invoke authority or `Network.sendToPlayer` directly.

The bridge:

- receives the authenticated `ServerPlayer` from the network framework;
- binds a session envelope to `player.getUUID()`;
- invokes the production `OperatorLoreServerAuthority` once;
- echoes the bounded request ID through `OperatorLoreProtocolPolicy`;
- converts the result to `OperatorLoreResponse`;
- sends the response only to the same `ServerPlayer`.

The loader-independent two-session acceptance proves:

1. both clients load the same canonical revision;
2. session A writes a canonical value successfully;
3. session B submits a stale retained draft;
4. session B receives canonical `CONFLICT` value and revision in its own inbox;
5. session A never receives session B's response and vice versa;
6. conflict does not mutate canonical bytes;
7. session B keeps its client-owned draft and retries with the current revision;
8. the explicit retry succeeds exactly once;
9. one request invokes authority and response sink exactly once;
10. request correlation is preserved;
11. unauthorized read and write return `FORBIDDEN` without value or revision disclosure;
12. unauthorized and invalid requests do not mutate canonical persistence.

### Layer boundary

`OperatorLoreNetworkSessionCore` is package-private and contains no Minecraft or packet dependency. `OperatorLoreNetworkSession` is the only Minecraft bridge. Revision conflict and persistence remain owned by the existing canonical authority/store code.

## Catalog decision

Two new deterministic scenarios are recorded:

```text
VAI-GAME-005   AUTOMATED   authenticated text ownership and exactly-once effects
VAI-CONCUR-005 AUTOMATED   authenticated two-session conflict and response isolation
```

The existing installed canaries remain, but their scope is narrowed:

- `VAI-GAME-001` now covers only exact installed-client NPC selection and visible response rendering;
- `VAI-CONCUR-004` now covers only real two-client conflict UI, reload/keep-draft presentation and human review before retry.

Provider behavior, persistence, authorization, request correlation, session isolation and stale-write safety no longer require repeated manual regression.
