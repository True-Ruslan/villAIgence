# VillAIgence release runbook

VillAIgence uses GitHub Releases for verified Fabric builds. Internal workflow and script paths still contain `livingworld` for compatibility, while public releases and artifacts use the **VillAIgence** brand.

## Current release line

```text
Minecraft 1.21.1
Java 21
Fabric release package
NeoForge compile compatibility
```

Published tag `0.1.24+1.21.1` is immutable and points to:

```text
42d8cb3408c53770abe63ced130727c805bc9e8a
```

The next release is:

```text
0.1.25+1.21.1
```

It contains the same accepted tombstone-inventory runtime correction plus the exact-production release gate merged afterward in PR #106. Existing tags and assets must never be overwritten or reused for different code.

## Exact release gate

Before GitHub Release publication, `.github/workflows/livingworld-release.yml` must:

1. validate `<mod_version>+<minecraft_version>`;
2. prove the candidate is current `1.21.1` HEAD;
3. reject an already-consumed version;
4. verify embedded Fabric and manifest release identity;
5. run repository security policy;
6. run production-acceptance harness contract tests;
7. stage the exact remapped Fabric candidate with the release version;
8. install it in an isolated production Fabric 1.21.1 server;
9. start, cleanly stop and restart the same world in two JVMs;
10. require fixture markers and valid restart-stable canonical stores;
11. run the risk catalog, common tests and real server GameTests;
12. build Fabric and NeoForge targets;
13. package and smoke-check the public artifact;
14. prove byte-for-byte identity between the production-accepted and published JARs;
15. upload JAR, SHA-256 and dependency manifest;
16. create the immutable tag and GitHub Release only after every prior gate passes.

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

For `0.1.25+1.21.1`:

```text
docs/releases/0.1.25+1.21.1.md
```

Pull-request execution is a non-publishing dry run. It validates that the requested tag is unused, builds with the requested embedded version and completes the full production, GameTest, loader-build, package and exact-JAR identity gates.

After the release PR is squash-merged, the `1.21.1` push caused by the changed `NEXT_RELEASE.txt` executes the same gate again on the exact merge commit. Only after PASS does the workflow:

1. create the annotated tag on that exact commit;
2. create the GitHub Release;
3. upload the verified JAR, checksum and dependency manifest.

The tag and release steps are idempotent only when an existing tag resolves to the same exact commit. A tag pointing elsewhere fails closed.

## Manual tag fallback

The normal path is the release-request flow. Manual tagging remains an emergency fallback:

```bash
git checkout 1.21.1
git pull --ff-only origin 1.21.1

git tag -a "0.1.25+1.21.1" \
  -m "VillAIgence 0.1.25 for Minecraft 1.21.1"

git push origin "0.1.25+1.21.1"
```

The tag-triggered workflow rejects a stale commit and still executes the complete exact-production gate.

## Expected `0.1.25` assets

```text
villaigence-fabric-0.1.25+1.21.1.jar
villaigence-fabric-0.1.25+1.21.1.jar.sha256
villaigence-dependencies-0.1.25+1.21.1.txt
```

Record the exact release commit, workflow run and official JAR SHA-256 in:

```text
docs/livingworld/VALIDATION_0.1.25_RELEASE_GATE.md
```

Do not infer the official asset hash from a PR or dry-run artifact.

## Installed `0.1.25` canary

The exact official release JAR requires the focused manual inventory/grave canary:

1. back up the production world and persistent stores;
2. install the exact official JAR on server and client;
3. verify startup, Voice Chat and Text/STT/Chat/TTS smoke;
4. give one MCA villager known custom inventory stacks;
5. kill the villager next to a valid empty tombstone;
6. verify no loose duplicate stacks are emitted;
7. break and place the filled grave through Silk Touch;
8. restart the server;
9. resurrect the NPC;
10. verify UUID, name and every known stack count exactly once;
11. verify the no-tombstone fallback still drops stacks exactly once;
12. verify canonical persistent data remains valid and restart-safe.

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

Do not create assets manually to bypass a failed workflow. Preserve the failed evidence, fix the cause and use a new sequential version whenever a tag has been published or may have been consumed.

Only delete a mistaken tag when no valid release exists and it has never become a published version. Never reuse a published release tag for different code.
