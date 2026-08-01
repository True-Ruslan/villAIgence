# Upstream S2 UUID-Preserving Conversion Validation

**Implementation date:** 2026-08-01  
**Package:** S2 — UUID-preserving villager ↔ zombie-villager conversion  
**Pull request:** #73  
**VillAIgence base:** `0c776012a1b0cd58221536b09d73c2502379a737`  
**Upstream sources:** `edd6f24c97c827dd47049fdedf460ad1b8ab00b2`, `21d723792d26d33aed12fafbbfb0d07749c6851e`, `c3f92f1f7d6f745ab885dcfed350b4e60e1b8cbc`  
**Upstream audit target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`

## Identity defect

The previous MCA conversion path registered a replacement entity under a generated UUID and then changed the object UUID after registration. VillAIgence persistent domains use the NPC UUID as their identity key, so a changed or incorrectly registered UUID could disconnect the converted NPC from memory, semantic knowledge, events, relationships and voice identity.

## Adapted implementation

VillAIgence does not contain the upstream generated `chatAIPrompt` field. S2 therefore imports only the identity-preserving conversion lifecycle and keeps the existing VillAIgence post-conversion behavior authoritative.

```text
removed source
→ fail soft without creating a target

valid source
→ create target without registering it
→ capture MCA tracked-data snapshot
→ copy position, age flag, AI flag, name, persistence and invulnerability
→ optionally move equipment and preserve drop chances
→ assign the source UUID
→ apply the MCA snapshot
→ discard the source
→ register the target
→ restore the vehicle relation
```

The implementation is restricted to the MCA counterpart conversions selected by `VillagerEntityMCA` and `ZombieVillagerEntityMCA`. All unrelated `Mob.convertTo` calls retain vanilla behavior.

The existing entity-specific conversion code remains responsible for:

- villager profession/data, gossip, trade offers and XP;
- MCA inventory transfer;
- age restoration;
- infection/curing state;
- conversion effects and persistence flags.

No VillAIgence JSON schema is changed. The six world-local identity domains continue to reference the same UUID:

```text
memory.json
memory2.json
semantic-memory.json
events.json
relationships.json
voices.json
```

## TDD evidence

### Canonical RED

```text
head: 603c48ce7e44247fd319518d577693278aa8b87f
VillAIgence CI #1001 / 30695838106
result: expected FAILURE
boundary: common:compileTestJava
reason: VillagerConversionIdentityPolicy was absent at four test call sites
```

### GREEN

```text
head: d5e91a3d1f0e1764af14f840172a8042df992337
VillAIgence CI #1005 / 30695911794              SUCCESS
Java Pull Request CI #524 / 30695911797       SUCCESS
Repository security policy #214 / 30695911790 SUCCESS
```

The GREEN gate executes:

```text
:common:test
:fabric:build
:neoforge:build
Fabric distributable-package verification
repository security policy
```

## Automated regression coverage

`VillagerConversionIdentityTest` proves:

1. source data is snapshotted and applied before source discard;
2. an already removed source creates and discards nothing;
3. target creation failure leaves the source registered;
4. target registration occurs only after source removal, preventing registered duplicate UUIDs.

Both loader builds compile the actual Minecraft adapter and mixin signature.

## Deferred cumulative server acceptance

Per operator decision, S2 is not blocked on an isolated server test. Its world-level acceptance is accumulated with S1–S6 and will run once against the complete core synchronization candidate.

Required cumulative S2 segment:

```text
seed one NPC with entries in all six persistent domains
→ record UUID and relevant hashes
→ infect villager
→ verify one live entity with the same UUID
→ restart
→ verify zombie and all six identity links
→ cure zombie
→ verify restored villager with the same UUID
→ restart
→ verify all identity links again
→ run Text/Voice smoke checks
→ verify no duplicate UUID, recovery warning or persistence corruption
```

## Acceptance boundary

```text
repository implementation: PASS
automated lifecycle tests: PASS
Fabric build/package verification: PASS
NeoForge compile compatibility: PASS
repository security policy: PASS
isolated live S2 validation: intentionally deferred
cumulative S1–S6 server validation: PENDING
release promotion based on live conversion evidence: NOT YET CLAIMED
```
