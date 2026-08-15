# VillAIgence 0.3.2 Corrective Installed Test Plan

Status: **READY FOR OPERATOR EXECUTION**

This plan validates only the installed boundary that remained blocked after official `0.3.1+1.21.1` validation. It deliberately does not reopen already accepted voice, STT, security, restart, navigation or gameplay canaries.

## Release under test

```text
release:      0.3.2+1.21.1
artifact:     villaigence-fabric-0.3.2+1.21.1.jar
release id:   371021968
asset id:     515590903
SHA-256:      b51cfcf3f46718fac9620586cf8b5aae53356c600d5ac375ca3280050befe015
release run:  VillAIgence GitHub Release #897 / run 31879227075 / SUCCESS
source merge: 3bb39e7ed126163efcdf971e85c89a4a5efd3111
runtime fix:  PR #169 / merge 101c74d178ec29ca15f67ebd6041ef256a339f31
```

The release is a narrow correction over immutable `0.3.1+1.21.1`. It changes Memory 2.0 query-aware ranking and explicit marker/code/token recall only. It adds no persistence schema/version, migration, provider call, public configuration, memory-window widening, FACT/BELIEF authority change or NPC/player isolation weakening.

## Why this canary is required

Installed `0.3.1+1.21.1` proved:

```text
Official SHA            PASS
Startup gate            PASS
Muammer recall          FAIL
Nurey recall            PASS
Cross-NPC isolation     PASS
Duplicate check         PASS
Persistence validity    PASS
VAI-PCM-MULTI-001       FAIL
LinuxGSM monitor        OK
```

The retained source event for Muammer still existed under the correct NPC UUID and all 14 stored dialogue event IDs were unique. The unresolved boundary was therefore retrieval/ranking rather than persistence, UUID ownership or cross-NPC isolation.

`0.4` remains blocked until this exact official `0.3.2` artifact passes the installed corrective canary on the retained world.

## Preserved installed baseline

Use the existing world state. Do not reset, regenerate, migrate or manually rewrite the marker events before testing.

```text
Muammer UUID: 34e2a220-7e85-4edc-8c93-52b068b97608
Muammer marker: amber-pine-314
Muammer source event: 3252f67f-27f5-38bf-840c-d522c36b34fd

Nurey UUID: 4adf884f-81ad-4a88-bdf9-46eda1ce237a
Nurey marker: violet-river-926
```

The pre-`0.3.2` backup must be created before replacing the active JAR. Preserve at minimum:

```text
world/
config/
mods/
current LivingWorld JSON files
active 0.3.1 JAR
pre-install persistence hashes
crontab / LinuxGSM evidence used for recovery
```

Recommended backup name:

```text
/home/server/minecraft/backups/pre-villaigence-0.3.2-<YYYYMMDD-HHMMSS>
```

## Phase A — official artifact identity

Download only the immutable GitHub Release asset and verify before upload/install.

Expected SHA-256:

```text
b51cfcf3f46718fac9620586cf8b5aae53356c600d5ac375ca3280050befe015  villaigence-fabric-0.3.2+1.21.1.jar
```

Reject the candidate if any of these differ:

- filename;
- SHA-256;
- embedded Fabric metadata version;
- installed server JAR SHA after deployment.

Expected embedded metadata:

```text
id:      mca
name:    VillAIgence
version: 0.3.2+1.21.1
```

## Phase B — pre-install persistence evidence

