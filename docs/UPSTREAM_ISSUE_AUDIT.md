# Upstream MCA issue audit & adoption backlog

## Purpose

The upstream repository `Luke100000/minecraft-comes-alive` has a large open backlog mixing current bugs, reports for older Minecraft versions, third-party mod incompatibilities, experimental regressions, and broad feature requests.

This document is our adoption filter. We do **not** copy upstream issues blindly. Every item must be classified by relevance to our `1.21.1` fork and LivingWorld goals.

## Audit scope

A coarse triage was performed across all open upstream issue creation ranges from 2022 through 2026. No open issues created before 2022 were returned by upstream search. High-impact candidates were then inspected in more depth using issue bodies/comments and current applicability.

Our current upstream base line is `7.7.22` / commit `a3de832`. Experimental `7.7.23-alpha` regressions are therefore sync blockers/watch items, not current bugs in our fork.

---

## P0 / P1 — adopt into our technical backlog

### Upstream #884 — Hole villagers cause lag

**Status:** ADOPT / investigate high priority

Created by the upstream author as a bug: a stuck NPC appears to run a task without an adequate cooldown, causing lag.

**Our action:** profile AI goals/brain behavior in stuck states; add rate limiting/retry backoff where needed; add diagnostics/regression coverage.

### Upstream #1088 — Pathfinding Megathread

**Status:** ADOPT / umbrella

Upstream-maintained pathfinding backlog. Reproduced reports include villagers getting stuck behind walls/fences, failure to reach beds across floors, children struggling with doors, unsafe drops into holes, drowning/water traps, and workstation pathing failures.

**Our action:** dedicated pathfinding/anti-stuck workstream. Prefer generic navigation fixes and bounded failsafes over per-block patches or unconditional teleport hacks.

### Upstream #580 — Blueprints locked to Player/Village

**Status:** ADOPT / multiplayer safety

Blueprint operations can potentially let players mutate villages they do not own/manage. Even on a private server this is a permission boundary.

**Our action:** audit all blueprint mutation endpoints and add explicit owner/admin/permission checks where required.

### Upstream #1314 — ChatAI hallucinates inventory/location

**Status:** ADOPT / LivingWorld P1

The issue explicitly reports that ChatAI does not reliably know its location or what it actually possesses.

**Our action:** implement an immutable world/NPC/player context snapshot captured on the Minecraft server thread, then consumed asynchronously by the LLM. Include bounded factual location/dimension/biome/time/weather, equipment/held items, relevant NPC inventory facts, nearby entities/events, and knowledge provenance.

### Upstream #1243 — NPC memory / awareness / NPC-to-NPC interaction

**Status:** PARTIALLY DONE / ADOPT remaining

- persistent NPC × player memory — **already implemented by us**;
- awareness of surroundings — TODO;
- NPC-to-NPC interaction/knowledge exchange — future;
- deeper persistent personalities — future.

**Our action:** Context Snapshot → Event/Knowledge layer → controlled NPC-to-NPC information exchange.

### Upstream #1292 — Interaction impact with villagers

**Status:** ADOPT / LivingWorld P1-P2

Player conversations/actions should have gameplay consequences instead of being cosmetic text only.

**Our action:** structured relationship deltas (`trust`, `respect`, `fear`, `affinity`) with validation/clamping, world events, persistence, and deterministic consequences such as avoidance/cooperation/trade behavior.

### Upstream #1140 — spouse/parent/child dialogue identity

**Status:** INVESTIGATE / high product value

Report says spouses/children sometimes talk as strangers/adults. Original report is for 1.20.1, so runtime reproduction is required on our 1.21.1 stack.

**Our action:** age/kinship/relationship identity snapshot and dialogue tests. Do not mark as confirmed 1.21.1 runtime bug until reproduced.

### Upstream #912 — flirty trait on children

**Status:** INVESTIGATE / safety & quality

Our LLM prompt already forbids romantic/flirty child responses, but invalid trait/domain combinations should be prevented in the domain layer, not only hidden by prompt rules.

**Our action:** audit trait generation/inheritance/editor paths and prevent age-incompatible traits/behaviors.

---

## P2 — relevant after core reliability

### Upstream #838 — API for addons / extensibility

Custom building rules, professions, conditions, and pack integration align with our adapter/data-driven direction. Defer until core stability is stronger.

### Upstream #1277 — Carry On / right-click interaction conflicts

