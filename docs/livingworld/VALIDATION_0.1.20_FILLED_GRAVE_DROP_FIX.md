# VillAIgence 0.1.20 Filled Grave Drop Safety Fix Validation

## Status

```text
date: 2026-08-02
installed source release: 0.1.20+1.21.1
installed acceptance result: PARTIAL PASS — DESTRUCTIVE GRAVE DEFECT FOUND
observed defect: filled grave broken with Silk Touch disappeared without a block item
repository implementation: PASS
loader-independent regression tests: PASS
Fabric and NeoForge builds: PASS
installed filled-grave acceptance: PENDING
release promotion: PENDING
```

This package addresses only the destructive filled-grave drop defect found during installed acceptance of `0.1.20+1.21.1`.

It does not claim that the installed defect is fixed until a new exact candidate JAR passes the focused procedure below.

---

## Installed failure

The exact official `0.1.20+1.21.1` JAR was tested with a filled MCA grave and a Silk Touch pickaxe.

Expected:

```text
a tombstone item is dropped
the item retains the deceased entity and its stored inventory
placing the item restores the filled grave
```

Actual:

```text
no tombstone item appeared
the grave disappeared
the stored body and inventory were lost
```

This is release-blocking because it converts an ordinary player action into irreversible world-state loss.

Canonical installed evidence:

```text
docs/livingworld/VALIDATION_0.1.20_INSTALLED_ACCEPTANCE.md
```

---

## Root cause

`TombstoneBlock.getDrops()` obtained the evaluated loot list from `super.getDrops()` and then attempted to serialize its block-entity data only into an already existing tombstone item:

```java
stacks.stream()
        .filter(stack -> stack.getItem() == asItem())
        .findFirst()
        .ifPresent(data::writeToStack);
```

That path is safe only when the underlying loot evaluation already returns `asItem()`.

When the evaluated list omits the tombstone block item, the stream is empty:

```text
no target ItemStack exists
Data.writeToStack is never called
no fallback item is produced
the block entity is removed with its stored body
```

The installed Silk Touch failure matches this exact missing-target branch.

---

## Implemented safety contract

### Loader-independent policy

`TombstoneDropPolicy.ensurePreservedDrop(...)` now receives:

```text
evaluated drops
predicate identifying the current tombstone item
fallback tombstone supplier
serialization callback
```

It performs the following deterministic operation:

```text
1. copy the evaluated list
2. locate the first existing tombstone item
3. if absent, append exactly one fallback tombstone item
4. serialize stored body data into that one target
5. return the ordered result
```

Properties established by unit tests:

```text
missing self item -> one fallback is appended
existing self item -> reused without creating a duplicate
unrelated entries -> retained in original order
input list -> not mutated
serialization callback -> exactly one invocation
```

### Narrow runtime hook

`MixinTombstoneBlock` injects at the return of MCA-owned:

```text
TombstoneBlock.getDrops(BlockState, LootParams.Builder)
```

The hook is applied only when:

```text
LootContextParams.BLOCK_ENTITY is TombstoneBlock.Data
Data.hasEntity() is true
```

For an empty grave, the returned loot list is left unchanged.

For a filled grave, the policy reuses the current block item or creates:

```java
new ItemStack(block.asItem())
```

and serializes the body through the existing:

```java
data.writeToStack(itemStack)
```

### Existing data format retained

No new NBT key or item component is introduced.

The fix continues to use the existing tombstone item round-trip:

```text
Data.writeToStack -> BLOCK_ENTITY_DATA
TombstoneBlock.setPlacedBy -> Data.readFromStack
```

Therefore existing filled-grave items and worlds require no migration.

---

## TDD evidence

### Canonical RED

```text
head: 4448f2a062e76645fefa0d67fd499a0c1824a540
VillAIgence CI #1260 / 30751844347: EXPECTED FAILURE
Java Pull Request CI #709 / 30751844358: SUCCESS
Repository security policy #611 / 30751844346: SUCCESS
```

Expected failure boundary:

```text
:common:compileTestJava FAILED
TombstoneDropPolicyTest.java: cannot find symbol TombstoneDropPolicy
```

The failure appeared at all four initial policy call sites and contained no unrelated repository or infrastructure failure.