Before shutdown, capture hashes of every existing file under the active LivingWorld data root, especially:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
npc-social-graph.json
```

The corrective patch has no persistence write/migration requirement merely from installation. Existing hashes should therefore remain unchanged across the installation restart before any new canary dialogue is generated.

Also verify before replacing the JAR:

- Muammer source event `3252f67f-27f5-38bf-840c-d522c36b34fd` is still present under Muammer's UUID;
- `amber-pine-314` is still present in that retained event;
- Nurey's retained `violet-river-926` baseline still exists under Nurey's UUID;
- `memory2.json` parses as valid JSON;
- no duplicate event IDs exist in the persisted dialogue set.

If the source marker has disappeared before installation, stop: the test would no longer exercise the same corrective boundary.

## Phase C — install and startup gate

1. Stop the server cleanly.
2. Create the recovery backup.
3. Replace only the VillAIgence JAR with the verified official `0.3.2+1.21.1` asset.
4. Keep the retained world and current configuration unchanged.
5. Start the server normally through LinuxGSM.
6. Verify the active JAR SHA against the official SHA.
7. Verify post-restart LivingWorld hashes before generating any new dialogue.

Startup gate requires:

- Minecraft `1.21.1`;
- Java `21`;
- Fabric Loader and Fabric API compatible with the existing accepted setup;
- Simple Voice Chat compatible with the existing accepted setup;
- VillAIgence internal mod ID `mca`, version `0.3.2+1.21.1`;
- server reaches `Done`;
- Minecraft TCP/UDP `25565` active;
- Simple Voice Chat UDP `24454` active;
- LinuxGSM monitor returns `OK`;
- no blocking VillAIgence, Mixin, refmap, injection, fatal or crash error.

A non-blocking external Mojang-network warning may be recorded separately if startup, login, provider dialogue and health checks remain functional; do not silently classify a new VillAIgence failure as external noise.

## Phase D — corrective VAI-PCM-MULTI-001 canary

Do not teach either marker again before recall. The purpose is to prove retrieval of the already-preserved source events.

### D1 — Muammer exact recall

Send exactly:

```text
Муаммер, назови личный маркер, который я ранее сообщил именно тебе.
```

Required PASS:

- the response contains `amber-pine-314`, allowing only trivial case/punctuation formatting differences that preserve the exact marker value;
- the response does not substitute `violet-river-926`;
- the response is persisted under Muammer's UUID;
- the new request/response event receives a unique event ID;
- no duplicate provider reply or duplicate persisted event is produced.

Any answer equivalent to “не помню”, an invented marker, the other NPC's marker or an unrelated old fact is FAIL.

### D2 — Nurey exact recall

Send exactly:

```text
Нурэй, назови личный маркер, который я ранее сообщил именно тебе.
```

Required PASS:

- the response contains `violet-river-926`;
- the response does not contain `amber-pine-314`;
- persistence owner UUID is Nurey's UUID;
- event ID is unique;
- no duplicate response/event is observed.

### D3 — isolation and integrity

After both turns verify:

- Muammer recalled only `amber-pine-314`;
- Nurey recalled only `violet-river-926`;
- no cross-NPC leakage occurred;
- `memory2.json` remains valid JSON;
- all persisted event IDs are unique;
- original source event `3252f67f-27f5-38bf-840c-d522c36b34fd` remains present and unmodified;
- provider/admission counters return to zero after completion;
- LinuxGSM monitor remains `OK`.

Record the exact new request and response event IDs for both NPCs.

## Acceptance matrix

```text
Official release SHA         PASS required
Embedded version             PASS required
Active installed JAR SHA     PASS required
Startup gate                 PASS required
Pre-dialogue persistence     unchanged required
Muammer retained source      present required
Muammer exact recall         PASS required
Nurey exact recall           PASS required
Cross-NPC isolation          PASS required
Duplicate event check        PASS required
memory2.json validity        PASS required
LinuxGSM monitor             OK required

VAI-PCM-MULTI-001            PASS only if every required row above passes
```

Existing unrelated deferrals remain exactly:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Do not convert either to PASS by inference.

## Evidence to retain on PASS or FAIL

Always retain enough evidence for deterministic follow-up:

```text
official release URL/tag
release asset SHA-256
active server JAR SHA-256
server startup log excerpt
pre/post-install LivingWorld hashes
pre/post-canary memory2.json snapshot or safe backup
exact Muammer/Nurey prompts
exact persisted NPC responses
source and new event IDs
NPC owner UUIDs for all relevant events
unique-event-ID count
LinuxGSM monitor result
backup path
```

If recall fails, additionally preserve the exact original source-event JSON object and the final bounded Memory 2.0 context/ranking diagnostics if available. Do not rewrite or reseed the marker before collecting failure evidence.

## Disposition after execution

### If PASS

```text
0.3.2 installed corrective acceptance → PASS
VAI-PCM-MULTI-001                  → PASS
0.3 delivery boundary             → eligible for reconciliation
0.4 Knowledge ecosystem           → may be unblocked only after repository state/roadmap reconciliation
```

The next repository change should update `docs/PROJECT_STATE.md`, `docs/ROADMAP.md` and any installed-validation ledger with the exact installed artifact SHA and evidence. Only then begin the first 0.4 slice.

### If FAIL

Keep `0.3.2+1.21.1` operational only if normal server health remains acceptable, but leave `0.4` blocked. Preserve the exact evidence above and return to root-cause analysis before creating another runtime correction. Do not broaden the memory window, add provider-assisted retrieval or modify persistence as an unproven workaround.