Useful modpack UX issue. Consider an explicit interaction keybind/gesture after voice targeting and interaction UX are stabilized.

### Upstream #1132 — configurable modded threats

Good extension point for world awareness/modpacks. Prefer tags/config/data-driven threat definitions over hardcoding individual mods.

### Upstream #929 / #1148 — Dramatic Doors / modded bridges pathing

Fold into #1088 pathfinding workstream. Avoid one-off compatibility patches where a generic path/navigation abstraction can solve the class of problems.

### Upstream #862 — villagers cannot exit water

Also part of #1088. Needs generic water escape/navigation behavior.

### Upstream #524 — Rose Gold tags / Tinkers compatibility

Potential low-risk compatibility/data-quality fix. Verify current 1.21.1 tags before implementation.

---

## WATCH — do not import now

### Upstream #1373 — `7.7.23-alpha` blueprint regressions

**Status:** WATCH / upstream-sync blocker

Reported regressions include multi-floor inns not recognized correctly, two-block-tall blocks double-counted, open patios/doorless entries causing volume errors, and scan-height sensitivity.

Our base is `7.7.22`, so these are not current fork bugs. Do not import the experimental blueprint rewrite until these behaviors are checked/fixed and covered by tests.

### Upstream #1372 — candle blockstate counts / water scanning

Valid scanner enhancement, but tied to the newer/experimental building system. Not a current blocker.

### Upstream #1369 — VulkanMod Reforged visual incompatibility

Current NeoForge-specific compatibility report. Our main LivingWorld MVP target is Fabric 1.21.1. Track only if NeoForge becomes a supported deployment target.

### Upstream #1363 — VillagerTradeFix infinite trades

Reproduced on Fabric 1.20.1 with a third-party mod. Do not adopt without reproduction on 1.21.1 and without that mod being part of our target modpack.

### Upstream #1282 — Chunky + MCA slow pregeneration

Upstream comments include profiling that attributes most of the cost to vanilla `finalizeSpawn()` synchronously searching/loading chunks while pregenerating. Do **not** classify as a direct MCA bug without new evidence. Spawn strategy optimization may still be useful later.

### Upstream #1263 — Invisible Buildings

Old 1.20.1 building-system report with non-deterministic reproduction; likely partly superseded by later blueprint work. Requires target-version reproduction before adoption.

---

## REJECT / DEFER BY DESIGN

### Upstream #1096 — dialogue action executing arbitrary server-level commands

**Status:** REJECT for LivingWorld security model

LivingWorld must never give an LLM/NPC unrestricted command execution. Only hard-coded whitelisted actions with schema validation, permission/precondition checks, and server-thread execution are allowed.

### Broad content requests

War/industrial age, complete pillager factions, large furniture/content packs, dozens of professions, infection progression, etc. are product epics rather than core defects. Consider only after reliability, context, memory, relationships, world events, and NPC autonomy are mature.

### Ancient mod-specific reports

1.18/1.19/1.20 compatibility reports without reproduction on our 1.21.1 target are not automatically adopted as bugs. Reproduce first; then prefer generic compatibility fixes where possible.

---

## Upstream issue adoption rule

An upstream issue becomes actionable for our fork when at least one condition is true:

1. it reproduces on our target stack (Fabric 1.21.1);
2. it is a cross-version architectural defect (security, data loss, deadlock, severe lag, unsafe threading);
3. it is confirmed/maintained upstream and affects core behavior;
4. it directly advances LivingWorld goals: memory, context, relationships, world events, safe actions, voice, or NPC autonomy;
5. it is a third-party compatibility issue for a mod actually included in our target server/modpack.

An issue remaining `open` upstream is **not** sufficient reason to copy it into our roadmap.

---

## Ordered work derived from this audit

1. **Context Snapshot / thread-boundary architecture** — foundation for #1314 and part of #1243.
2. **Pathfinding + anti-stuck audit** — #884 + #1088 + #862/#929/#1148.
3. **Blueprint permission audit** — #580.
4. **Age/kinship/relationship correctness** — #912 + #1140 + #1292.
5. **Upstream sync gate** — do not import `7.7.23-alpha` blueprint work until #1373/#1372 are verified.

## Maintenance

At each upstream sync:

1. inspect new upstream issues and relevant recent commits;
2. update this audit first;
3. only then create implementation work for adopted items;
4. record when an adopted issue becomes fixed, obsolete, or superseded.
