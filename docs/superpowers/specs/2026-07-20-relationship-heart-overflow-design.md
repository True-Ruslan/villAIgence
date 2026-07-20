# Relationship Heart Overflow Fix Design

## Problem

`Memories.modHearts(int)` currently performs `this.hearts += value` using 32-bit signed integer arithmetic. A positive or negative overflow wraps across the integer boundary and can transform a large positive relationship score into a value near `Integer.MIN_VALUE`, matching the class of symptom reported by upstream #977 (`-2.147B` hearts).

## Decision

Preserve the existing unbounded integer relationship model and semantics, but replace wrapping addition with saturating addition:

- normal additions remain unchanged;
- positive overflow clamps to `Integer.MAX_VALUE`;
- negative overflow clamps to `Integer.MIN_VALUE`;
- no arbitrary gameplay heart cap is introduced in this fix.

This is intentionally narrower than redesigning MCA relationship balance.

## Implementation

Add a small pure-Java `RelationshipValueMath.saturatingAdd(int,int)` helper and unit tests. `Memories.modHearts(int)` delegates to that helper before calling the existing `setHearts(...)`, preserving persistence/update behavior.

## Safety

- no NBT format change;
- no migration required;
- existing already-corrupted saves are not silently rewritten;
- no change to thresholds or dialogue logic;
- full Fabric/NeoForge CI required before merge.

## Follow-up

A separate relationship-state milestone may introduce bounded LivingWorld dimensions (`trust/respect/fear/affinity`). That must not be conflated with this compatibility/data-integrity fix.
