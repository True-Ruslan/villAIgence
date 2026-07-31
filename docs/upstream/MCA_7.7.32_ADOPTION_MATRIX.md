# MCA 7.7.32 Upstream Adoption Matrix

> Canonical commit-level classification for selectively integrating `Luke100000/minecraft-comes-alive` changes after MCA `7.7.22` into VillAIgence.

## Audit snapshot

```text
Audit date:              2026-07-31
VillAIgence repository:  True-Ruslan/villAIgence
VillAIgence branch:      1.21.1
VillAIgence base SHA:    521568f903078b91dd5817cdc9a551bd2392e663
Upstream repository:     Luke100000/minecraft-comes-alive
Upstream branch:         1.21.1
Upstream target SHA:     c3f92f1f7d6f745ab885dcfed350b4e60e1b8cbc
Common ancestor:         a3de832505fcc6a9c4649bfbc0260beb6f0740c4
Common ancestor release: MCA 7.7.22
Upstream-only commits:   53
Fork-only commits:       443
```

The branches are substantially diverged. The accepted strategy is **subsystem final-state patch porting**, not a whole-branch merge and not blind sequential cherry-picking.

## Decision vocabulary

| Decision | Meaning |
|---|---|
| `ADOPT` | The behavior is wanted and sufficiently isolated to port with focused tests. |
| `ADAPT` | The behavior is wanted, but must be manually reconciled with VillAIgence architecture. |
| `SPLIT` | The commit mixes independent subsystems; only separately reviewed portions may be ported. |
| `DEFER` | Potentially useful, but excluded from the first core synchronization train. |
| `BLOCKED` | Depends on an external change or unresolved contract and cannot be imported safely yet. |
| `REJECT` | Must not be imported into VillAIgence under the present architecture/security policy. |
| `RECORD` | Changelog, merge or metadata commit; retain provenance but import no runtime code directly. |

## Target work packages

```text
S0  Synchronization manifest and guardrails
S1  Tombstone item/block-entity data integrity
S2  UUID-preserving villager ↔ zombie conversion
S3  HOME POI and occupied-bed correctness
S4  Water/collision navigation foundation
S5  Ladder/climbable navigation
S6  Pathfinding scheduling and progress watchdog integration
S7  Graveyard and mourning behavior
S8  Isolated gameplay and compatibility fixes
S9  Operator-authored lore persistence
S10 Context Editor UI/networking integration
S11 Generated persistent personality profile — milestone 0.3, not sync release
S12 Destiny/modded-village support
S13 Skin Library/auth/paging fixes
S14 EMF and rendering compatibility
S15 Gifts and relationship-item compatibility
```

## Commit-by-commit classification

