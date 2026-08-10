# Relationship/Trust Social Epistemology — Implementation Plan

**Goal:** Add a bounded server-owned trust adjustment for selected player-origin Semantic BELIEFs without mutating stored confidence, ranking, FACT authority, contradiction state or settlement routing.

**Base:** `d259572124f0a01b400e189e359c960e971cdae6`

**Branch:** `feat/relationship-trust-social-epistemology`

## Hard invariants

```text
trust input                       LivingWorldRelationshipState.trust() only
trust range                       -100..100
trust delta                       trust / 10
trust delta range                 -10..10
effective BELIEF confidence       clamp(base + delta, 0, 100)
persisted Semantic confidence     unchanged
Semantic ranking/retention        unchanged
FACT treatment                    unchanged / ineligible
contradiction winner              none
settlement routing                unchanged
provider calls                    unchanged
persistence schema/version        unchanged
```

## Task 1 — pure social epistemic policy

Production target:
- `SocialEpistemicState.java`
- `SocialEpistemicPolicy.java`

Test target:
- `SocialEpistemicPolicyTest.java`

TDD:
- [ ] tests-only compile RED for absent policy/state API;
- [ ] minimal GREEN for trust-only arithmetic;
- [ ] assert ±10 hard bound and 0..100 effective clamp;
- [ ] FACT, null state and non-player-source inputs cannot create social epistemic state;
- [ ] respect/fear/affinity differences do not change trust arithmetic.

## Task 2 — exact source-player resolution

Production target:
- `SocialEpistemicSourceResolver.java`

Test target:
- `SocialEpistemicSourceResolverTest.java`

Direct PLAYER_TOLD:
- [ ] tests-only compile RED;
- [ ] require retained `DIALOGUE / PLAYER_TOLD` source evidence;
- [ ] unique non-owner participant is the source player;
- [ ] every retained supporting source event must resolve to the same player;
- [ ] missing/malformed/conflicting source evidence -> unresolved.

NPC_TOLD player-origin rumor:
- [ ] use existing `KnowledgeTransferProvenanceResolver`;
- [ ] require origin `BELIEF / PLAYER_TOLD`;
- [ ] reread exact origin Semantic entry;
- [ ] require `KnowledgeTransferProvenancePolicy.originMatchesSource(...)`;
- [ ] resolve original player only through retained direct source DIALOGUE evidence;
- [ ] forgotten origin entry/source evidence -> unresolved, never infer from prose/scope.

Non-player origins:
- [ ] FACT/SYSTEM_OBSERVED origin -> no social trust state;
- [ ] BELIEF/INFERRED origin -> no social trust state.

## Task 3 — derived resolver combining evidence + relationship

Production target:
- `SocialEpistemicResolver.java`

Test target:
- `SocialEpistemicResolverTest.java`

TDD:
- [ ] tests-only RED;
- [ ] exact source player -> `LivingWorldRelationshipStore.get(listenerNpc, sourcePlayer)`;
- [ ] only trust contributes;
- [ ] derived state does not mutate Semantic entry or relationship state;
- [ ] changing persisted trust changes subsequent derived state without rewriting semantic memory.

## Task 4 — formatter/provider integration after ranking

Production target:
- `SemanticMemoryContextFormatter.java`
- `SemanticMemoryContextProvider.java`

Test target:
- `SocialEpistemicContextTest.java`

TDD:
- [ ] RED: selected player-origin BELIEF lacks social annotation;
- [ ] GREEN: formatter appends `socialEpistemics={trustDelta=..., effectiveBeliefConfidence=...}` before statement;
- [ ] base `confidence=` remains persisted value unchanged;
- [ ] source player UUID is never rendered;
- [ ] conditional prompt guidance appears only when validated social metadata exists;
- [ ] provider computes social metadata only for already-ranked max-6 results;
- [ ] high trust cannot pull an otherwise unselected belief into the result set;
- [ ] low trust cannot remove a selected belief.

## Task 5 — preservation / restart / pressure

Test target:
- `SocialEpistemicPreservationTest.java`

Coverage:
- [ ] current observed FACT authority text remains unchanged;
- [ ] FACT lines never receive social metadata;
- [ ] contradiction relations remain live regardless of trust;
- [ ] existing rumor fallibility/transformation metadata still renders alongside social metadata;
- [ ] foreign-player eligibility happens before Semantic allocation and before relationship reads;
- [ ] malformed/missing provenance fails soft to previous line format;
- [ ] fresh-root copy of `memory2.json`, `semantic-memory.json`, `relationships.json` reproduces identical context;
- [ ] many relationship records + Semantic pressure preserve existing 32 candidate / 6 result bounds;
- [ ] no settlement-flow routing behavior changes.

## Task 6 — documentation and changelog

- [ ] add staged TDD evidence under `docs/superpowers/evidence/`;
- [ ] update root `CHANGELOG.md` `[Unreleased]` in the runtime PR;
- [ ] state explicitly that stored confidence/ranking/retention and routing are unchanged;
- [ ] preserve official installed `0.2.0+1.21.1` boundary.

## Task 7 — review and exact-head delivery

- [ ] independent base→head review for P0/P1/P2;
- [ ] inspect source resolution for fabricated-player risk;
- [ ] inspect prompt metadata for truth-promotion ambiguity;
- [ ] inspect privacy-before-allocation and relationship lookup scope;
- [ ] inspect config/persistence/provider/release drift;
- [ ] freeze exact feature SHA;
- [ ] Repository security policy SUCCESS;
- [ ] VillAIgence CI SUCCESS;
- [ ] Production Soak SUCCESS;
- [ ] GitHub Release dry-run SUCCESS;
- [ ] publication job SKIPPED;
- [ ] squash merge product PR;
- [ ] docs-only PROJECT_STATE/ROADMAP reconciliation after merge.

## Expected next canonical step after completion

`Personality + NPC↔NPC Social Graph / 0.3 convergence`.
