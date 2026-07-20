# Upstream MCA issue audit & adoption backlog

## Purpose

The upstream repository `Luke100000/minecraft-comes-alive` has a large open backlog mixing current bugs, reports for older Minecraft versions, third-party mod incompatibilities, experimental regressions, and broad feature requests.

This document is the **single canonical, version-controlled adoption filter** for our fork. We do **not** copy upstream issues blindly. Every item must be classified by relevance to our `1.21.1` fork and LivingWorld goals.

## Audit scope

A coarse triage was performed across all open upstream issue creation ranges from 2022 through 2026, followed by deeper inspection of high-impact candidates and older backlog batches, including the issues visible on older upstream pages such as page 7.

Our current upstream base line remains the stable `7.7.22` line unless explicitly synchronized. Experimental `7.7.23-alpha` regressions are sync blockers/watch items, not automatically current bugs in our fork.

---

## P0 / P1 — adopted technical backlog

### Upstream #1088 — Pathfinding Megathread

**Status:** PARTIALLY MITIGATED / umbrella remains open

Upstream reports cover villagers stuck behind walls/fences, beds across floors, children struggling with doors, unsafe drops, water traps and workstation pathing failures.

**Implemented in fork:** PR #11 adds a bounded progress watchdog to `WanderOrTeleportToTargetTask`. When an NPC makes no meaningful progress towards the same `WALK_TARGET` for a sustained period, the navigation path is recomputed without deleting the target intent and without enabling unconditional teleportation. Movement or target changes reset the watchdog. Both LivingWorld CI and the official Fabric/NeoForge Gradle workflow passed before merge.

**Remaining work:** this is not a claim that #1088 is fully fixed. Door handling, water escape/drowning, unsafe drops, long-range/multi-floor target selection and workstation-specific cases need focused reproduction and generic fixes.

### Upstream #884 — Hole villagers cause lag

**Status:** NEEDS REPRODUCTION / partially protected

Current `1.21.1` already has an explicit `SLOWDOWN = 5` pathfinding gate. PR #11 additionally prevents indefinite stale-path behavior by bounded replanning after lack of progress. This still does not prove the original lag hotspot is gone.

**Our action:** profile current Fabric 1.21.1 before adding any further cooldown/backoff. Fix only the task proven hot by profiling.

### Upstream #580 — Blueprints locked to Player/Village

**Status:** FIXED IN FORK / keep regression coverage

PR #9 closed a real server-authority gap: blueprint/village mutation packets now enforce server-owned rank and nearest-village checks, validate mutation values/types, sanitize names, prevent destructive confirmation replay paths, and explicitly persist valid changes. Fabric and NeoForge CI passed before merge.

**Our action:** retain authority-policy tests and re-audit whenever upstream blueprint networking is synchronized.

### Upstream #1314 — ChatAI hallucinates inventory/location

**Status:** PARTIALLY IMPLEMENTED / LivingWorld P1 remaining

The direct LivingWorld/OpenAI path captures an immutable server-thread context snapshot containing dimension, coordinates, biome, day/night, weather, held/equipped item facts, bounded villager inventory facts, existing MCA personality/relationship/village/player context and currently available safe actions. Prompt/memory/network work uses copied snapshot data asynchronously.

The Event/Knowledge foundation adds a bounded server-owned factual event journal. Recent same-dimension nearby events are injected into snapshots with explicit `SYSTEM_OBSERVED` provenance; arbitrary player/LLM text is not promoted to factual world state.

**Remaining action:** broader nearby entity/event sources, knowledge provenance beyond direct observations, and migration/verification of historical ChatAI/Inworld callers.

### Upstream #1243 — NPC memory / awareness / NPC-to-NPC interaction

**Status:** PARTIALLY DONE / ADOPT remaining

