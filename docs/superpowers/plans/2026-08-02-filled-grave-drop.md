# Filled Grave Drop Safety Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent a filled MCA tombstone from destroying its stored body and inventory when the underlying loot path does not produce the tombstone block item, including the installed Silk Touch failure.

**Architecture:** Keep vanilla/datapack loot evaluation authoritative for ordinary drops. For a filled tombstone only, pass the evaluated list through a loader-independent policy that reuses the first existing tombstone item or appends one fallback tombstone item, then serializes the block-entity body into that item. Empty tombstones, remains naming, other loot entries, persistence schemas and placement restoration remain unchanged.

**Tech Stack:** Java 21, Minecraft 1.21.1 Mojang mappings, JUnit 5, Fabric Loom, NeoForge, Gradle package verification.

## Global Constraints

- Base branch is `1.21.1` at `ff4df17a086b6f904f7efda3abc77f8a135fa8ed`.
- Preserve the internal mod ID `mca` and package root `net.conczin.mca`.
- Do not change tombstone block-entity NBT or item component schemas.
- Do not change empty-tombstone loot behavior.
- Do not duplicate a tombstone item when the loot list already contains one.
- Preserve all unrelated loot entries and remains-name decoration.
- Do not change water navigation, AI providers, Chat/STT/TTS, memory, packets, operator lore or release version.
- Require common tests, Fabric build/package verification, NeoForge build, supply-chain verification and repository security policy.
- Treat installed Silk Touch acceptance as pending until an exact candidate JAR is tested.

---

### Task 1: Establish the loader-independent RED contract

**Files:**
- Create: `common/src/test/java/net/conczin/mca/block/TombstoneDropPolicyTest.java`

**Interfaces:**
- Consumes: planned `TombstoneDropPolicy.ensurePreservedDrop(List<T>, Predicate<T>, Supplier<T>, Consumer<T>)`.
- Produces: executable contracts for reuse, fallback creation, unrelated-drop preservation and exactly-once serialization.

- [ ] **Step 1: Write the failing tests**

Create tests with a small mutable fake-drop record and verify:

```java
List<FakeDrop> result = TombstoneDropPolicy.ensurePreservedDrop(
        List.of(remains),
        FakeDrop::tombstone,
        () -> created,
        drop -> drop.preserved = true
);
assertEquals(List.of(remains, created), result);
assertTrue(created.preserved);
```

Also verify an existing tombstone is reused without a duplicate, unrelated entries retain order, the input list is not mutated, and the preservation callback runs once.

- [ ] **Step 2: Run the RED gate**

Run:

```bash
./gradlew :common:test --tests net.conczin.mca.block.TombstoneDropPolicyTest --no-daemon
```

Expected: compile-test failure because `TombstoneDropPolicy` does not exist.

- [ ] **Step 3: Commit the canonical RED boundary**

```bash
git add common/src/test/java/net/conczin/mca/block/TombstoneDropPolicyTest.java
git commit -m "test: define filled tombstone drop safety contract"
```

### Task 2: Implement minimal filled-drop preservation

**Files:**
- Create: `common/src/main/java/net/conczin/mca/block/TombstoneDropPolicy.java`
- Modify: `common/src/main/java/net/conczin/mca/block/TombstoneBlock.java`

**Interfaces:**
- Consumes: evaluated `List<ItemStack>`, predicate for the current tombstone item, fallback `ItemStack` supplier and `Data::writeToStack` callback.
- Produces: a new ordered mutable result list containing at least one serialized tombstone item for filled tombstones.

- [ ] **Step 1: Implement the pure policy**

```java
public static <T> List<T> ensurePreservedDrop(
        List<T> drops,
        Predicate<T> isTombstone,
        Supplier<T> fallback,
        Consumer<T> preserve
) {
    List<T> result = new ArrayList<>(drops);
    T target = result.stream().filter(isTombstone).findFirst().orElseGet(() -> {
        T created = fallback.get();
        result.add(created);
        return created;
    });
    preserve.accept(target);
    return result;
}
```

