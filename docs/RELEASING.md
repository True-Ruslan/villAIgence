# VillAIgence release runbook

VillAIgence uses GitHub Releases for verified Fabric builds. Internal workflow and script paths still contain `livingworld` for compatibility, while public releases and artifacts use the **VillAIgence** brand.

## Current release line

```text
Minecraft 1.21.1
Java 21
Fabric release package
NeoForge compile compatibility
```

Latest published release:

```text
tag:     0.3.0+1.21.1
commit:  d42141511c0c61f10256fd06576f977f2a784d1c
```

Latest fully installed-accepted release:

```text
tag:     0.2.0+1.21.1
commit:  e426f588efefa6aa48a6e536c4a998421bbda241
```

`0.3.0+1.21.1` is published but its installed acceptance is incomplete because `VAI-PCM-MULTI-001` exposed the crowded-history recall defect corrected by PR #165.

Current release-request candidate:

```text
0.3.1+1.21.1
```

The candidate is defined by:

```text
docs/releases/0.3.1+1.21.1.md
docs/releases/NEXT_RELEASE.txt
```

The immutable `0.3.0` convergence contract remains the baseline capability/persistence boundary for the 0.3 line; `0.3.1` is a narrow corrective release over that boundary and introduces no new persistence schema or migration.

Published tags and assets are immutable. Never overwrite or reuse an existing release tag for different code.

## 0.3.x rollout boundary

`0.3.0` packages the bounded post-`0.2.0` Memory 2.0 capability set plus MCA Personality / directed NPC↔NPC social state through PR #158. `0.3.1` adds only the bounded query-aware Memory 2.0 recall correction from PR #165.

The supported pre-1.0 rollout remains a **clean LivingWorld state on the private test server**. No importer, backfill, dual reader or migration ledger is required for the removed experimental `memory.json` path or unsupported pre-release graph state.

Current world-local state:

```text
memory2.json
semantic-memory.json
events.json
relationships.json
voices.json
operator-lore.json
npc-social-graph.json
```

Current destructive auxiliary recovery matrix:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
npc-social-graph.json
```

`events.json` remains authoritative event history with its own validation path and is deliberately not one of the six auxiliary recovery cases.

Canonical installed evidence source remains:

```text
docs/livingworld/VALIDATION_0.3.0_CLEAN_WORLD_INSTALLED.md
```

For `0.3.1`, the previously failing `VAI-PCM-MULTI-001` scenario must be repeated against the exact official `0.3.1+1.21.1` JAR before it is promoted to installed PASS. Existing unrelated PASS evidence is not silently re-labelled as `0.3.1` evidence.

## Exact release gate

Before GitHub Release publication, `.github/workflows/livingworld-release.yml` must:

1. resolve exactly one requested `<mod_version>+<minecraft_version>` identity;
2. prove the candidate/tag is current `1.21.1` HEAD at publication time;
3. reject an already-consumed tag that points elsewhere;
4. validate the 0.3 capability/persistence boundary plus exact changelog/release-request state;
5. verify embedded Fabric and manifest release identity;
6. run repository security and supply-chain policy;
7. run production-acceptance, lifecycle, persistence-recovery and soak contract tests;
8. stage the exact remapped Fabric candidate with `-Prelease_version=0.3.1+1.21.1`;
9. install it in an isolated production Fabric 1.21.1 server;
10. start, cleanly stop and restart the same world in separate JVMs;
11. require fixture-ready terminal markers and restart-stable canonical stores;
12. execute the current six-case destructive auxiliary-store recovery matrix;
13. run the common suite, required Fabric server GameTests and risk catalog;
14. build Fabric and NeoForge targets;
15. package and smoke-check the public artifact;
16. prove byte-for-byte identity between the production-accepted and packaged JARs;
17. upload JAR, SHA-256 and dependency manifest;
18. create the immutable tag and GitHub Release only after every prior gate passes.

The dedicated Production Soak workflow also runs on the exact release-request head because `docs/releases/NEXT_RELEASE.txt` is part of its PR trigger surface. This gives the release request fresh constrained-concurrency and repeated-restart evidence without adding another workflow.

Release recovery remains version-aware. Recovery of an immutable historical tag uses the release contract from that exact tag commit and must never move the tag.

The workflow does not publish to Maven, Modrinth or CurseForge.

## Release-request flow

The canonical requested version is stored in:

```text
docs/releases/NEXT_RELEASE.txt
```

For this candidate it must contain exactly:

```text
0.3.1+1.21.1
```

Version-specific notes must exist at:

```text
docs/releases/0.3.1+1.21.1.md
```

Pull-request execution is a **non-publishing request-validation dry run**. It embeds the requested version, requires the complete release acceptance matrix and leaves `github-release` skipped.

Required frozen-head PR gates are:

```text
Repository security policy   SUCCESS
VillAIgence CI               SUCCESS
VillAIgence Production Soak  SUCCESS
VillAIgence GitHub Release   SUCCESS / publication SKIPPED
independent review           P0/P1/P2 = 0
unresolved review threads    0
```

Only after those gates pass may the release PR be squash-merged with expected-head protection.

The `1.21.1` push caused by the changed `NEXT_RELEASE.txt` is the separate publication event. The release workflow reruns the complete exact gate on the squash-merge commit; only then may it create the immutable tag, GitHub Release and verified assets.

## Candidate versus installed evidence

Automated release-request evidence is **not** installed acceptance.

The 0.3 policy is intentionally:

```text
request-validation dry run
→ exact-head PR gates
→ squash merge
→ exact main publication gate
→ immutable tag + GitHub Release
→ verify official asset identity
→ install that exact official asset on clean private test server
→ run only unavoidable installed/manual checks
→ record PASS / FAIL / NOT TESTED honestly
```

Do not manually repeat deterministic persistence, retry, codec, corruption-recovery, grave, navigation, fishing, mounted-combat, provenance, contradiction, replay/idempotency or logical-session internals already covered by CI unless an installed symptom specifically points at them.

The canonical acceptance catalog remains:

```text
34 total
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