- persistent NPC × player conversation memory — **implemented**;
- authoritative basic factual context snapshot — **implemented for direct LivingWorld/OpenAI flow**;
- bounded recent local factual event awareness — **implemented as foundation**;
- player claims / uncertain beliefs / rumors — future separate knowledge layer;
- controlled NPC-to-NPC information exchange — future;
- deeper persistent personality/knowledge model — future.

**Our action:** expand verified event sources carefully → add belief/provenance model → controlled NPC-to-NPC knowledge exchange. Never collapse world truth, player claims and rumors into one undifferentiated memory stream.

### Upstream #1292 — Interaction impact with villagers

**Status:** ADOPT / LivingWorld P1-P2

Conversations and actions should have deterministic gameplay consequences instead of being cosmetic text only.

**Our action:** structured relationship deltas (`trust`, `respect`, `fear`, `affinity`) with schema validation/clamping, persistence and deterministic effects such as cooperation, avoidance, information sharing and trade behavior.

### Upstream #1140 — spouse/parent/child dialogue identity

**Status:** PARTIALLY COVERED / NEEDS REPRODUCTION

The original report targets 1.20.1. Current `1.21.1` ChatAI already injects age and explicit spouse/parent/child/relative facts, so a current LivingWorld context defect is not proven.

**Our action:** reproduce classic MCA dialogue behavior on 1.21.1 and add regression tests only for remaining failures.

---

## DATA INTEGRITY — investigate before feature expansion

### Upstream #977 — Negative Billion Hearts

**Status:** INVESTIGATE / potential numeric-state corruption

The report describes relationship hearts jumping to approximately `-2.147B`, strongly suggestive of overflow/sentinel/state corruption, but it lacks a current 1.21.1 reproduction.

**Our action:** audit heart mutation/clamping/serialization paths and reproduce on a clean current stack before patching. Any relationship model added by LivingWorld must use explicit bounds and reject overflow/non-finite inputs.

### Upstream #1234 and #978 — missing/deceased NPC recovery

**Status:** INVESTIGATE / data-consistency class

Reports describe entities disappearing or dying without a recoverable grave/entity record while FamilyTree still retains identity/relationship UUID data. #1234 involved a modded vehicle on 1.20.1; #978 describes broader missing-grave recovery scenarios.

**Our action:** do not assume a current MCA core bug yet. Audit entity lifecycle → family tree → civil registry/grave consistency, define invariants, and add recovery/diagnostic tooling before attempting speculative resurrection logic.

---

## VERIFIED FIXED / OBSOLETE FOR CURRENT FORK

### Upstream #912 — flirty trait on children

**Status:** VERIFIED FIXED in current `1.21.1`

`Personality.getRandom(AgeState)` excludes `FLIRTY` for `BABY`, `TODDLER` and `CHILD`. LivingWorld additionally blocks romantic/flirty child responses in LLM context.

**Our action:** retain regression protection when personality generation changes.

---

## P2 — relevant after core reliability

### Upstream #838 — API for addons / extensibility

Custom building rules, professions, conditions and pack integration align with a data-driven adapter direction. Defer until core reliability is stronger.

### Upstream #1277 — Carry On / right-click interaction conflicts

Valid modpack/UX design problem and relevant because interaction also selects a LivingWorld voice target. Prefer a unified configurable interaction policy/keybind/gesture rather than a Carry On-specific hack.

### Upstream #1132 — configurable modded threats

Prefer tags/config/data-driven threat definitions over hardcoding individual mods.

### Upstream #929 / #1148 — Dramatic Doors / modded bridges pathing

Remain under #1088. Prefer generic navigation fixes over per-mod block patches.

### Upstream #862 — villagers cannot exit water

Remain under #1088. Needs generic water escape/navigation behavior and drowning safety.

### Upstream #524 — Rose Gold tags / Tinkers compatibility

Potential low-risk compatibility/data-quality fix. Verify current 1.21.1 tags before implementation.

---

## WATCH — do not import now

### Upstream #1373 — `7.7.23-alpha` blueprint regressions

**Status:** WATCH / upstream-sync blocker