| # | Upstream SHA | Upstream message | Decision | Target | Rationale / required treatment |
|---:|---|---|---|---|---|
| 1 | `c3f92f1f7d6f745ab885dcfed350b4e60e1b8cbc` | Missing import | `ADAPT` | S2 | Final compilation correction associated with the conversion work. Consume only as part of the final S2 state. |
| 2 | `af7e3d63a462f522a1a8579eb244d87d27e058f5` | 7.7.32 changelog.md | `RECORD` | S0 | Provenance only. VillAIgence maintains its own changelog and release numbering. |
| 3 | `4ee4741c861a3a5b5561246dcc99005c41f45160` | 7.7.32 changelog.md / graveyard data-loss fix | `ADOPT` | S1 | Corrects tombstone item storage to `BLOCK_ENTITY_DATA` and proper block-entity serialization. Add legacy-read compatibility and round-trip tests. |
| 4 | `ac7a8588d18fcc218dab06573140c1cc0ff0f664` | 7.7.31 changelog.md | `RECORD` | S0 | Provenance only. |
| 5 | `21d723792d26d33aed12fafbbfb0d07749c6851e` | Address comments | `ADAPT` | S2, S10 | Follow-up corrections belong to the final conversion/context-editor states, not an independent cherry-pick. |
| 6 | `edd6f24c97c827dd47049fdedf460ad1b8ab00b2` | Improved Villager <-> Zombie conversion flow | `ADAPT` | S2 | High-priority identity fix. Reconcile with world-local memory, relationship and voice records keyed by UUID. |
| 7 | `4bf2e70f846f0a4bb57116339c248968c18bb7fd` | make ChatAI context editing target stable and persist correctly | `ADAPT` | S9, S10 | Stable target identifiers are valuable. Global-config persistence and prompt authority semantics must be replaced with world-local operator lore. |
| 8 | `96dfb61e910f1a3cf374ddae35c38b63eea13d93` | tuned values | `DEFER` | S11 | Tuning relates to generated character backgrounds. Persistent personality requires a separate milestone-0.3 design. |
| 9 | `ecda2566b90aac52faedf5ae296948b10f457bd8` | added prompts | `DEFER` | S11 | Random/generated profile prompts must not be mixed with operator-authored lore or semantic FACT. |
| 10 | `de85f9cd1536315b341f7b9d6934f9361442abf9` | 7.7.31 - mod compat | `SPLIT` | S8, S12, S13 | Inspect each compatibility change independently and import only for supported target modpacks. |
| 11 | `fb58d5acf2a42db1b934d75b9362d58818923a7d` | Address comments | `ADAPT` | S13 | Final-state correction for Skin Library work; review with the complete S13 patch set. |
| 12 | `4f7a3d80db4d2a29528d1755bbada94afae016d5` | Add back copy clipboard btn | `DEFER` | S13 | Optional client UX. Excluded from core correctness release. |
| 13 | `ed64ec06ef49a1b23a9e28d9d874c7480d4fa51b` | new login flow (needs immersive library PR) | `BLOCKED` | S13 | Upstream explicitly declares an external dependency. Do not import until the dependency and authentication contract are audited. |
| 14 | `d761c02977ccecc6c6bef2c8b406ad76b96a7cf4` | fixed blacklist | `DEFER` | S13 | Useful only with the Skin Library train; test against current API behavior. |
| 15 | `4a8f438c81692a968032f45eb120898bc4c9c096` | better profession name fallback | `DEFER` | S13 | Low-risk client fallback; include only with isolated localization/UI coverage. |
| 16 | `df6cb0f0cb4112268e242c892e115cb60b54a8a5` | submission and likes paging | `DEFER` | S13 | Remote-service paging is unrelated to core NPC correctness and requires network/API tests. |
| 17 | `d3d7c3d680b9aadc8e5f64c5df0b7f35eb64e499` | added better auth | `BLOCKED` | S13 | Authentication changes require a separate security review and verified external contract. |
| 18 | `297855f1c58333134d8149b75f76dbb925692743` | 7.7.31 - Simplify | `ADAPT` | S5 | Consume only as part of the final ladder/navigation implementation. |
| 19 | `c70af35f14c87c4d12dcaaa787e42e0345b6492c` | 7.7.31 - Fix ladder oscillation | `ADOPT` | S5 | Required regression fix for stable climb/exit behavior. |
| 20 | `a652878c5dbfac0ebc4bf46fe6c1b9417e28f86f` | 7.7.31 | `ADAPT` | S5, S6 | Contains ladder performance changes and staggered path checks. Merge with VillAIgence progress tracking rather than replacing it. |
| 21 | `6987ad0a5329f68baf979e199a45f99ad070722c` | 7.7.30 | `SPLIT` | S5, S8 | Separate navigation changes from the archer-on-horse crash fix and other gameplay changes. |
| 22 | `9f1aeb3b6242383f6680179be4cc91cabf783459` | 7.7.30 | `ADAPT` | S5 | Intermediate ladder implementation; use final 7.7.32 state only. |
| 23 | `b4ed32501fea20b18bf19d52239306278e01cca0` | 7.7.30 | `SPLIT` | S5, S8 | Do not cherry-pick release aggregation; extract testable changes into their subsystem PRs. |
| 24 | `872f7b397b04e8e8effe5fd08aa6f6e368f53e94` | 7.7.29 / sync destiny locations and tracked data | `DEFER` | S12 | Valuable client/gameplay feature, but not part of the core data-integrity release. |
| 25 | `4e32d9ed6f7b3a7f7f52fe9f1542c2d8f764ff43` | long names | `DEFER` | S12, S13 | UI/name handling should be validated with the screen that consumes it. |
| 26 | `608ce5a18cb3be83e7c73ae7354e735bbeb33a7e` | synced supporters | `REJECT` | — | Dynamic supporter metadata is unrelated to VillAIgence synchronization and creates noisy diffs. |
| 27 | `2b60684ecd7a4739cbb925bb634fac438d514b9d` | updated scripts | `REJECT` | — | VillAIgence has an audited script inventory and supply-chain policy. Upstream scripts may not bypass it. |
| 28 | `0691c82d27e9255fafc7c0300190b23a4f667793` | stop villagers crowding around occupied beds | `ADOPT` | S3 | Correct HOME POI claim/reclaim logic. Add multi-NPC and restart tests. |
| 29 | `b239c3fbe69c83f05b25685c2c50f2f4e52c8e8a` | Fix renderer | `DEFER` | S14 | Include only in the isolated rendering train and full client matrix. |
| 30 | `c935df1863182e8071c7c777dc894b7d0114b8e3` | Add ladder pathfinding | `ADAPT` | S5 | Primarily release/changelog and class-tweaker cleanup around the ladder implementation. Validate final access-widener needs. |
| 31 | `c65806b469fbea79d9d7033f7b114796bd1a3b13` | Add ladder pathfinding | `ADAPT` | S5 | Core climbable graph/motion implementation. Preserve VillAIgence collision and progress-watchdog guarantees. |
| 32 | `b8fcc4ed673e8755dc8ae601f44ffbb227ad286f` | 7.7.26 changelog | `RECORD` | S0 | Provenance only. |
| 33 | `792a1cfe801c985915f570e59f5f2293ef205f29` | Move to InteractionResult Mojang API | `ADOPT` | S8 | API-correctness modernization. Port separately with relationship-item interaction tests. |
| 34 | `16b126935c323cbca5ab1fe099feb53dd397405d` | changelog | `RECORD` | S0 | Provenance only. |
| 35 | `9fd2c2b16c4b6aa7061a955b8b55e6ab53c7fb9f` | merge EMF feature into 1.21.1 | `DEFER` | S14 | Large client/mixin merge. Import final state only after dedicated-server and client compatibility validation. |
| 36 | `8faf68980f8fc10e7d4cd99ffbeb447997966576` | merge gifts feature into 1.21.1 | `DEFER` | S15 | Feature merge must be decomposed and tested independently. |
| 37 | `b53b8cca1c1507652ea7e876974f03ba3356e673` | Bouquets can be treated as gifts | `DEFER` | S15 | Low-priority gameplay enhancement after core synchronization. |
| 38 | `c6c3136148fdf82e62b5a622cd336190e595f981` | changelog.md | `RECORD` | S0 | Provenance only. |
| 39 | `75d94549a56cf6fffea6deb20b7fd837eabac01c` | skip empty textures | `ADOPT` | S14 | Small defensive client fix, but ship only with rendering/client validation. |
| 40 | `0f092bd76d26274ac8d45cf8aa9b3421089cd562` | port 1.20.1 and fix bugs | `SPLIT` | S7, S8, S14 | Large mixed commit. Never cherry-pick. Extract mourning, editor, eye/rendering and other fixes into independently reviewed patches. |
| 41 | `71cbb13f7a2cf406a86f15ea6a33a2dfd5911b8f` | improve fishing logic and AquaCulture compatibility | `ADOPT` | S8 | Isolated compatibility candidate; require reproduction and mod-present/mod-absent tests. |
| 42 | `32fd63a6401de48022ffd367412a0db259fd03c0` | update changelog.md | `RECORD` | S0 | Provenance only. |
| 43 | `e48f5dfdef00e8c7745c6900554e93ec2d1e23fa` | Fix CF | `RECORD` | S0 | Packaging/publication correction; VillAIgence retains its own release workflow. |
| 44 | `a9818b39087d3b4820b657ce42150da54f8e4ac9` | fix crash | `ADOPT` | S13 | Defensive image copy prevents shared `NativeImage` ownership corruption. Include with Skin Library crash regression coverage. |
| 45 | `be20101d7b10f970cb3a58533094862bc121dda9` | fix root cause of library corruption | `ADOPT` | S13 | Clears incompatible preview hair state before clothing preview. Include with library-state isolation tests. |
| 46 | `29d26d9db5e9cb9ad2e6134f680a7e7e50cdbbc8` | Simplify last commit | `ADAPT` | S4 | Consume only through the final water/collision navigation state. |
| 47 | `b4c6381fd3e206b5c3c2bedff9b63f06186cea19` | collision, door-goal and modded-water fixes | `ADAPT` | S4 | High-value navigation foundation. Retain actual VillAIgence entity-clearance semantics unless fixed `1×2` clearance is proven superior. |
| 48 | `e4b43095494ac41252c4653a5fd7f85055f81988` | Fix EMF rotational bug | `ADOPT` | S14 | Required if EMF support is shipped; validate all model layers and poses. |
| 49 | `452323cb7a62d12682319ac94f619f4911f8c702` | Simplify rendering | `ADAPT` | S14 | Intermediate client refactor; consume final rendering state only. |
| 50 | `6e25c8996010a92600ca379353146eb6ef40baac` | Simplify rendering | `ADAPT` | S14 | Intermediate client refactor; consume final rendering state only. |
| 51 | `1368d0d4f1c7209638b1fb3a039cca6fb9d67939` | Simplify rendering | `ADAPT` | S14 | Intermediate client refactor; consume final rendering state only. |
| 52 | `bda4c3e7d7bf3c293ac33af9c468a63331a601b0` | fix player model rendering | `ADAPT` | S14 | Part of final EMF/model compatibility state. |
| 53 | `d89655c0610f33280ab53c55d2adaccf015d8f8b` | 7.7.23-beta.1 | `ADAPT` | S14 | Initial EMF compatibility aggregation. Do not import without all subsequent corrections. |

