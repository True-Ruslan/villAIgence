# Filled Grave Drop Safety Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent a filled MCA tombstone from destroying its stored body and inventory when evaluated loot omits the tombstone block item, including the installed Silk Touch failure.

**Architecture:** Keep vanilla/datapack loot evaluation authoritative. A loader-independent policy copies the evaluated list, reuses the first tombstone item or appends one fallback item, and serializes the stored body into that one target. A narrow common Mixin applies the policy at the return of MCA-owned `TombstoneBlock.getDrops()` only when the block entity is filled; this avoids replacing the large tombstone class while preserving its existing remains-name and item-data logic.

**Tech Stack:** Java 21, Minecraft 1.21.1 Mojang mappings, Sponge Mixin, JUnit 5, Fabric Loom, NeoForge.

## Global Constraints

- Base: `1.21.1` at `ff4df17a086b6f904f7efda3abc77f8a135fa8ed`.
- Preserve internal mod ID `mca` and package root `net.conczin.mca`.
- Do not change tombstone block-entity NBT or item component schemas.
- Do not change empty-tombstone loot behavior.
- Do not duplicate a tombstone item when loot already contains one.
- Preserve unrelated loot entries, their order and remains-name decoration.
- Do not change water navigation, AI providers, Chat/STT/TTS, memory, packets, operator lore, dependencies or embedded release version.
- Require common tests, Fabric/NeoForge builds, distributable package verification and repository security checks.
- Installed Silk Touch acceptance remains pending until an exact candidate JAR is tested.

---

### Task 1: Canonical RED policy contract

**Files:**
- Create: `common/src/test/java/net/conczin/mca/block/TombstoneDropPolicyTest.java`

**Interfaces:**
- Consumes: planned `TombstoneDropPolicy.ensurePreservedDrop(List<T>, Predicate<T>, Supplier<T>, Consumer<T>)`.
- Produces: contracts for fallback creation, existing-item reuse, order preservation, input-list isolation and exactly-once serialization.

- [x] Write four tests using loader-independent fake drops.
- [x] Run `:common:test` through CI.
- [x] Confirm canonical RED is `:common:compileTestJava` failure because `TombstoneDropPolicy` is absent.
- [x] Commit RED at `4448f2a062e76645fefa0d67fd499a0c1824a540`.

### Task 2: Pure drop preservation policy

**Files:**
- Create: `common/src/main/java/net/conczin/mca/block/TombstoneDropPolicy.java`

**Interfaces:**
- Consumes: evaluated drops, self-item predicate, fallback supplier and serialization callback.
- Produces: a new ordered list with one serialized tombstone target.

- [x] Validate collaborators with `Objects.requireNonNull`.
- [x] Copy the input list into `ArrayList`.
- [x] Reuse the first matching tombstone or append one fallback.
- [x] Invoke the serialization callback exactly once.

### Task 3: Narrow runtime integration

**Files:**
- Create: `common/src/main/java/net/conczin/mca/mixin/MixinTombstoneBlock.java`
- Modify: `common/src/main/resources/mca.mixins.json`

**Interfaces:**
- Consumes: the return value of MCA-owned `TombstoneBlock.getDrops()`, `LootContextParams.BLOCK_ENTITY`, `TombstoneBlock.Data.hasEntity()` and `Data.writeToStack(ItemStack)`.
- Produces: unchanged output for empty graves; preserved block item output for filled graves.

- [x] Inject at `RETURN`, cancellable, with `remap = false` because the target method is MCA-owned.
- [x] Filter to a filled `TombstoneBlock.Data`.
- [x] Reuse `block.asItem()` when present or create `new ItemStack(block.asItem())`.
- [x] Register `MixinTombstoneBlock` in the common Mixin configuration.
- [x] Confirm initial GREEN: VillAIgence CI #1266, Java PR CI #712, security #617.

### Task 4: Permanent runtime-wiring contract

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/block/TombstoneDropPolicyTest.java`

**Interfaces:**
- Consumes: Mixin source and `mca.mixins.json`.
- Produces: a test failure if the policy remains present but is no longer registered or applied to filled tombstones.

- [x] Require `"MixinTombstoneBlock"` in `mca.mixins.json`.
- [x] Require `TombstoneDropPolicy.ensurePreservedDrop`, the filled-data filter and `data::writeToStack` in the Mixin source.
- [ ] Confirm exact-head common/Fabric/NeoForge/package/security gates.

### Task 5: Validation evidence

**Files:**
- Create: `docs/livingworld/VALIDATION_0.1.20_FILLED_GRAVE_DROP_FIX.md`

**Interfaces:**
- Consumes: installed failure, canonical RED and final exact-head GREEN runs.
- Produces: authoritative repository status and focused installed acceptance procedure.

- [ ] Record the root cause: serialization depended on an already existing self-item drop.
- [ ] Record preserved schema and subsystem boundaries.
- [ ] Require exact-JAR SHA-256 and installed tests for Silk Touch, ordinary tool, wrong tool, empty-grave control, break/place/reopen, restart and duplicate prevention.
- [ ] Keep installed acceptance `PENDING` until the exact candidate passes.

### Task 6: Review and integration

- [ ] Confirm runtime diff is limited to one pure policy, one narrow Mixin and its registration.
- [ ] Confirm no unresolved review threads.
- [ ] Require successful VillAIgence CI, Java PR CI, supply-chain verification and repository security policy on the final documented head.
- [ ] Merge PR only after all automated evidence is fresh; do not claim the installed defect fixed before live acceptance.