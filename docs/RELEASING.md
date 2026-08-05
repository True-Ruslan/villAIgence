# VillAIgence release runbook

VillAIgence uses GitHub Releases for verified Fabric builds. The internal workflow/script paths still contain `livingworld` for compatibility and to avoid unnecessary churn, but all new public releases and artifacts use the **VillAIgence** brand.

## Current release line

Current target:

```text
Minecraft 1.21.1
Java 21
Fabric release package
NeoForge compile compatibility
```

The next candidate after the merged tombstone-inventory fix is:

```text
0.1.24+1.21.1
```

Existing tags and release assets are immutable. Never overwrite or reuse a published tag for different code.

## Exact release gate

When a valid release tag is pushed, `.github/workflows/livingworld-release.yml` must complete the full gate before GitHub Release publication:

1. validate `<mod_version>+<minecraft_version>` tag format;
2. prove the tag points exactly to current `1.21.1` HEAD;
3. prove exactly one matching tag points at the candidate commit;
4. verify embedded Fabric and manifest release identity;
5. run repository security policy;
6. run production-acceptance harness contract tests;
7. stage the exact remapped Fabric candidate with the release version;
8. install it in an isolated production Fabric 1.21.1 server;
9. start, cleanly stop and restart the same world in two separate JVM processes;
10. require the production fixture marker on both runs;
11. require valid and restart-stable canonical persistent stores;
12. run the acceptance risk catalog and common tests;
13. run real Fabric server GameTests;
14. build Fabric and NeoForge targets;
15. package and smoke-check the public Fabric artifact;
16. prove the production-accepted JAR is byte-for-byte identical to the packaged JAR;
17. upload the JAR, SHA-256 and dependency manifest;
18. create the matching GitHub Release only after every prior step passes.

The workflow does not publish to Maven, Modrinth or CurseForge.

## Dry-run release gate

Before creating a release tag:

1. Open GitHub → Actions.
2. Select **VillAIgence GitHub Release**.
3. Click **Run workflow**.
4. Select branch `1.21.1`.
5. Run it manually.

A manual run performs the same security, production startup/restart, persistence, GameTest, loader-build, package and exact-JAR identity gates. It uploads evidence and the dry-run Fabric package, but does **not** create a tag or GitHub Release.

Expected Actions artifacts:

```text
production-server-acceptance-<run-number>
villaigence-fabric-package
```

## Preconditions before tagging

- All intended changes are merged into `1.21.1`.
- Required PR checks are green.
- A manual **VillAIgence GitHub Release** dry-run on `1.21.1` succeeds.
- `1.21.1` contains exactly the code intended for release.
- The chosen version tag does not already exist.
- The tag is created from current `1.21.1`, never from a feature branch.
- A backed-up installed-server canary plan exists for behavior that CI cannot fully prove.

The workflow intentionally rejects stale tags that do not point to current `1.21.1` HEAD.

## Create the `0.1.24` release tag

```bash
git checkout 1.21.1
git pull --ff-only origin 1.21.1

git tag -a "0.1.24+1.21.1" \
  -m "VillAIgence 0.1.24 for Minecraft 1.21.1"

git push origin "0.1.24+1.21.1"
```

After the tag push, open GitHub → Actions → **VillAIgence GitHub Release** and require both jobs to succeed.

Expected release assets:

```text
villaigence-fabric-0.1.24+1.21.1.jar
villaigence-fabric-0.1.24+1.21.1.jar.sha256
villaigence-dependencies-0.1.24+1.21.1.txt
```

Record the exact tag commit and JAR SHA-256 in the matching validation document. Do not infer the official asset hash from a PR or dry-run artifact.

## Installed `0.1.24` canary

The exact official release JAR still requires the focused manual inventory/grave canary documented in:

```text
docs/livingworld/VALIDATION_0.1.24_RELEASE_GATE.md
```

Minimum manual boundary:

1. back up the production world and persistent stores;
2. install the exact official `0.1.24+1.21.1` JAR on server and client;
3. verify startup, Voice Chat connection and normal Text/STT/Chat/TTS smoke;
4. give one MCA villager known custom inventory stacks;
5. kill the villager next to a valid empty tombstone;
6. verify no loose duplicate stacks are emitted;
7. break and place the filled grave through Silk Touch;
8. restart the server;
9. resurrect the NPC;
10. verify UUID, name and all known stack counts exactly once;
11. compare canonical persistent-file validity and restart hashes;
12. record PASS/FAIL evidence without converting untested scenarios into claims.

## Test installation stack

Required stack:

- Minecraft 1.21.1
- Fabric Loader
- Fabric API compatible with Minecraft 1.21.1
- Simple Voice Chat 2.6.20+
- Java 21 on the server
- the same VillAIgence release JAR on server and clients

VillAIgence intentionally keeps MCA's internal mod id `mca`.

**Remove original MCA Reborn before installing VillAIgence. Do not install both simultaneously.**

## Compatibility-sensitive names

The public rebrand does not rename these internals:

```text
mod id:          mca
Java namespace:  net.conczin.mca
config:          config/livingworld.json
world data:      <world>/livingworld/
engine classes:  LivingWorld*
```

Renaming them requires a separate migration design and may break worlds, configs or mod compatibility.

## AI configuration

Credentials are server-only.

OpenAI:

```bash
export OPENAI_API_KEY="..."
```

OpenRouter:

```bash
export OPENROUTER_API_KEY="sk-or-v1-..."
```

Alternative server configuration remains `config/livingworld.json`.

Never place real API keys in Git, release assets, logs or client modpacks.

## General release smoke

Before using an important world, back it up and verify:

1. server starts without loader, dependency, Mixin or refmap errors;
2. MCA villagers spawn and normal MCA interactions work;
3. AI text conversation works;
4. Simple Voice Chat connects and normal player voice works;
5. microphone → STT → selected NPC → clean text works;
6. when TTS is enabled, the clean answer plays spatially from the NPC;
7. persisted NPC voices remain stable across restart;
8. canonical world-local JSON stores remain valid after restart;
9. safe NPC actions remain whitelist/server-authority constrained;
10. forced TTS failure leaves the text reply and DIALOGUE state intact.

## Failed release or bad tag

Do not create assets manually to bypass a failed workflow. Fix the cause and use a new version tag when the previous tag may already have been consumed.

Only delete a mistaken tag when no valid release should exist and it has not become a published version:

```bash
git push origin --delete "<tag>"
git tag -d "<tag>"
```

Never reuse a published release tag for different code.
