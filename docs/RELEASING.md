# VillAIgence release runbook

VillAIgence uses GitHub Releases for verified Fabric builds. The internal workflow/script paths still contain `livingworld` for compatibility and to avoid unnecessary churn, but all new public releases and artifacts use the **VillAIgence** brand.

## Current release line

Current Minecraft target:

```text
Minecraft 1.21.1
Java 21
Fabric
```

The next release after the rebrand is expected to use the next free sequential tag (currently `0.1.5+1.21.1` unless a newer tag is created first).

Existing tags and release assets are immutable. Do not rename or move already published versions just to apply the new brand.

## What CI does automatically

When a valid release tag is pushed, `.github/workflows/livingworld-release.yml`:

1. validates `<mod_version>+<minecraft_version>` tag format;
2. verifies the tag points exactly to current `1.21.1` HEAD;
3. runs `:common:test`;
4. builds the Fabric JAR with Java 21;
5. smoke-checks required VillAIgence/LivingWorld classes inside the JAR;
6. creates the public artifact `villaigence-fabric-<tag>.jar`;
7. creates its SHA-256 checksum;
8. stores both files as a GitHub Actions artifact;
9. creates or updates the matching GitHub Release.

Alpha/beta/RC tags are marked prerelease automatically.

The workflow does not publish to Maven, Modrinth or CurseForge.

## Dry-run release packaging

Before creating a release tag:

1. Open GitHub → Actions.
2. Select **VillAIgence GitHub Release**.
3. Click **Run workflow**.
4. Select branch `1.21.1`.
5. Run it manually.

A manual run performs tests, Fabric build, package smoke-check, checksum creation and Actions artifact upload, but does **not** create a GitHub Release.

Expected Actions artifact:

```text
villaigence-fabric-package
```

## Preconditions before tagging

- All intended changes are merged into `1.21.1`.
- Required PR checks are green.
- A manual **VillAIgence GitHub Release** dry-run on `1.21.1` succeeds.
- `1.21.1` contains exactly the code intended for release.
- The chosen version tag does not already exist.
- The tag is created from current `1.21.1`, never from a feature branch.

The workflow intentionally rejects stale tags that do not point to current `1.21.1` HEAD.

## Create a release tag

Example for `0.1.5+1.21.1`:

```bash
git checkout 1.21.1
git pull --ff-only origin 1.21.1

git tag -a "0.1.5+1.21.1" \
  -m "VillAIgence 0.1.5 for Minecraft 1.21.1"

git push origin "0.1.5+1.21.1"
```

After the tag push, open GitHub → Actions → **VillAIgence GitHub Release** and verify both jobs succeed.

Expected release assets:

```text
villaigence-fabric-0.1.5+1.21.1.jar
villaigence-fabric-0.1.5+1.21.1.jar.sha256
```

The JAR filename changed with the public rebrand. Old `mca-livingworld-*` release assets remain historical artifacts and must not be rewritten.

## Test installation

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

The rebrand does not rename these internals:

```text
mod id:          mca
Java namespace:  net.conczin.mca
config:          config/livingworld.json
world data:      <world>/livingworld/
engine classes:  LivingWorld*
```

This is intentional. Renaming them would require a separate migration design and could break worlds/configs/mod compatibility.

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

Never place real API keys in Git, release assets or client modpacks.

## Release smoke test

Before using an important world, back it up and test on a disposable/private world:

1. server starts without loader/mod dependency errors;
2. MCA villagers spawn and normal MCA interactions work;
3. AI text conversation works;
4. Simple Voice Chat connects and normal player voice works;
5. microphone → STT → selected NPC → clean text works;
6. when TTS is enabled, the same clean answer plays spatially from the NPC;
7. persisted NPC voices remain stable across restart;
8. `memory.json`, `events.json`, `relationships.json` and `voices.json` remain readable after restart;
9. safe NPC actions remain whitelist/server-authority constrained;
10. forced TTS failure leaves the text reply intact.

## Failed release or bad tag

Do not create assets manually to bypass a failed workflow. Fix the cause and use a new version tag when the previous tag may already have been consumed.

Only delete a mistaken tag when no valid release should exist and it has not become a published version:

```bash
git push origin --delete "<tag>"
git tag -d "<tag>"
```

Never reuse a published release tag for different code.