Explicit deferrals remain:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither may be represented as PASS without the missing real installed evidence.

## Expected `0.3.1` assets

```text
villaigence-fabric-0.3.1+1.21.1.jar
villaigence-fabric-0.3.1+1.21.1.jar.sha256
villaigence-dependencies-0.3.1+1.21.1.txt
```

Record the exact release commit, release workflow/run, asset identifiers and official JAR SHA-256. The server and every graphical client used for installed acceptance must use the same exact official JAR bytes.

## Installation stack

- Minecraft 1.21.1
- Fabric Loader
- Fabric API compatible with Minecraft 1.21.1
- Simple Voice Chat 2.6.20+
- Java 21 on the server
- the same exact VillAIgence JAR on server and clients

VillAIgence intentionally keeps MCA's internal mod ID `mca`.

**Remove original MCA Reborn before installing VillAIgence. Do not install both simultaneously.**

## Compatibility-sensitive names

```text
mod id:          mca
Java namespace:  net.conczin.mca
config:          config/livingworld.json
world data:      <world>/livingworld/
engine classes:  LivingWorld*
```

Renaming them requires a separate migration design and may break worlds, configuration or mod compatibility.

## AI configuration

Credentials remain server-only, for example:

```bash
export OPENAI_API_KEY="..."
export OPENROUTER_API_KEY="..."
```

Alternative server configuration remains `config/livingworld.json`. Never place real keys in Git, release assets, logs or client modpacks.

Installed acceptance should use a known-working provider/model configuration unless provider behavior itself is the subject of the test. Provider/model choice must not redefine persistent NPC identity, memory, relationships, social graph or voice.

## Rollback and failure handling

Before installed acceptance:

1. stop the server;
2. retain the current known-good `0.2.0+1.21.1` JAR and the published `0.3.0+1.21.1` evidence separately;
3. make an offline backup of the test world, `config/` and `mods/`;
4. preserve any previous `<world>/livingworld/` separately;
5. use a clean LivingWorld test state for the 0.3.x acceptance boundary unless the specific scenario requires retained history;
6. record the official `0.3.1+1.21.1` release JAR SHA-256 before installation.

If the publication workflow fails, do not create or move tags/assets manually to bypass the gate. Fix the cause under a development/release PR and rerun the exact candidate while the tag remains unused.

If installed acceptance fails:

1. stop the server;
2. preserve logs and failed LivingWorld evidence;
3. restore the offline test backup or clean test state;
4. reinstall the known-good `0.2.0+1.21.1` release if a fully installed-accepted fallback is required;
5. fix the cause under a narrow development PR;
6. use a new release version if the failing 0.3.x tag has already been published and runtime bytes must change.

Never represent automated logical-client or hardware-independent voice evidence as installed graphical-client or physical-device acceptance.
