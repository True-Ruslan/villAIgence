# S10b validation — server-authoritative operator lore API

Date: 2026-08-01  
Target branch: `1.21.1`

## Purpose

Expose the S9 operator-lore store to a future client editor through a bounded, permission-gated and optimistic-concurrency-protected server API. The client is never authoritative for identities, village membership, permissions or persistence.

## Authority contract

- READ and WRITE require `ServerPlayer.hasPermissions(2)`.
- WORLD resolves to the current server world.
- PLAYER resolves to the authenticated sender UUID; the packet contains no player UUID.
- VILLAGER accepts only a runtime entity ID and resolves a live `VillagerEntityMCA` in the sender's current level.
- VILLAGER/VILLAGE targets must be within 64 blocks.
- VILLAGE derives dimension and stable MCA village ID from the resolved villager's home village.
- Arbitrary UUID, dimension and village ID are absent from the wire format.
- Writes require the SHA-256 revision of the current canonical value.
- Stale writes return `CONFLICT` plus the current canonical value/revision.
- Exact replay returns `UNCHANGED` and does not rewrite storage.
- Invalid scope, missing target, forbidden access and persistence failures are explicit statuses.
- C2S payload handling runs through the existing server executor and common `MessagesMCA` registration path.

## Payload contract

```text
maximum lore code points: 4096
maximum lore UTF-8 bytes:  12288
revision:                  64 lowercase SHA-256 hex characters
scope/status labels:       max 16 characters
```

Validation occurs twice:

1. bounded `stringUtf8(...)` packet codecs limit allocation during decode;
2. `OperatorLoreEditorPolicy` validates code points, UTF-8 bytes and control characters before storage.

Stored lore that predates the transport limit is handled safely: the response sends an empty value with status `INVALID`, while preserving the SHA-256 revision of the original stored content. An operator can therefore replace or clear it without packet encoding failure and without bypassing optimistic concurrency.

## Wire messages

### C2S

- `OperatorLoreReadRequest(scope, villagerEntityId)`
- `OperatorLoreWriteRequest(scope, villagerEntityId, expectedRevision, value)`

### S2C

- `OperatorLoreResponse(scope, villagerEntityId, status, canonicalValue, revision)`

The response is stored in a minimal client mailbox for S10c. This package contains no screen, button or client-side authority decision.

## TDD evidence

### Canonical RED — authority policy

```text
head: f442660ec79f555a6e3f3866435d0b9aa0067238
VillAIgence CI #1103 / 30704249852 — expected FAILURE
boundary: common:compileTestJava
reason:
  - OperatorLoreEditorPolicy absent
  - OperatorLoreTargetPolicy absent
```

Loader CI #610 and security #412 remained green because production code had not changed.

### Intermediate GREEN

```text
head: 3ca9c32a0437c97048a6f62cde2dffa69f064fb6
VillAIgence CI #1112 / 30704578635              SUCCESS
Java Pull Request CI #619 / 30704578637       SUCCESS
Repository security policy #430 / 30704578668 SUCCESS
```

### Additional RED — oversized stored value

```text
head: 3d3384e9b3fefb321820dad3094e15f7c98a83dc
VillAIgence CI #1113 / 30704737126 — expected FAILURE
boundary: common:compileTestJava
reason: NetworkView/networkView absent
```

Loader CI #620 and security #432 remained green.

### Final GREEN

```text
head: 84d97bf0af9f6e8c2f10a23792b46f77a30ec164
VillAIgence CI #1115 / 30704914861              SUCCESS
Java Pull Request CI #622 / 30704914859       SUCCESS
Repository security policy #436 / 30704914855 SUCCESS
```

Validated gates:

- common unit tests;
- Fabric build and payload registration;
- NeoForge build and payload registration;
- distributable Fabric package verification;
- deterministic repository security policy.

## Automated policy coverage

- operator/non-operator READ and WRITE decisions;
- deterministic content-sensitive SHA-256 revisions;
- current-revision requirement;
- stale conflict;
- unchanged replay;
- code-point limit;
- UTF-8 byte limit;
- invalid payload precedence;
- WORLD/PLAYER server resolution without client target identity;
- VILLAGER same-level/live/distance requirements;
- VILLAGE home-village requirement;
- NaN/negative distance fail-closed behavior;
- safe network view for oversized stored values;
- canonical line-ending normalization in responses.

## Manual release acceptance

1. Join as a non-operator and send valid read/write payloads; expect `FORBIDDEN` and no file change.
2. Join as permission level 2 operator and read WORLD; expect canonical value/revision.
3. Read PLAYER; verify the sender UUID is used regardless of packet entity ID.
4. Read/write VILLAGER for a nearby MCA villager; verify only that villager UUID changes.
5. Attempt a vanilla villager, removed entity, other dimension or target beyond 64 blocks; expect `NOT_FOUND`.
6. Read/write VILLAGE; verify dimension and home-village ID are derived from the target villager.
7. Send two writes with the same starting revision; the first succeeds and the second returns `CONFLICT` with current state.
8. Replay an identical value/revision; expect `UNCHANGED` and unchanged file timestamp.
9. Send forbidden control characters, excessive code points and excessive UTF-8 bytes; expect `INVALID` and no file mutation.
10. Seed the file with transport-oversized but storage-valid lore; expect `INVALID`, empty response value and revision of the original content, then clear it using that revision.
11. Restart the server and repeat reads; values and revisions must remain stable.
12. Confirm dialogue context from S10a still uses the updated lore after a successful write.

## Scope review

Expected changed areas:

- pure editor permission/revision/target policy;
- server authority service and canonical result model;
- two C2S and one S2C packet;
- common packet registration;
- minimal client response mailbox;
- focused policy tests;
- this validation document.

Explicitly excluded:

- client editor screen;
- changes to provider transport/parser/retry;
- semantic-memory ingestion;
- generated biography/personality;
- changes to existing LivingWorld schemas or files other than authorized S9 store mutations;
- dependency, workflow or script changes.
