# VillAIgence Changelog

> Human-readable implementation and validation history. Read `docs/PROJECT_STATE.md` for exact current state and next priority; read `docs/ROADMAP.md` for long-term direction.

## 2026-08-01 — S10b server-authoritative operator lore API

**Status:** merged to `1.21.1`; automated validation PASS; live-server acceptance pending.

```text
PR: #85
feature head: de2ab0ad38e6b88999cfb397a8dd42ba88c6b1bb
merge: 3cc3137b09629ab348cf0d3e79c821a524259d56
```

Added:

- permission-level-2 READ/WRITE authority;
- trusted server-side scope resolution;
- PLAYER bound to authenticated sender UUID;
- VILLAGER/VILLAGE bound to live nearby same-level MCA villagers;
- home-village dimension and ID derived by the server;
- SHA-256 optimistic revisions;
- explicit `OK`, `UNCHANGED`, `FORBIDDEN`, `CONFLICT`, `INVALID`, `NOT_FOUND`, `ERROR` statuses;
- bounded C2S read/write and canonical S2C response payloads;
- code-point, UTF-8-byte and control-character validation;
- safe network view for transport-oversized pre-existing lore;
- minimal client mailbox for the later UI.

Security boundary:

```text
client-provided player UUID     none
client-provided villager UUID   none
client-provided dimension       none
client-provided village ID      none
client file access              none
client authority decision       none
```

TDD/CI:

```text
canonical RED: f442660ec79f555a6e3f3866435d0b9aa0067238
additional RED: 3d3384e9b3fefb321820dad3094e15f7c98a83dc

VillAIgence CI #1116 / 30705048055              SUCCESS
Java Pull Request CI #623 / 30705048075       SUCCESS
Repository security policy #438 / 30705048038 SUCCESS
```

Evidence:

```text
docs/livingworld/VALIDATION_UPSTREAM_S10B_OPERATOR_LORE_AUTHORITY.md
```

---

## 2026-08-01 — S10a immutable operator lore AI context

**Status:** merged to `1.21.1`; automated validation PASS; live provider behavior pending cumulative acceptance.

```text
PR: #84
feature head: f5ebfdad0b90e86be7b2ea0309dd61bceef39346
merge: b700e14636e3370774173978b0b2519941dafed0
```

Implemented:

- separate immutable `operatorAuthoredContext` snapshot component;
- server-thread loading of world, villager, player and home-village lore;
- no synthetic village scope for villagers without a home village;
- explicit prompt provenance;
- observed current facts before operator lore;
- current observations win conflicts;
- lore inserted before structured JSON/command instructions;
- narrow prompt-return mixin without provider transport/parser/retry changes.

```text
VillAIgence CI #1101 / 30703811853              SUCCESS
Java Pull Request CI #609 / 30703811848       SUCCESS
Repository security policy #407 / 30703811864 SUCCESS
```

Evidence:

```text
docs/livingworld/VALIDATION_UPSTREAM_S10A_OPERATOR_LORE_CONTEXT.md
```

---

## 2026-08-01 — S9 world-local operator lore store

**Status:** merged to `1.21.1`; automated persistence validation PASS.

```text
PR: #83
feature head: e5699bd491fc8deff6719cce0094320b43dcd744
merge: f4a0d369c6e787d9ed91501fc87323aded9b4cbc
file: <world>/livingworld/operator-lore.json
schema: version 1
```

Implemented scopes:

```text
WORLD
VILLAGER by persistent NPC UUID
PLAYER by player UUID
VILLAGE by dimension + stable MCA village ID
```

Persistence guarantees:

- 4096-code-point value limit;
- Unicode-safe truncation in the store boundary;
- line-ending normalization;
- forbidden-control rejection;
- deterministic inspectable JSON;
- exact replay no-op;
- temporary file and atomic replace;
- `.corrupt` backup and fail-open empty-schema recovery;
- explicit provenance formatter;
- no prompt integration, packets, UI or semantic-memory ingestion in S9 itself.