Reject null collaborators with `Objects.requireNonNull` so malformed internal calls fail at the source.

- [ ] **Step 2: Integrate only for filled tombstones**

After existing remains-name decoration, replace the conditional stream mutation with:

```java
if (data.isPresent()) {
    stacks = TombstoneDropPolicy.ensurePreservedDrop(
            stacks,
            stack -> stack.getItem() == asItem(),
            () -> new ItemStack(asItem()),
            data.get()::writeToStack
    );
}
```

Do not invoke the policy when no stored entity exists.

- [ ] **Step 3: Run focused and loader builds**

```bash
./gradlew :common:test --tests net.conczin.mca.block.TombstoneDropPolicyTest :fabric:build :neoforge:build --no-daemon
```

Expected: all tasks succeed.

- [ ] **Step 4: Commit the GREEN implementation**

```bash
git add common/src/main/java/net/conczin/mca/block/TombstoneDropPolicy.java common/src/main/java/net/conczin/mca/block/TombstoneBlock.java
git commit -m "fix: preserve filled tombstone when loot omits block"
```

### Task 3: Enforce the distributable package boundary

**Files:**
- Modify: `fabric/build.gradle`

**Interfaces:**
- Consumes: remapped Fabric JAR produced by `remapJar`.
- Produces: a package failure if the tombstone class loses the fallback policy call or the policy class is omitted.

- [ ] **Step 1: Extend `verifyFabricRefmap`**

Require both class entries:

```text
net/conczin/mca/block/TombstoneDropPolicy.class
net/conczin/mca/block/TombstoneBlock.class
```

Inspect `TombstoneBlock.class` constants and require references to:

```text
net/conczin/mca/block/TombstoneDropPolicy
ensurePreservedDrop
```

- [ ] **Step 2: Run the complete automated gate**

```bash
./gradlew :common:test :fabric:check :neoforge:build --no-daemon
python3 scripts/ci/repository_security_policy.py --check
```

Expected: zero test failures, both loader builds succeed, remapped-JAR verification succeeds and repository security policy succeeds.

- [ ] **Step 3: Commit package enforcement**

```bash
git add fabric/build.gradle
git commit -m "build: enforce filled tombstone drop preservation"
```

### Task 4: Record evidence and installed acceptance

**Files:**
- Create: `docs/livingworld/VALIDATION_0.1.20_FILLED_GRAVE_DROP_FIX.md`

**Interfaces:**
- Consumes: canonical RED run, final exact-head GREEN runs and installed failure evidence.
- Produces: authoritative implementation status and focused operator acceptance procedure.

- [ ] **Step 1: Document root cause and preserved boundaries**

Record that `TombstoneBlock.getDrops()` serialized body data only when `super.getDrops()` already contained `asItem()`, leaving an empty loot result destructive.

- [ ] **Step 2: Define focused installed scenarios**

Require exact-JAR SHA-256 and test:

```text
filled grave + Silk Touch pickaxe
filled grave + ordinary valid tool
filled grave + wrong tool
empty grave control
break/place/reopen round trip
restart after re-placement
inventory and deceased identity equality
no duplicate grave item
```

- [ ] **Step 3: Run fresh exact-head CI and commit evidence**

Do not mark the live defect fixed. Repository implementation may be `PASS`; installed acceptance remains `PENDING` until the exact candidate succeeds.

```bash
git add docs/livingworld/VALIDATION_0.1.20_FILLED_GRAVE_DROP_FIX.md
git commit -m "docs: validate filled tombstone drop fix"
```

### Task 5: Review and integration

**Files:**
- Review all changed files against `1.21.1`.

- [ ] **Step 1: Confirm scope**

Expected runtime diff: one pure policy and one narrow `getDrops()` integration. No schema, provider, navigation, version or dependency changes.

- [ ] **Step 2: Confirm exact-head checks**

Require successful VillAIgence CI, Java PR CI, supply-chain verification and repository security policy.

- [ ] **Step 3: Merge the isolated PR**

Merge only after no unresolved review threads remain. Preserve `installed acceptance: PENDING` in the PR and validation document.