## Non-negotiable exclusions

The following upstream paths may not be replaced wholesale during this synchronization:

```text
common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java
.github/workflows/**
scripts/**
gradle/wrapper/**
gradle/verification-metadata.xml
**/gradle.lockfile
docs/security/APPROVED_SCRIPT_INVENTORY.json
```

Specific rules:

1. The upstream `OpenAIChatAI` transport/parser must never replace VillAIgence endpoint binding, redirect denial, bounded response reading, retry policy, structured parsing or diagnostics.
2. Upstream workflows and scripts are untrusted inputs until a dedicated supply-chain review explicitly accepts them.
3. Changelog and release-number commits are provenance, not merge instructions.
4. Generated NPC biographies remain outside the synchronization release and belong to milestone `0.3 Personality`.
5. Operator-authored lore must never be stored as `SYSTEM_OBSERVED` semantic FACT.

## Release allocation

```text
0.1.16+1.21.1 candidate
  S1 Tombstone integrity
  S2 UUID-preserving conversion
  S3 HOME POI
  S4 Water/collision navigation
  S5 Ladder navigation
  S6 Pathfinding scheduling/watchdog

0.1.17+1.21.1 candidate
  S9 Operator lore persistence
  S10 Context Editor UI/networking

Later optional trains
  S7 Mourning/graveyard behavior
  S8 Gameplay/mod compatibility
  S12 Destiny/modded villages
  S13 Skin Library
  S14 EMF/rendering
  S15 Gifts
```

## Maintenance rule

Before implementing any package:

1. refresh the upstream target SHA;
2. compare the affected final upstream files against the target SHA recorded here;
3. update this matrix if new upstream commits supersede the planned behavior;
4. port the final subsystem state manually;
5. retain the original upstream SHAs in the PR body;
6. run the package-specific automated and live acceptance gates;
7. update this matrix from `planned` to the merged PR and validation evidence.
