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
tag:     0.1.25+1.21.1
commit:  588cc676d356271c4cf74eb21131f6d071476e48
```

The next candidate request is:

```text
0.1.26+1.21.1
```

Published tags and assets are immutable. Never overwrite or reuse an existing release tag for different code.

## Exact release gate

Before GitHub Release publication, `.github/workflows/livingworld-release.yml` must:

1. validate `<mod_version>+<minecraft_version>`;
2. prove the candidate is current `1.21.1` HEAD;
3. reject an already-consumed version;
4. verify embedded Fabric and manifest release identity;
5. run repository security and supply-chain policy;
6. run production-acceptance, lifecycle, persistence-recovery and soak contract tests;
7. stage the exact remapped Fabric candidate with the requested release version;
8. install it in an isolated production Fabric 1.21.1 server;
9. start, cleanly stop and restart the same world in separate JVMs;
10. require fixture-ready terminal markers and valid restart-stable canonical stores;
11. verify real death, portable grave, resurrection, UUID, name and inventory lifecycle evidence;
12. execute the six-case destructive auxiliary-store recovery matrix;
13. run the acceptance catalog, common tests and sixteen required server GameTests;
14. build Fabric and NeoForge targets;
15. package and smoke-check the public artifact;
16. prove byte-for-byte identity between the production-accepted and packaged JARs;
17. upload JAR, SHA-256 and dependency manifest;
18. create the immutable tag and GitHub Release only after every prior gate passes.

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
docs/releases/0.1.26+1.21.1.md
```

Pull-request execution is a **non-publishing dry run**. It validates that the requested tag is unused, embeds the requested version and completes the full production, recovery, GameTest, loader-build, package and exact-JAR identity gates.

The release PR must remain unmerged until the exact dry-run artifact passes the focused installed canaries. After the release PR is squash-merged, the `1.21.1` push caused by the changed `NEXT_RELEASE.txt` executes the same gate again on the exact merge commit. Only after PASS does the workflow create the tag, GitHub Release and verified assets.

The tag and release steps are idempotent only when an existing tag resolves to the same exact commit. A tag pointing elsewhere fails closed.

## Installed candidate canaries

Use the exact JAR produced by the release-request dry run. Do not substitute a local or snapshot build.

Before testing:

1. back up the production world and `<world>/livingworld/`;
2. record the candidate JAR SHA-256;
3. install the same candidate JAR on server and graphical clients;
4. remove original MCA Reborn; do not install both mods simultaneously.

Run only the six catalog boundaries that require installed, graphical, physical or subjective evidence:

1. **Startup:** exact candidate reaches full server startup and a client connects without forbidden Mixin/refmap errors.
2. **Ordinary MCA water escape:** two normal MCA NPC brains visibly escape separate reachable water lanes and remain alive/mobile on land.
3. **Visible selected-NPC text:** one installed client addresses the selected NPC and renders one response.
4. **Real Silk Touch grave interaction:** known inventory → death/tombstone → Silk Touch pickup → placement → restart → resurrection preserves one UUID, name and exact stack multiset without duplicate loose drops.
5. **Physical voice:** one microphone turn traverses OS permission, client capture, UDP/Opus and produces one audible spatial NPC response without duplicate playback.
6. **Two-client Operator Lore:** both clients read one revision; first save succeeds; stale second save visibly shows conflict and preserves the draft; reviewed retry with the current revision succeeds exactly once.

Do not manually repeat deterministic persistence, retry, codec, recovery or logical-session internals already covered by CI.

## Expected `0.1.26` assets

```text
villaigence-fabric-0.1.26+1.21.1.jar
villaigence-fabric-0.1.26+1.21.1.jar.sha256
villaigence-dependencies-0.1.26+1.21.1.txt
```

Record the exact candidate/release commit, workflow run, artifact IDs and official JAR SHA-256 in version-specific validation evidence. Do not infer an official release hash from a PR artifact.

## Manual tag fallback

The normal path is the release-request flow. Manual tagging remains an emergency fallback and must point to current `1.21.1` HEAD:

```bash
git checkout 1.21.1
git pull --ff-only origin 1.21.1

git tag -a "0.1.26+1.21.1" \
  -m "VillAIgence 0.1.26 for Minecraft 1.21.1"

git push origin "0.1.26+1.21.1"
```

The tag-triggered workflow still executes the complete exact-production gate and rejects a stale commit.

## Installation stack

- Minecraft 1.21.1
- Fabric Loader
- Fabric API compatible with Minecraft 1.21.1
- Simple Voice Chat 2.6.20+
- Java 21 on the server
- the same VillAIgence JAR on server and clients

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

Credentials remain server-only:

```bash
export OPENAI_API_KEY="..."
export OPENROUTER_API_KEY="sk-or-v1-..."
```

Alternative server configuration remains `config/livingworld.json`. Never place real keys in Git, release assets, logs or client modpacks.

## Failure handling

Do not create assets manually to bypass a failed workflow. Preserve failed evidence, fix the cause and rerun the same candidate while its tag remains unused. If a tag has been published or may have been consumed, use a new sequential version.

Never merge the release request when an installed canary is untested or failed. Never represent automated logical-client or hardware-independent voice evidence as installed graphical-client or physical-device acceptance.
