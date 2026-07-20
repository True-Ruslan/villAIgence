# Upstream MCA issue triage

Last review: 2026-07-20  
Upstream: `Luke100000/minecraft-comes-alive`  
Target fork branch: `1.21.1`

## Purpose

This is the canonical, version-controlled registry for upstream MCA issues that may affect the fork or the LivingWorld roadmap. It deliberately does not mirror every open upstream issue. Old reports, duplicates, unsupported Minecraft versions and third-party conflicts are filtered before they enter the fork backlog.

## Triage statuses

- **P0/P1 confirmed** — current 1.21.1 defect with a reliable reproduction, affecting crashes, data integrity, core gameplay or severe performance.
- **P2 candidate** — relevant and credible, but still needs reproduction or profiling on the fork.
- **LivingWorld opportunity** — not necessarily an upstream defect, but useful for the LivingWorld product direction.
- **External compatibility** — conflict with a third-party mod; fix only when ownership and integration boundaries are clear.
- **Stale / unsupported** — old version, duplicate, insufficient evidence or already resolved in current code.
- **Upstream alpha only** — concerns experimental upstream work not yet present in the fork. Track during future upstream synchronization; do not port automatically.

## Batch 1 — current high-value candidates

### P1 candidate: player hitbox affects block interaction

- Upstream: [#1283 — water is placed on top instead of the aimed side](https://github.com/Luke100000/minecraft-comes-alive/issues/1283)
- Reported scope: NeoForge 1.21.1; reporter says Forge 1.20.1 is unaffected.
- Current hypothesis: MCA player-model scale/hitbox changes alter the eye or interaction ray origin.
- Required validation:
  - clean Fabric 1.21.1 reproduction;
  - clean NeoForge 1.21.1 reproduction;
  - compare vanilla model, MCA model and relevant hitbox/model config;
  - add an interaction-ray regression test where practical.
- Decision: do not patch until reproduced, but treat as high priority because it changes basic Minecraft interaction.

### Upstream alpha P1: blueprint scanner regressions

- Upstream: [#1373 — new blueprint system regressions](https://github.com/Luke100000/minecraft-comes-alive/issues/1373)
- Confirmed report scope: MCA `7.7.23-alpha.*`, Minecraft 1.21.1.
- Reported cases:
  - multifloor inns not classified correctly;
  - two-block-tall modded blocks counted twice;
  - open patios and doorless entrances produce `building has too large volume`;
  - scan result changes depending on the scanner's vertical position.
- Decision: this is not currently a fork regression unless the alpha blueprint work is synchronized. Before any future port, require a regression suite for multi-floor, open-plan, modded multi-block and scan-origin cases.

### P2 external compatibility: accumulating trades

- Upstream: [#1363 — VillagerTradeFix permits infinite accumulated trades](https://github.com/Luke100000/minecraft-comes-alive/issues/1363)
- Original report: Fabric 1.20.1, MCA 7.6.26.
- Risk: repeated workplace switching may append offers indefinitely and grow entity data.
- Required validation:
  - test current 1.21.1 with and without VillagerTradeFix;
  - inspect offer replacement/retention semantics on profession change;
  - add an invariant that offers remain bounded and do not silently accumulate unrelated professions.
- Decision: compatibility issue until reproduced, but the data-growth invariant is worth testing independently.

### P2 UX and compatibility: right-click interception

- Upstream: [#1277 — incompatibility with Carry On and other right-click integrations](https://github.com/Luke100000/minecraft-comes-alive/issues/1277)
- Relevance to LivingWorld: right-click currently also selects the NPC voice-conversation target.
- Proposed direction:
  - configurable modifier/keybind for opening the MCA interaction menu;
  - pass-through when another mod claims the interaction;
  - explicit, visible voice-target selection state;
  - avoid permanently swallowing entity interaction hooks.
- Decision: valid design problem; requires a unified interaction policy rather than a Carry On-specific hack.

## Valid report, but not currently an MCA-specific defect

### Chunk pregeneration load

- Upstream: [#1282 — Chunky + MCA slows world generation](https://github.com/Luke100000/minecraft-comes-alive/issues/1282)
- The discussion includes profiling that attributes the main cost to vanilla villager `finalizeSpawn()` while chunks are not fully loaded: bed/workstation search, AI initialization and village checks trigger synchronous chunk work.
- Decision:
  - do not apply an MCA optimization without fork-specific profiling;
  - document operational guidance for chunk pregeneration;
  - profile our server before and after LivingWorld work because AI additions must not worsen spawn-time cost;
  - consider temporarily limiting villager spawning during offline pregeneration only if profiling proves it useful and safe.

## LivingWorld opportunities extracted from upstream

### P1 architecture: reduce ChatAI hallucinations with authoritative context

- Upstream: [#1314 — broad improvement proposal, including ChatAI location/inventory hallucinations](https://github.com/Luke100000/minecraft-comes-alive/issues/1314)
- Direct implication: LivingWorld needs a server-thread immutable context snapshot before asynchronous LLM execution.
- Snapshot candidates:
  - NPC/player identity and relation;
  - dimension, position, biome, time and weather;
  - held/equipped items and bounded inventory/possession summary;
  - nearby entities and relevant structures;
  - explicit distinction between world truth and facts known by the NPC.
- Decision: high-priority LivingWorld architecture work. Never give the model fictional ownership or location data when the server can provide the truth.

### P2: dialogue should affect relationships and gameplay

- Upstream: [#1292 — interaction impact with villagers](https://github.com/Luke100000/minecraft-comes-alive/issues/1292)
- Direction: structured relationship deltas such as trust, respect, fear and affinity, validated and clamped server-side. Effects may influence trade, willingness to follow, information sharing and later quest generation.
- Decision: roadmap item after safe context snapshots and persistent memory.

### Roadmap cluster: identity, family standing, proactive dialogue and quests

- Upstream: [#1291 — surnames, families, interaction, quests and reputation](https://github.com/Luke100000/minecraft-comes-alive/issues/1291)
- Split into independent systems:
  - identity and lineage;
  - family/village reputation;
  - NPC-initiated conversations;
  - gossip and shared knowledge;
  - generated quests with deterministic validation.
- Decision: useful product direction, but never implement as one oversized feature.

## Older backlog, including upstream page 7

The older pages contain a mixture of historical crashes, old-loader reports, compatibility requests and broad suggestions. They must be processed in batches rather than copied into the fork.

### Batch groups

- [ ] rendering, player model, skin and animation compatibility;
- [ ] profession, workplace, building and AI-navigation defects;
- [ ] family tree, marriage, child and world-migration data integrity;
- [ ] server/client config synchronization;
- [ ] third-party mod compatibility;
- [ ] gameplay suggestions that overlap LivingWorld;
- [ ] reports limited to Minecraft versions older than 1.20;
- [ ] duplicates or issues lacking logs/reproduction.

### Examples already observed in the older backlog

- [#263 — revived villagers remain sad/widowed](https://github.com/Luke100000/minecraft-comes-alive/issues/263): plausible relationship-state defect, but very old; needs current reproduction.
- [#376 — rendering crash with Mowzie's and animated attacks](https://github.com/Luke100000/minecraft-comes-alive/issues/376): old compatibility report; stale unless reproduced on 1.21.1.
- [#415 — configurable name lists](https://github.com/Luke100000/minecraft-comes-alive/issues/415): useful customization request, not a defect.
- [#430 — TerraFirmaCraft compatibility](https://github.com/Luke100000/minecraft-comes-alive/issues/430): broad third-party integration; not fork core unless included in the target modpack.
- [#452 — server config/model synchronization](https://github.com/Luke100000/minecraft-comes-alive/issues/452): potentially important class of defect; verify whether the current networking/config implementation still has the problem.
- [#494 — miners and lumberjacks](https://github.com/Luke100000/minecraft-comes-alive/issues/494): gameplay expansion, possible future LivingWorld profession/action layer.
- [#516 — Regrowth compatibility](https://github.com/Luke100000/minecraft-comes-alive/issues/516): useful reference for an extensible NPC action adapter, but ownership may belong to the integration mod.
- [#524 — rose-gold tags for Tinker's Construct](https://github.com/Luke100000/minecraft-comes-alive/issues/524): narrow tag/compatibility issue; cheap to verify when the relevant item/tag code is audited.

## Rules for promoting an item into the fork backlog

1. Do not create a dedicated fork task without a current reproduction or a clear testable contract.
2. Record Minecraft version, loader, MCA version and third-party mod versions.
3. Separate upstream defects from compatibility behavior owned by another mod.
4. Do not cherry-pick upstream alpha systems without migration tests and a regression suite.
5. Confirm that the issue is not already fixed in the fork or newer upstream commits.
6. Use one focused issue and PR per confirmed defect.
7. Keep upstream synchronization work separate from LivingWorld feature PRs.
8. For AI-related requests, the LLM may propose behavior but the server remains authoritative.

## Next review actions

- [ ] Complete metadata scan of all open upstream issues.
- [ ] Triage older issues by the batch groups above.
- [ ] Reproduce P1/P2 candidates on clean Fabric 1.21.1.
- [ ] Compare relevant fixes already present in newer upstream commits.
- [ ] Add dedicated fork tasks only for confirmed findings.
- [ ] Update this document with review date, status and linked PR when a finding is fixed.
