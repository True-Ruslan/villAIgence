# LivingWorld release runbook

## Current release target

The first public test build is:

`0.1.0-alpha.1+1.21.1`

This is a Fabric-only alpha for Minecraft 1.21.1.

## What CI does automatically

When a valid release tag is pushed, `.github/workflows/gradle.yml`:

1. validates the tag format;
2. verifies the tag points exactly to the current `1.21.1` branch head;
3. runs `:common:test`;
4. builds the Fabric JAR with Java 21;
5. smoke-checks required LivingWorld/Fabric classes inside the JAR;
6. renames the public artifact to `mca-livingworld-fabric-<tag>.jar`;
7. creates a SHA-256 checksum;
8. stores both files as a GitHub Actions artifact;
9. creates a GitHub Release and attaches the JAR/checksum.

Alpha/beta/RC versions are created as GitHub prereleases.

The workflow does not publish to Maven, Modrinth or CurseForge and does not use the original MCA project IDs.

## Preconditions before creating a release tag

- All intended changes are merged into `1.21.1`.
- Required PR checks are green.
- `1.21.1` contains exactly the code you want to ship.
- Do not create the tag from a feature branch.

The release workflow deliberately fails if the tag is not on the current `1.21.1` head.

## Create the first release from Git CLI

```bash
git checkout 1.21.1
git pull --ff-only origin 1.21.1
git tag -a "0.1.0-alpha.1+1.21.1" -m "LivingWorld 0.1.0-alpha.1 for Minecraft 1.21.1"
git push origin "0.1.0-alpha.1+1.21.1"
```

After the tag push, open GitHub → Actions → `LivingWorld GitHub Release` and wait for all jobs to succeed.

Then open GitHub → Releases. The release should contain:

- `mca-livingworld-fabric-0.1.0-alpha.1+1.21.1.jar`
- `mca-livingworld-fabric-0.1.0-alpha.1+1.21.1.jar.sha256`

## Create the release tag from GitHub UI

Prefer the Git CLI path above because it makes the exact tagged commit obvious.

When using GitHub UI, create the tag `0.1.0-alpha.1+1.21.1` from the latest commit on branch `1.21.1`. Do not manually create a GitHub Release first: pushing/creating the tag is the trigger and CI creates the Release automatically.

## Test installation

Required stack:

- Minecraft 1.21.1
- Fabric Loader
- Fabric API compatible with Minecraft 1.21.1
- Simple Voice Chat 2.6.20 or newer
- Java 21 on the server
- LivingWorld release JAR

The fork keeps MCA's internal mod id `mca`. **Remove the original MCA Reborn JAR before installing LivingWorld. Do not install both simultaneously.**

For multiplayer, use the same LivingWorld/MCA fork version on server and clients where MCA is required.

## AI configuration

Normal MVP setup requires only one server-side OpenAI key.

Recommended production setup:

```bash
export OPENAI_API_KEY="..."
```

Alternative:

1. start the dedicated server once;
2. stop it;
3. edit `config/livingworld.json`;
4. set `apiKey`;
5. restart the server.

Never put a real API key in Git, a release asset or a client modpack.

## First alpha smoke test

Before using an important world, make a backup and test on a disposable/private world:

1. server starts with no loader/mod dependency errors;
2. MCA villagers spawn and normal MCA interaction works;
3. text AI conversation works with the configured provider;
4. Simple Voice Chat connects and normal player voice works;
5. select an MCA NPC and verify voice → STT → AI response → spatial TTS;
6. restart the server and verify persistent conversation memory remains;
7. verify `world/livingworld/memory.json`, `events.json` and `relationships.json` are created/updated as expected;
8. exercise safe actions such as follow/stay and verify no unrestricted commands are possible.

## Version progression

Use sequential prerelease tags for fixes discovered during testing:

- `0.1.0-alpha.1+1.21.1`
- `0.1.0-alpha.2+1.21.1`
- `0.1.0-alpha.3+1.21.1`

Move to beta only after repeated clean server tests and migration/update testing.

## Failed release or bad tag

If the workflow fails, do not create assets manually. Fix the cause and create a new prerelease tag when possible.

If a tag was created by mistake and no release should exist:

```bash
git push origin --delete "0.1.0-alpha.1+1.21.1"
git tag -d "0.1.0-alpha.1+1.21.1"
```

Do not reuse a published release tag for different code. Once users may have downloaded a build, increment the prerelease number instead.
