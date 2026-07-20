# Relationship Heart Overflow Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent MCA relationship hearts from wrapping across signed integer boundaries during additive updates.

**Architecture:** Keep the current integer heart model and persistence format. Introduce pure saturating arithmetic and use it only in `Memories.modHearts(int)` so normal behavior stays unchanged while overflow cannot invert relationship state.

**Tech Stack:** Java 21, JUnit 5, existing MCA Memories/NBT model.

## Global Constraints

- No NBT/schema migration.
- No new arbitrary heart cap.
- No balance/threshold changes.
- Full Fabric/NeoForge CI before merge.

---

### Task 1: Saturating relationship arithmetic

**Files:**
- Create: `common/src/main/java/net/conczin/mca/entity/ai/RelationshipValueMath.java`
- Test: `common/src/test/java/net/conczin/mca/entity/ai/RelationshipValueMathTest.java`

- [ ] Test normal positive/negative additions.
- [ ] Test positive overflow clamps to `Integer.MAX_VALUE`.
- [ ] Test negative overflow clamps to `Integer.MIN_VALUE`.
- [ ] Implement using `long` intermediate arithmetic.

### Task 2: Integrate into Memories

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/Memories.java`
- Modify: `docs/UPSTREAM_ISSUE_AUDIT.md`

- [ ] Replace wrapping `this.hearts += value` with saturating helper call.
- [ ] Preserve existing `setHearts(...)` update/persistence path.
- [ ] Mark #977 as code-level overflow class confirmed/mitigated; root trigger still monitorable.
- [ ] Run both required CI workflows and merge only when green.