```text
VillAIgence CI #1091 / 30702297647              SUCCESS
Java Pull Request CI #600 / 30702297654       SUCCESS
Repository security policy #387 / 30702297650 SUCCESS
```

Evidence:

```text
docs/livingworld/VALIDATION_UPSTREAM_S9_OPERATOR_LORE.md
```

---

## 2026-08-01 — S7–S8 server behavior and compatibility train

**Status:** merged; automated validation PASS; cumulative live acceptance pending.

### S7 — Graveyard mourning

```text
PR #79
merge: 8dccf686f5f8ab245979e7ab64e1d75b87d14623
```

- persistent mourning site and position memories;
- safe adjacent standing positions;
- reservation-aware multi-NPC distribution;
- flower/look/dialogue grieving lifecycle;
- 1200-tick retry;
- grave-removal completion and resurrection cleanup.

### S8a — Relationship gifts

```text
PR #80
merge: 0d833659626f1a91d6e86c7e0cae0b2a7689b755
```

Replaced the ambiguous special-gift boolean with `InteractionResult` semantics:

```text
PASS    ordinary gift processing may continue; consume 0
FAIL    handled rejection; consume 0
CONSUME handled success; consume exactly 1
```

### S8b — Fishing and AquaCulture compatibility

```text
PR #81
merge: 75c2c0c77508571c010c0c5838c8a8b62ab6afaf
```

- actual held rod used in loot context;
- loot generated at catch time;
- target-water center used as origin;
- safe cod fallback for empty loot;
- dominant-hand stack and slot damaged correctly;
- compatible modded `FishingRodItem` subclasses retained.

### S8c — Mounted archer crash fix

```text
PR #82
merge: 15ee14321f1b8d846ddb2df7c24c5159a53e6938
```

- captures the original stable `ArcherMoveControl`;
- vehicle replacement of active move control no longer breaks archer behavior;
- implemented through narrow owner interface and mixins;
- full archer state machine left intact.

Evidence:

```text
docs/livingworld/VALIDATION_UPSTREAM_S7_MOURNING.md
docs/livingworld/VALIDATION_UPSTREAM_S8A_INTERACTION_RESULT.md
docs/livingworld/VALIDATION_UPSTREAM_S8B_FISHING.md
docs/livingworld/VALIDATION_UPSTREAM_S8C_ARCHER_MOUNTED.md
```

---

## 2026-08-01 — S1–S6 MCA core synchronization checkpoint

**Status:** implementation complete and automated PASS; exact installed-server acceptance pending.

```text
S1 PR #72 tombstone item/entity-data integrity
S2 PR #73 UUID-preserving villager↔zombie conversion
S3 PR #74 occupied HOME-bed rejection and ticket correctness
S4 PR #75 water-tag and entity-AABB collision navigation
S5 PR #76 stable climbable/ladder navigation
S6 PR #77 staggered pathfinding plus retained progress watchdog
checkpoint PR #78
```

Key outcomes:

- filled tombstones retain NPC NBT across break/place;
- infection/cure conversion preserves UUID identity without duplicate registered entities;
- occupied beds are rejected as HOME targets;
- water navigation recognizes tagged/modded water;
- collision checks use the NPC's actual translated bounding box;
- climbable path graph and motion handle ascent, descent and exits;
- expensive pathfinding starts are deterministically staggered per NPC;
- the existing progress watchdog and recovery behavior remain active.

Protected boundaries retained:

```text
OpenAIChatAI transport/parser/security
LivingWorld persistent schemas
security workflows and scripts
verified dependencies and lockfiles
```

Evidence begins at:

```text
docs/upstream/README.md
docs/livingworld/VALIDATION_UPSTREAM_S1_TOMBSTONE.md
docs/livingworld/VALIDATION_UPSTREAM_S6_PATH_SCHEDULING.md
```

