# GitHub-only alpha release pipeline design

## Goal

Make the fork releasable as a safe Fabric 1.21.1 alpha build through GitHub without using the original MCA Maven, Modrinth or CurseForge publishing credentials/project IDs.

## Release format

Release tags use:

`<mod_version>+<minecraft_version>`

First planned release:

`0.1.0-alpha.1+1.21.1`

The Gradle build already derives the project version from the tag at `HEAD`, so the tag remains the single release-version source.

## Pipeline

Replace the inherited upstream tag-publishing workflow with a fork-owned GitHub-only workflow.

On pull requests that modify the release workflow or release documentation, the workflow performs a dry-run build/package verification but never publishes a release.

On a valid release tag, the workflow:

1. validates the tag format;
2. checks that the tagged commit is exactly the current `1.21.1` branch head;
3. uses Java 21;
4. runs `:common:test` and `:fabric:build`;
5. verifies the exact Fabric JAR exists and contains `fabric.mod.json`, LivingWorld config classes and the Fabric voice conversation integration;
6. renames the public asset to `mca-livingworld-fabric-<tag>.jar`;
7. generates a SHA-256 checksum;
8. uploads the JAR/checksum as a GitHub Actions artifact;
9. creates a GitHub prerelease for alpha/beta/rc tags and attaches the JAR/checksum.

## Publication boundary

The release workflow must contain no upstream:

- Modrinth project IDs/tokens;
- CurseForge project IDs/tokens;
- Maven publish step/credentials.

Only the repository-scoped `GITHUB_TOKEN` is required for creating a GitHub Release.

## Loader scope

The first LivingWorld release is Fabric-only because the voice MVP integration lives in the Fabric module and depends on Simple Voice Chat there. NeoForge continues to compile in PR CI for compatibility but is not published as a LivingWorld alpha asset yet.

## Release artifact

Public asset:

`mca-livingworld-fabric-0.1.0-alpha.1+1.21.1.jar`

The internal MCA mod id remains `mca`, so users must replace the original MCA Reborn JAR rather than install both simultaneously.

Required runtime dependencies for the alpha test stack:

- Minecraft 1.21.1;
- Fabric Loader;
- Fabric API compatible with 1.21.1;
- Simple Voice Chat 2.6.20 or newer.

## Release notes

The generated GitHub release notes must clearly state:

- alpha/testing status;
- Fabric 1.21.1 only;
- Java 21;
- required dependencies;
- do not install original MCA Reborn alongside this fork;
- server owner configures only `OPENAI_API_KEY` or `config/livingworld.json` `apiKey` for the normal MVP path.

## Failure policy

A release is not created if tag validation, branch-head verification, tests, build, JAR smoke checks, checksum generation or artifact upload fails.

No `continue-on-error` or `fail-mode: skip` is allowed in the release-critical path.