Reported regressions include multi-floor inns, double-counted multi-block objects, open patios/doorless entries, volume errors and scan-height sensitivity. These belong to experimental upstream work not automatically present in our stable fork.

### Upstream #1372 — candle blockstate counts / water scanning

Valid scanner enhancement tied to newer/experimental building work. Include in regression tests before any sync.

### Upstream #1369 — VulkanMod Reforged visual incompatibility

NeoForge-specific compatibility report. Track if it affects an actually supported deployment/modpack.

### Upstream #1363 — VillagerTradeFix infinite trades

Reproduced on Fabric 1.20.1 with a third-party mod. Requires 1.21.1 reproduction before adoption, but independently preserve the invariant that profession switching must not grow offers/entity data without bounds.

### Upstream #1283 — player hitbox/block-interaction regression

Reported on NeoForge 1.21.1: water placement/raycast behavior changes with MCA. Treat as a high-value reproduction candidate because it affects core Minecraft interaction. Compare Fabric/NeoForge, vanilla/MCA model and hitbox/model configuration before patching.

### Upstream #1282 — Chunky + MCA slow pregeneration

Profiling in the issue attributes most cost to vanilla villager `finalizeSpawn()` while pregenerating unloaded chunks. Do not classify as an MCA bug without fork-specific profiling.

### Upstream #1263 — Invisible Buildings

Old 1.20.1 non-deterministic building-system report; requires target-version reproduction.

---

## REJECT / DEFER BY DESIGN

### Upstream #1096 — dialogue action executing arbitrary server-level commands

**Status:** REJECT for LivingWorld security model

An LLM/NPC must never receive unrestricted command execution. Only hard-coded whitelisted actions with schema validation, permission/precondition checks and server-thread execution are allowed. The fork already follows this model.

### Broad content requests

War/industrial age, complete pillager factions, furniture/content packs, dozens of professions and similar proposals are product epics, not bug-fix imports. Split and prioritize only after reliability, context, memory, relationships, events and NPC autonomy are mature.

### Ancient mod-specific reports

1.18/1.19/1.20 compatibility reports without reproduction on target 1.21.1 are not automatically adopted. Prefer generic compatibility fixes where possible.

---

## Upstream issue adoption rule

An upstream issue becomes actionable for our fork when at least one condition is true:

1. it reproduces on our target stack (primarily Fabric 1.21.1);
2. it is a cross-version architectural defect (security, data loss, deadlock, severe lag, unsafe threading);
3. it is confirmed/maintained upstream and affects core behavior;
4. it directly advances LivingWorld goals: memory, context, relationships, events, safe actions, voice or NPC autonomy;
5. it is a third-party compatibility issue for a mod actually included in our target server/modpack.

An issue remaining `open` upstream is **not** sufficient reason to copy it into our roadmap.

---

## Ordered work derived from this audit

1. **Pathfinding hardening** — PR #11 adds generic stale-path recovery; next isolate doors, water/drowning, unsafe drops and multi-floor/long-range cases under #1088.
2. **Relationship consequences** — #1292, built on validated/clamped structured deltas and deterministic gameplay effects.
3. **Knowledge provenance and sharing** — continue #1243 beyond factual event observations into claims/rumors/NPC-to-NPC propagation.
4. **Data-integrity invariants/reproduction** — #977, #1234, #978, plus family tree/entity lifecycle consistency.
5. **Age/kinship correctness** — reproduce remaining #1140 behavior; #912 is verified fixed.
6. **Interaction policy** — #1277 after voice/interaction UX stabilization.
7. **Upstream sync gate** — do not import `7.7.23-alpha` blueprint work until #1373/#1372 regressions are covered.

## Maintenance

At each upstream sync or meaningful fork milestone:

1. inspect new upstream issues and relevant commits;
2. update this audit first;
3. create implementation work only for adopted/testable findings;
4. record linked PRs and downgrade/remove resolved findings;
5. keep this file as the single canonical issue audit to avoid split-brain backlog documents.
