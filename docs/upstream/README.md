# Upstream synchronization

This directory is the entry point for controlled adoption of changes from `Luke100000/minecraft-comes-alive`.

## Current synchronization program

```text
Program:                 MCA 7.7.32 selective synchronization
Status:                  design and implementation plan approved
Runtime code changed:    no
VillAIgence base SHA:    521568f903078b91dd5817cdc9a551bd2392e663
Upstream target SHA:     c3f92f1f7d6f745ab885dcfed350b4e60e1b8cbc
Common MCA base:         a3de832505fcc6a9c4649bfbc0260beb6f0740c4 (7.7.22)
Upstream-only commits:   53
Fork-only commits:       443
Prerequisite PR #68:     merged
Next implementation:     S1 tombstone data integrity
```

## Canonical documents

1. [`MCA_7.7.32_ADOPTION_MATRIX.md`](MCA_7.7.32_ADOPTION_MATRIX.md) — commit-level classification of all 53 upstream commits.
2. [`../superpowers/specs/2026-07-31-mca-7.7.32-selective-sync-design.md`](../superpowers/specs/2026-07-31-mca-7.7.32-selective-sync-design.md) — approved architecture, boundaries and release design.
3. [`../superpowers/plans/2026-07-31-mca-7.7.32-selective-sync.md`](../superpowers/plans/2026-07-31-mca-7.7.32-selective-sync.md) — executable task-by-task implementation and validation plan.
4. [`../UPSTREAM_ISSUE_AUDIT.md`](../UPSTREAM_ISSUE_AUDIT.md) — issue-level adoption filter for upstream reports and requests.

## Governing rule

VillAIgence does not merge upstream wholesale. Every change is accepted only when it:

- reproduces or provides clear value on the supported Minecraft 1.21.1 stack;
- preserves VillAIgence identity, persistence, authority and security laws;
- is isolated into a reviewable subsystem PR;
- has automated and, where required, real-server acceptance evidence;
- can be rolled back without deleting LivingWorld data.

## Immediate sequence

```text
merge this documentation PR
→ refresh upstream target SHA
→ implement S1 tombstone integrity
→ implement S2 UUID-preserving conversion
→ implement S3 HOME POI correctness
→ implement S4 water/collision navigation
→ implement S5 ladder navigation
→ implement S6 pathfinding scheduling/watchdog integration
→ run and record the core synchronization release checkpoint
```

Operator-authored lore and Context Editor work begins only after the core synchronization checkpoint. Generated personality backgrounds remain deferred to milestone `0.3`.
