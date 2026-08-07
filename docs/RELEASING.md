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
tag:     0.1.26+1.21.1
commit:  40ce7cb77e9b9178fd96fd91025cee22ba686dc0
```

Current release-request candidate:

```text
0.2.0+1.21.1
```

Published tags and assets are immutable. Never overwrite or reuse an existing release tag for different code.

## 0.2.0 rollout boundary

`0.2.0` begins the Memory 2.0 release line. It intentionally does **not** migrate the experimental pre-0.2 `<world>/livingworld/memory.json` raw conversation store.

Supported installed validation uses a clean LivingWorld state on a dedicated test world or offline world copy. The old file is not imported, read or included in the current recovery matrix.

Canonical installed plan:

```text
docs/livingworld/VALIDATION_0.2.0_CLEAN_WORLD_INSTALLED.md
```

## Exact release gate

Before GitHub Release publication, `.github/workflows/livingworld-release.yml` must:

1. validate `<mod_version>+<minecraft_version>`;
2. prove the candidate is current `1.21.1` HEAD at publication time;
3. reject an already-consumed version;
4. verify embedded Fabric and manifest release identity;
5. run repository security and supply-chain policy;
6. run production-acceptance, lifecycle, persistence-recovery and soak contract tests;
7. stage the exact remapped Fabric candidate with the requested release version;
8. install it in an isolated production Fabric 1.21.1 server;
9. start, cleanly stop and restart the same world in separate JVMs;
10. require fixture-ready terminal markers and valid restart-stable canonical stores;
11. execute the current **five-case** destructive auxiliary-store recovery matrix;
12. run the acceptance catalog, common tests and required server GameTests;
13. build Fabric and NeoForge targets;
14. package and smoke-check the public artifact;
15. prove byte-for-byte identity between the production-accepted and packaged JARs;
16. upload JAR, SHA-256 and dependency manifest;
17. create the immutable tag and GitHub Release only after every prior gate passes.

Current auxiliary recovery matrix:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`events.json` is authoritative factual event history with its own validation path and is not one of the five auxiliary corruption cases.

Release recovery is version-aware. When rebuilding an existing immutable historical tag, the controller checks out that release commit and validates the non-empty all-PASS matrix defined by that commit. It must not impose the current five-store count on older releases such as `0.1.26`, whose immutable acceptance boundary contained six stores.

The workflow does not publish to Maven, Modrinth or CurseForge.

## Release-request flow

The canonical requested version is stored in:

```text
docs/releases/NEXT_RELEASE.txt
```

A release PR must also provide version-specific notes at:

```text
docs/releases/<tag>.md
```

For this candidate:

```text
docs/releases/0.2.0+1.21.1.md
```

Pull-request execution is a **non-publishing dry run**. It validates that the requested tag is unused, embeds the requested version and completes the full production, recovery, GameTest, loader-build, package and exact-JAR identity gates.

The release PR must remain unmerged until the exact dry-run artifact passes the focused installed acceptance described in `docs/livingworld/VALIDATION_0.2.0_CLEAN_WORLD_INSTALLED.md`.

After the release PR is squash-merged, the `1.21.1` push caused by the changed `NEXT_RELEASE.txt` executes the same gate again on the exact merge commit. Only after PASS may the workflow create the tag, GitHub Release and verified assets.

The tag and release steps are idempotent only when an existing tag resolves to the same exact commit. A tag pointing elsewhere fails closed.

## Candidate identity

Use the exact JAR produced by the release-request dry run. Do not substitute a local or snapshot build.

Before installed testing record:

```text
release-request PR
exact PR head
release dry-run workflow/run
artifact id
candidate JAR filename
candidate JAR SHA-256
```

The SHA supplied to the operator must match the JAR installed on both server and graphical client.

## Installed acceptance policy

Do not manually repeat deterministic persistence, retry, codec, recovery, grave, navigation, fishing, mounted-combat or logical-session internals already covered by CI unless an installed symptom specifically requires them.

For `0.2.0`, installed acceptance focuses on the new Memory 2.0 boundary:

1. exact candidate clean startup and real client connection;
2. text dialogue persistence and immediate recall;
3. exact NPC isolation;
4. physical microphone/voice turn entering the same DIALOGUE model;
5. same-world restart recall;
6. `memory2.json` exists and removed `memory.json` is not recreated.

`VAI-CONCUR-004` remains `NOT TESTED / DEFERRED` until two real graphical clients are available. Automated authenticated two-session acceptance covers the server authority/revision model but is not graphical evidence.

## Expected `0.2.0` assets

```text
villaigence-fabric-0.2.0+1.21.1.jar
villaigence-fabric-0.2.0+1.21.1.jar.sha256
villaigence-dependencies-0.2.0+1.21.1.txt
```

Record the exact candidate/release commit, workflow run, artifact IDs and official JAR SHA-256 in version-specific validation evidence. Do not infer an official release hash from a different build.

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

Unless provider behavior itself is under test, installed acceptance should retain the known-good provider/model configuration already accepted on the test server. `0.1.26` passed Chat with `google/gemini-2.5-flash-lite`.

## Rollback and failure handling

Before installed testing:

1. stop the server;
2. make an offline backup of the test world, `config/` and `mods/`;
3. preserve any old `<world>/livingworld/` separately;
4. create/choose the clean LivingWorld test state;
5. record the current known-good version and candidate SHA-256.

If any required installed case fails:

1. stop the server;
2. preserve logs and failed LivingWorld evidence;
3. do not merge the release request and do not create the tag;
4. restore the offline test backup or known-good `0.1.26+1.21.1` test setup;
5. fix the cause under a development PR and rerun a fresh exact candidate.

Do not create assets manually to bypass a failed workflow. Do not merge the release request when a required installed canary is failed or missing. Never represent automated logical-client or hardware-independent voice evidence as installed graphical-client or physical-device acceptance.
