# VillAIgence Rebrand Design

## Goal

Promote VillAIgence as the public product identity of the mod while preserving all compatibility-sensitive MCA and LivingWorld internals.

## Public identity

- Product name: `VillAIgence`
- Wordplay: `Vill-AI-gence`
- Short name: `VAI`
- Tagline: `Giving villagers a mind of their own.`
- Repository: `True-Ruslan/villAIgence`

`LivingWorld` remains the internal AI/living-world engine namespace and storage prefix. It is not renamed in Java packages, configuration paths, world data paths, workflow internals, or serialized data in this rebrand.

## Compatibility invariants

The following MUST NOT change:

- Fabric/NeoForge mod id: `mca`
- Java package namespace: `net.conczin.mca`
- Minecraft world data directory: `<world>/livingworld/`
- config file path: `config/livingworld.json`
- existing JSON field names and stored data schemas
- upstream MCA compatibility assumptions
- existing release tags

Changing these would turn a branding change into a migration and could break worlds, clients, configs, integrations, or loader compatibility.

## Public-facing changes

Update product-facing surfaces to VillAIgence:

- README title, intro, installation copy, architecture description, credits wording;
- user-facing documentation headings and explanatory prose;
- release workflow display name, release title, release notes and public artifact filename;
- packaging script artifact naming;
- new public release assets use `villaigence-fabric-<tag>.jar`;
- documentation explains that `LivingWorld` is the internal engine name and `mca` remains the compatibility mod id.

## Internal naming policy

Keep `LivingWorld*` Java classes unchanged for now. They represent a coherent internal subsystem and mass-renaming them would create a large low-value diff with unnecessary regression risk.

Future architecture may expose `VillAIgence` facades/modules above LivingWorld, but that is outside this PR.

## Release compatibility

Old release assets remain immutable. New releases use the VillAIgence filename while the JAR still contains mod id `mca`.

The release workflow must continue validating tags against current `1.21.1` HEAD and producing SHA-256 checksums.

## Acceptance criteria

- A new user landing on the repository sees VillAIgence as the primary product name.
- Installation docs clearly warn not to install original MCA Reborn alongside VillAIgence.
- Existing worlds/configs continue working with no migration.
- CI and release packaging no longer present LivingWorld as the public product name except where explicitly describing the internal engine.
- Fabric and NeoForge builds remain green.