---

## 2026-08-01 — 0.1.17 SEC-004 closure and Step 1 completion

**Status:** exact official release JAR accepted; all Step 1 findings Closed.

```text
tag: 0.1.17+1.21.1
commit: 88a20d86e8b08e4b5eaf60da943a63e750f2b545
SHA-256: b33af40f7a2696dc679c49e0fc544f6b5df99e0aa600ea5c767bc5a9747da1ab
marker: V0117_SEC004_ARTIFACT_AND_EVIDENCE_PASS
```

Focused exact-JAR results:

```text
HTTP success                         SUCCESS / 200
HTTP redirect                        HTTP_ERROR / 307
redirect followed                    no
redirect_target_hits                 0
declared/chunked oversize            TOO_LARGE
HTTPS loopback                       rejected before connection
SSLException                         none
```

Final matrix:

```text
SEC-001 Closed
SEC-002 Closed
SEC-003 Closed
SEC-004 Closed
SEC-005 Closed
SEC-006 Closed
SEC-007 Closed
SEC-008 Closed
SEC-009 Closed
```

`0.1.17` was exact-artifact validated, not installed as the production Minecraft checkpoint.

---

## 2026-08-01 — 0.1.16 full controlled server acceptance

```text
commit: 521568f903078b91dd5817cdc9a551bd2392e663
SHA-256: 036cbacc657ceb676813f41ee293024690b981e971e7c6037fc5d3ecbe3ee062
```

Live-proven:

- Chat/STT/TTS/error response limits;
- authenticated redirect rejection;
- ten-minute slow-drip deadline;
- TTS fail-soft text/DIALOGUE preservation;
- voice duration clamp;
- exact 128 MiB PCM budget and recovery;
- production configuration restoration;
- six-file restart durability;
- server, Voice Chat, ports and monitor health.

This remains the latest installed-server checkpoint until a newer synchronized candidate passes live acceptance.

---

## 2026-07-31 — 0.1.15 production and endpoint-policy checkpoint

```text
commit: 26070c37b806897e37cc3dabe2e4b27af458ac20
SHA-256: af142be94885541bb4840d0effff73627afe3f0e245dec8307ed665701cc94fb
```

Validated production Text/STT/TTS, NPC isolation, memory recall, TTS fail-soft behavior, controlled 429 handling, endpoint rejection, configuration restoration and persistent-file restart hashes.

---

## 2026-07-31 — 0.1.14 deterministic semantic forgetting/decay

```text
commit: c45aea45dd915b24ba236344feef30559c7171bb
marker: V0114_FINAL_RESTART_VERIFICATION_PASS
```

Live-proven:

- older corroborated FACT survival under pressure;
- source-event durability;
- deterministic decay ordering;
- weak existing FACT eviction;
- per-NPC retention isolation;
- semantic UUID/source preservation;
- restart-safe persistence.

Rejected-new-append no-rewrite remains automated-proven but was not reached through the live randomized model path.

---

## Memory 2.0 implementation train

Material milestones retained:

```text
PRs #31–#43  episodic MemoryEvent model, persistence, retrieval and ingestion
PR #46       Working Memory and Semantic Memory foundation
PR #49       controlled semantic FACT ingestion
PR #53       deterministic semantic consolidation and source union
PR #56       deterministic pressure-based forgetting/decay
```

Current remaining Memory 2.0 work is intentionally sequenced after the S10c/release gate:

- additive legacy `memory.json` migration;
- controlled BELIEF producers;
- relationship reasons;
- long-horizon and multiplayer soak;
- NPC-to-NPC knowledge and rumors.

---

# Current next step

```text
S10c operator lore client editor UI
→ synchronized release candidate
→ cumulative installed-server acceptance S1–S10c
→ release promotion only on PASS
→ resume additive legacy memory.json migration
```

No S1–S10b live-validation or new-release claim is implied by the successful automated package gates.
