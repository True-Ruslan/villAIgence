# Bounded transformed-claim representation — implementation plan

Base: `48c19d3ae520a14fe1448f93c3ef782269733190`

## Protected boundaries

Do not change:

- FACT admission authority;
- existing `npc-knowledge-transfer-v2` deterministic identity namespace;
- max 8 provenance hops;
- `32 / 24+8 / 6` Semantic retrieval bounds;
- max 4 contradiction prompt relations;
- provider request count/schema;
- public config;
- release/tag/version identity;
- legacy `memory.json` policy.

## Task 1 — pure transformation state and deterministic omission

RED first:

- structured transformation snapshot validation;
- exact one-step maximum;
- exact hop binding;
- deterministic `OMIT_TRAILING_SENTENCE` transformation;
- reject single-sentence/non-applicable input;
- reject fabricated/reordered/substituted transformed text;
- old no-transformation path remains valid.

GREEN:

- `KnowledgeTransferTransformation`;
- `KnowledgeTransferTransformationPolicy`.

## Task 2 — MemoryEvent evidence and transfer adapter integrity

RED first:

- optional transformation survives Gson persistence/reload;
- old MemoryEvent constructors remain source-compatible;
- NPC_TOLD evidence requires statement to match provenance origin when no transform;
- transformed evidence requires statement to match validated transformation current statement;
- same transfer identity with conflicting payload cannot be treated as a valid replay.

GREEN:

- additive optional MemoryEvent field;
- transformation-aware `NpcToldDialogueAdapter` / evidence policy overloads.

## Task 3 — canonical provenance resolver and downstream propagation

RED first:

- transformed listener Semantic claim resolves canonical provenance through direct evidence;
- later unchanged transfer copies the exact transformation snapshot;
- origin provenance statement remains unchanged;
- canonical branch ordering remains newest-valid then evidence UUID;
- transformed source without retained direct evidence remains provenance-unavailable.

GREEN:

- transformation-aware content matching;
- resolver returns retained transformation snapshot;
- provenance append validates current transformed content without redefining v2 origin.

## Task 4 — server-owned transformed transfer lifecycle

Behavioral RED first:

- multi-sentence source transformed once into listener BELIEF/NPC_TOLD;
- single sentence → `TRANSFORMATION_NOT_APPLICABLE`;
- second transform in same lineage → `TRANSFORMATION_LIMIT_REACHED`;
- exact retry byte-idempotent;
- transformed vs plain same-identity retry rejected;
- no confidence/FACT promotion.

GREEN:

- explicit server API for `transferWithTrailingSentenceOmission`;
- minimal lifecycle integration;
- existing `transfer` remains byte-compatible for non-transformed paths.

## Task 5 — fallibility/prompt integration

RED first:

- resolved transformed rumor renders `transformationsUsed=1`;
- resolved ordinary rumor remains `0`;
- missing direct evidence renders `transformationsUsed=UNKNOWN`, never fabricated `0`;
- statement text cannot forge transform/fallibility metadata;
- no new prompt slot/ranking effect.

GREEN:

- extend `RumorFallibilityState` validation;
- derive count from canonical retained transformation snapshot;
- fail-closed unresolved rendering.

## Task 6 — preservation simulation

Tests only unless a real defect is exposed:

- exact 8-hop lineage with one transform at an interior hop;
- transformation snapshot copied unchanged to hop 8;
- second transform rejected;
- >200 Semantic + >200 episodic pressure;
- two players / privacy-before-limit;
- fresh-root reload equality;
- direct evidence forgetting → provenance unavailable / fallibility unknown;
- prompt injection strings in transformed/remaining source text;
- current observed FACT authority unchanged;
- contradiction no-winner behavior unchanged;
- no provider/config changes.

## Task 7 — delivery

- update root `CHANGELOG.md` under `[Unreleased]`;
- write TDD evidence ledger;
- independent base→head review;
- fix any P0/P1/P2 through explicit RED→GREEN when behavior-related;
- freeze exact head;
- require exact-head repository security, full VillAIgence CI, Production Soak, GitHub Release dry-run;
- release publication must remain skipped;
- squash merge product PR with expected head SHA;
- separate docs-only `PROJECT_STATE.md` / `ROADMAP.md` reconciliation PR and gates.

## Release boundary

This product slice is intended to complete the planned Memory 2.0 development checkpoint before preparation of `0.2.1+1.21.1`. This PR itself does not publish that release.