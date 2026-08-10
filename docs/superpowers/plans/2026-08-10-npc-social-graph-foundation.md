# NPC Social Graph Foundation — Implementation Plan

Date: 2026-08-10
Base: `05e48164b633a4989f95fe3b2ef12a1c7b33f2ea`
Branch: `feat/npc-social-graph-foundation`

## Delivery strategy

Strict staged TDD. No production behavior before an observed feature-missing RED.

## Stage 1 — pure NPC social state policy

RED first:
- `NpcSocialState` clamps every dimension to `[-100,+100]`;
- `NpcSocialDelta` sanitizes each component to caller-provided absolute mutation limit;
- applying a delta produces bounded independent dimensions;
- neutral state is explicit.

Minimal GREEN:
- add domain records only; no persistence.

## Stage 2 — directed store identity and mutation semantics

RED first:
- A→B and B→A are independent;
- self-edge returns `INVALID_PAIR`;
- null/malformed input cannot create state;
- no-op returns `NO_CHANGE`;
- non-neutral mutation returns exact before/after `APPLIED` result;
- neutral result removes persistent edge.

Minimal GREEN:
- add `NpcSocialGraphStore` on `npc-social-graph.json` format v1;
- use existing `JsonStoreRecovery` atomic codec;
- store only non-neutral edges.

## Stage 3 — hard outgoing-edge bound

RED first:
- first 64 non-neutral targets for one source are accepted;
- target 65 returns `CAPACITY_REACHED`;
- no retained edge is evicted or changed by overflow;
- another source still has independent capacity;
- neutralizing one retained edge frees one slot.

Minimal GREEN:
- enforce bound before creating a new non-neutral edge;
- existing-edge updates bypass capacity admission because they do not expand graph cardinality.

## Stage 4 — load/restart sanitation

RED first:
- fresh-root reload reproduces valid directed states exactly;
- malformed keys, self-edges, null entries and neutral entries do not become live edges;
- reconstructed values are clamped.

Minimal GREEN:
- canonical key parsing and deterministic sanitation on load.

## Stage 5 — production persistence/recovery automation

RED first in Python contracts:
- canonical persistent-store list must include `npc-social-graph.json`;
- recovery matrix must cover each canonical store exactly once;
- workflow assertions must require six recovery cases.

Minimal GREEN:
- add `npc-social-graph.json` to `CANONICAL_PERSISTENT_STORES`;
- add one incompatible-schema/corrupt recovery case;
- update exact recovery-case assertions from five to six anywhere they are contractual;
- ensure production recovery fixture creates/opens the graph store so the baseline file exists.

## Stage 6 — preservation / pressure

Tests only unless a real defect appears:
- 10+ source NPCs under 64-edge pressure;
- directed symmetry is never assumed;
- existing `relationships.json` remains byte-independent;
- no Semantic/Memory2/provider/config coupling;
- fresh-root copy/reload equality;
- recovery of graph corruption leaves sibling stores unchanged.

## Stage 7 — governance and delivery

- update root `CHANGELOG.md` `[Unreleased]` in the runtime PR;
- record staged RED/GREEN evidence under `docs/superpowers/evidence/`;
- base→head review for P0/P1/P2 and scope drift;
- freeze exact feature head;
- require exact-head security, main CI, production soak and release dry-run;
- release publication remains skipped;
- squash merge only after gates pass;
- follow with docs-only `PROJECT_STATE.md` / `ROADMAP.md` reconciliation.

## Local/manual testing policy

No local graphical/manual server acceptance is required for this foundation if the exact production startup/restart/recovery matrix is green. Manual local testing becomes justified when a later 0.3 slice makes personality/NPC social state observable in dialogue, behavior or commands, or when an exact release candidate is prepared.