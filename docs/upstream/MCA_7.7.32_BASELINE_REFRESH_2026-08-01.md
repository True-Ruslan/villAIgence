# MCA 7.7.32 Selective Synchronization Baseline Refresh

**Refresh date:** 2026-08-01  
**Purpose:** reconcile the approved selective-sync plan with repository changes that landed after the original 2026-07-31 audit and before S1 implementation.

## Current references

```text
VillAIgence repository:  True-Ruslan/villAIgence
VillAIgence branch:      1.21.1
VillAIgence base SHA:    5a67e4f79661e569c9958caa14cc89d998a1b57b
Upstream repository:     Luke100000/minecraft-comes-alive
Upstream branch:         1.21.1
Upstream target SHA:     7ba76165dcf8aa9aa79070bdbac141b65519b3e7
Common ancestor:         a3de832505fcc6a9c4649bfbc0260beb6f0740c4
Common ancestor release: MCA 7.7.22
Upstream-only commits:   54
Fork-only commits:       446
```

The synchronization strategy remains unchanged: subsystem final-state patch ports only; no whole-branch merge and no blind sequential cherry-pick.

## VillAIgence changes since the original audit

```text
PR #70  security acceptance reconciliation / verification-probe fix
PR #71  SEC-004 closure and final Step 1 documentation
PR #69  selective MCA synchronization plan
```

Current security state:

```text
Step 1 Security and supply-chain hardening — complete
SEC-001 through SEC-009 — Closed
installed full server acceptance checkpoint — 0.1.16+1.21.1
final exact-artifact security checkpoint — 0.1.17+1.21.1
```

These changes do not conflict with S1. They strengthen the baseline from which synchronization work starts.

## New upstream commit classification

| Upstream SHA | Message | Decision | Target | Treatment |
|---|---|---|---|---|
| `7ba76165dcf8aa9aa79070bdbac141b65519b3e7` | improve navigation | `ADAPT` | S4 | Use the final navigation state when S4 begins. It restores actual mob-bounding-box clearance, adds an exact partial-collision scan and extends special collision checks to fences, walls and fence gates. This supersedes the earlier fixed `1×2` clearance concern and aligns with the VillAIgence requirement to preserve entity-specific collision semantics. |

## Immediate implementation decision

S1 remains the first synchronization package because it is independent of navigation, AI transport, memory migration and security acceptance code.

```text
S1 source commit: 4ee4741c861a3a5b5561246dcc99005c41f45160
scope: TombstoneBlock item/block-entity data round trip
protected AI/security paths changed: no
persistent VillAIgence JSON schemas changed: no
```

The canonical plan remains in:

```text
docs/upstream/MCA_7.7.32_ADOPTION_MATRIX.md
docs/superpowers/specs/2026-07-31-mca-7.7.32-selective-sync-design.md
docs/superpowers/plans/2026-07-31-mca-7.7.32-selective-sync.md
```