### Initial GREEN

```text
head: 318da74d440ad6b9c4849a9468f8e63002149cc9
VillAIgence CI #1266 / 30752023680: SUCCESS
Java Pull Request CI #712 / 30752023684: SUCCESS
Repository security policy #617 / 30752023692: SUCCESS
```

The GREEN gate executed:

```text
:common:test
:fabric:build
:neoforge:build
Fabric distributable-package verification
repository security policy
```

### Runtime-wiring hardening

An additional source contract requires all of the following:

```text
mca.mixins.json registers MixinTombstoneBlock
MixinTombstoneBlock calls TombstoneDropPolicy.ensurePreservedDrop
runtime hook filters to TombstoneBlock.Data.hasEntity
runtime hook serializes with data::writeToStack
```

This prevents a future change from leaving the pure policy tested but disconnected from gameplay.

---

## Preserved boundaries

```text
empty grave loot behavior changed: no
remains-name decoration changed: no
tombstone item data schema changed: no
block entity persistence schema changed: no
entity resurrection logic changed: no
graveyard manager schema changed: no
world migration required: no
water navigation changed: no
AI provider/parser/retry changed: no
Chat/STT/TTS/Voice Chat changed: no
memory or relationship persistence changed: no
operator lore or packets changed: no
dependencies or verification metadata changed: no
embedded release version changed: no
```

The runtime behavior change is restricted to the destructive missing-self-item branch of a filled tombstone drop.

---

## Focused installed acceptance

Build one exact candidate JAR from the accepted commit and record its SHA-256 before installation.

### Startup gate

1. Back up the complete world and `<world>/livingworld/` directory.
2. Install the exact candidate JAR on the server.
3. Start without connecting a client.
4. Confirm world load, monitor, TCP `25565` and UDP `24454`.
5. Confirm no Mixin application or injection error involving `MixinTombstoneBlock` or `TombstoneBlock.getDrops`.

### Test body

Use a disposable test NPC with unmistakable identity and inventory:

```text
unique NPC name
known UUID if available
several distinct item types
multiple stack sizes
at least one item with components or custom name
```

Record the inventory before death and grave creation.

### Required scenarios

#### A. Filled grave with Silk Touch

1. Create a filled grave.
2. Break it with the same Silk Touch tool class that reproduced the release defect.
3. Confirm exactly one tombstone block item appears.
4. Confirm unrelated remains drops, if any, are not duplicated or removed.
5. Pick up and place the tombstone item.
6. Confirm the grave is still filled.
7. Confirm deceased identity and full inventory match the recorded source.

#### B. Existing normal self-drop path

1. Break a filled grave using an ordinary valid tool.
2. Confirm there is no duplicate tombstone block item.
3. Place the returned item and verify identity and inventory equality.

#### C. Wrong-tool safety

1. Break a disposable filled grave with a tool that previously causes the underlying loot path to omit the block item.
2. Confirm the safety fallback still produces one preserved tombstone item.
3. Confirm no inventory loss.

This scenario intentionally prioritizes world-state safety over tool-gating for a filled container block.

#### D. Empty-grave control

1. Place an empty tombstone.
2. Break it with Silk Touch and with an ordinary tool.
3. Confirm the fix does not invent stored entity data.
4. Confirm normal empty-grave loot semantics remain unchanged.

#### E. Restart round trip

1. Break a filled grave and place the preserved item.
2. Restart the server.
3. Reopen or inspect the grave.
4. Confirm identity and inventory still match.
5. Break and place it once more after restart.

### Failure conditions

Any of the following fails acceptance:

```text
no tombstone block item
more than one tombstone block item caused by the fallback
placed item becomes empty
NPC identity changes
inventory item or count changes
BLOCK_ENTITY_DATA cannot be read
Mixin startup error
persistent world files become corrupt
```

---

## Acceptance decision

Repository status:

```text
implementation: COMPLETE
automated policy validation: PASS
Fabric/NeoForge build: PASS
runtime-wiring contract: IMPLEMENTED
installed Silk Touch acceptance: PENDING
```

The branch may be merged after fresh exact-head CI. Release promotion remains blocked until a new exact JAR passes both this focused grave test and the separate water-navigation acceptance.