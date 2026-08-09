# Deterministic rumor fallibility — implementation plan

Base: `1.21.1` at `8329b886e792a368de1b92caf8904ab52bae3558`

Branch: `feat/deterministic-rumor-fallibility`

Design: `docs/superpowers/specs/2026-08-09-deterministic-rumor-fallibility-design.md`

## Protected boundaries

Do not change:

- FACT/BELIEF truth boundary;
- `SemanticMemoryEntry.confidence` semantics;
- `npc-knowledge-transfer-v2` deterministic evidence identity;
- transfer hop limit/cycle/no-fallback rules;
- persistence/config/provider schemas;
- Semantic `32 / 24+8 / 6` bounds;
- disagreement bound `4`;
- contradiction winner-neutrality;
- installed `0.2.0+1.21.1` evidence claims.

## Task 1 — pure fallibility model

RED first:

- first hop => `RESOLVED`, distance 1, transformations 0;
- eight hops => distance 8;
- invalid/unavailable provenance cannot fabricate a resolved state;
- state exposes no truth score and does not mutate Semantic confidence.

GREEN:

- add minimal immutable `RumorFallibilityState`;
- add pure `RumorFallibilityPolicy` deriving state only from valid retained `KnowledgeTransferProvenance`.

Regression: full common tests.

## Task 2 — retained-source resolver

RED first:

- valid `BELIEF/NPC_TOLD` + canonical direct evidence => resolved state;
- canonical branch selection matches existing provenance resolver;
- missing/forgotten/malformed direct evidence => explicit `UNRESOLVED`;
- FACT / PLAYER_TOLD / INFERRED are not rumor fallibility candidates.

GREEN:

- add read-only resolver reusing `KnowledgeTransferProvenanceResolver` and `MemoryEventStore`;
- no writes, repairs or fallback reconstruction.

Regression: full common tests.

## Task 3 — inline Semantic prompt annotation

RED first:

- selected eligible `NPC_TOLD` line receives fallibility annotation;
- first/multi-hop exact distance is rendered deterministically;
- unresolved retained rumor renders `sourcePath=UNRESOLVED` without invented distance;
- FACT/PLAYER_TOLD/INFERRED line output stays byte-compatible;
- no extra Semantic result slots or duplicate claim prose.

GREEN:

- extend formatter with a narrow fallibility-aware overload;
- preserve existing formatter APIs;
- resolve metadata only after current player eligibility, bounded candidate selection and ranking.

Regression: full common tests and existing prompt wiring tests.

## Task 4 — preservation simulation

Tests only unless they expose a real production defect:

- 8-hop rumor chain;
- multiple lineages/corroboration;
- >200 Semantic + >200 episodic noise;
- foreign-player/private noise;
- direct evidence forgetting while Semantic rumor survives;
- fresh-root reload equality;
- current observed FACT precedence;
- contradiction layer remains winner-neutral;
- injection-safe statements retain existing escaping.

## Task 5 — delivery

- update root `CHANGELOG.md` `[Unreleased]`;
- create exact TDD evidence ledger with observed RED/GREEN heads/runs;
- independent base→head review;
- verify no persistence/config/provider/release identity drift;
- freeze immutable final head;
- require exact-head:
  - Repository security policy;
  - VillAIgence CI;
  - Production Soak;
  - GitHub Release dry-run with publication skipped;
- squash merge only after all gates are green;
- follow with separate docs-only `PROJECT_STATE.md` / `ROADMAP.md` reconciliation.

## Exit criterion

A retained `BELIEF/NPC_TOLD` can expose bounded deterministic process fallibility in the existing Semantic prompt slot from retained canonical provenance, including explicit unresolved-source state, while truth authority, confidence, persistence, privacy and all existing hard bounds remain unchanged.
