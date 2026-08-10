# Bounded Contradiction Candidate/Producer — TDD Evidence

Date: 2026-08-10
Base: `92e6eed585d41478033fdebc2591c4745efd7432`
PR: #145
Branch: `feat/bounded-contradiction-producer`

## Contract

The slice adds automatic, server-owned contradiction candidate production after controlled Semantic admission while preserving the existing `SemanticContradictionLifecycle` as persistence authority.

Hard bounds:

```text
MAX_CANDIDATES_PER_ADMISSION = 16
MAX_COMPARISONS_PER_ADMISSION = 8
```

Candidate eligibility is applied before allocation: same NPC owner, exact canonical Semantic scope, distinct logical claim and non-equivalent normalized statement. Existing retained contradiction pairs are suppressed before comparison budget. The initial deterministic classifier recognizes only one standalone `not` / `не` polarity insertion/removal. It never selects a truth winner or mutates claim authority.

## Staged RED / GREEN ledger

### 1. Candidate selector

RED — tests only:

```text
commit: 75fec06d149325ad1c0d5627491fe40fcbc5e12a
VillAIgence CI #2412
run: 31371843855
result: FAILURE as intended
```

Observed: exactly three `cannot find symbol` compile errors for the absent `SemanticContradictionCandidateSelector` API. No production selector existed.

GREEN:

```text
commit: 6effbc50439ed9a77e8c6b972a1e0e2c2126dbf6
VillAIgence CI #2414
run: 31372206127
common + deterministic mock-provider tests: SUCCESS
```

The minimal selector filters owner/scope/equivalence inside `SemanticMemoryStore.getRecentMatching(...)` before the 16-result limit and preserves deterministic newest-first order.

### 2. Conservative opposition classifier

RED — tests only:

```text
commit: 7efbf6825aa2d5da9625935db5d45da297810979
VillAIgence CI #2416
run: 31372443469
result: FAILURE as intended
```

Observed: exactly thirteen compile errors for the absent `SemanticOppositionClassifier` API.

GREEN:

```text
commit: 4e49d7d13dfa3eb90bfce922f033b880130facf6
VillAIgence CI #2418
run: 31372692990
common + deterministic mock-provider tests: SUCCESS
```

Coverage includes English/Russian explicit negation, symmetry, equivalent text, double negation, antonyms, numeric differences, reordering, embedded negation-like text and trailing-sentence omission.

### 3. Bounded producer and duplicate suppression

RED — tests only:

```text
commit: 525a5f4b95560a8f65f266fba2bfbc42b320fe2d
VillAIgence CI #2420
run: 31372988719
result: FAILURE as intended
```

Observed: exactly eight compile errors for the absent `BoundedSemanticContradictionProducer` API.

GREEN:

```text
commit: a9e72c79594a8e06d8120ef61e513b785cff2977
VillAIgence CI #2422
run: 31373225512
common + deterministic mock-provider tests: SUCCESS
```

Coverage proves 16 candidate cap, eight-comparison cap, existing-pair suppression before comparison allocation, replay duplicate suppression, unrelated-pair isolation and delegation to the existing contradiction lifecycle.

### 4. Automatic controlled-admission integration

First test-only attempt:

```text
commit: bf2be822de8836c1ece3d1e3c58e05f819f67e17
VillAIgence CI #2424
run: 31373544186
result: INVALID FIXTURE ATTEMPT — NOT PRODUCT RED
```

The test referenced the wrong nested result type (`ResolvedContradiction` instead of `ResolvedSemanticContradiction`) and therefore failed compilation. Production code was not changed from this attempt.

Corrected behavioral RED — tests only:

```text
commit: 43baaab2d003d5ec2cc6b074ee3df05295fed0bd
VillAIgence CI #2426
run: 31373786982
result: FAILURE as intended
682 tests / exactly 2 failures
```

The full suite compiled. Exactly the two automatic-production assertions failed:

```text
controlledFactAndBeliefAdmissionAutomaticallyRecordsOneTruthNeutralDisagreement
replayedControlledAdmissionDoesNotCreateAnotherLiveRelation
```

The transformed trailing-sentence boundary test already passed. This is the intended behavioral RED: controlled FACT/BELIEF admission persisted successfully but no automatic contradiction event existed yet.

GREEN:

```text
commit: 48722a390cba8e931b46f492d46a9bcc3d25244b
VillAIgence CI #2428
run: 31374108022
common + deterministic mock-provider tests: SUCCESS
```

`ControlledSemanticMemoryIngestor` now persists, resolves the retained post-consolidation logical claim, and only then invokes the bounded producer. Existing public recordFact/recordBelief signatures are unchanged. FACT/BELIEF kind, provenance, confidence and source IDs remain unchanged in the integration assertions.

### 5. Pressure, forgetting and fresh-root reload preservation

Preservation tests-only commit:

```text
commit: 301a3facd64270025480cc6424357f235875d693
VillAIgence CI #2430
run: 31374412640
result: one test-fixture assertion failure; no production failure
```

The 10-NPC / >240-record hard-bound simulation and forgetting/no-resurrection tests passed. Fresh-root reload also preserved exact resolved relation equality, but a subsequent assertion incorrectly assumed canonical contradiction pair order was semantic-positive-first. The lifecycle canonically orders by logical claim ID, not semantic polarity.

Fixture-only correction:

```text
commit: 4ce3e7fb28717746b3dded058972aca13e63bd1a
VillAIgence CI #2432
run: 31374731497
common + deterministic mock-provider tests: SUCCESS
```

No production correction was required. Coverage proves:

- ten NPCs with more than 240 total Semantic candidate records remain at 16 candidates / 8 comparisons per producer invocation;
- forgotten source claims make historical contradiction evidence non-resolvable without resurrecting stored prose;
- fresh-root copies of `semantic-memory.json` and `memory2.json` resolve the same live relation exactly;
- contradiction evidence still stores no duplicate claim prose.

## Review hardening

Base-to-head review identified one boundedness-expression issue: retained contradiction duplicate-suppression used `Integer.MAX_VALUE` when reading an already bounded event store. The scan was changed to the explicit current `maxEventsPerNpc` bound in commit:

```text
b73d39e25311bf821d9638f348a564a92c0a98cd
```

This is a behavior-preserving hardening under the existing producer/replay/pressure tests, not an expansion of the feature contract.

## Compatibility / non-goals verified by scope

No new:

- provider request or response field;
- provider call;
- public config field;
- world file;
- `memory2.json` or `semantic-memory.json` version/field;
- migration/backfill;
- release identity;
- client authority;
- truth winner or confidence mutation.

The existing `semantic-contradiction-v1` lifecycle remains the sole persisted contradiction representation.

## Exact-head delivery evidence location

The staged RED/GREEN evidence above is immutable repository content. The final exact-head workflow run IDs, frozen feature SHA, review verdict and squash-merge SHA are recorded in PR #145 and then reconciled into `docs/PROJECT_STATE.md` / `docs/ROADMAP.md` after merge. This avoids changing the feature SHA merely to write the run IDs that verify that same SHA.

The mandatory frozen-head gates are:

```text
Repository security policy
VillAIgence CI
VillAIgence Production Soak
VillAIgence GitHub Release dry-run
```

Release publication remains outside this feature PR and must stay skipped.
