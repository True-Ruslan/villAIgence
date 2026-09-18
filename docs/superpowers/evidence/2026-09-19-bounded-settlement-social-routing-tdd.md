# Bounded Settlement Social-Topology Routing — TDD Evidence

Date: 2026-09-19
Base: `90cebf3887386727843d0f0108ec4a947b6e3c11`
PR: #178
Branch: `feat/0.4-settlement-social-routing-v2`

## Contract

This slice extends the existing bounded settlement knowledge-flow primitive without creating graph traversal, global knowledge state, provider authority or a new persistence schema.

Hard routing bounds and authority rules:

```text
existing settlement resident window <= 16
existing speakers per cycle         <= 4
existing source candidates/speaker <= 2
existing opportunities/cycle       <= 4
social listener candidates/source  <= 4
fan-out/source/cycle                = 1
```

Only exact directed speaker→listener social state is consulted. `AFFILIATIVE` / `RESPECTFUL` candidates are preferred over neutral candidates. `FEARFUL`, `DISTRUSTFUL` and `ANTIPATHETIC` candidates are ineligible. Reverse-only state never authorizes or blocks a forward route.

The provider/LLM cannot enumerate candidates, rank routes, choose identities, promote truth, alter confidence, or bypass provenance. Successful transfer still delegates to `NpcKnowledgeTransferLifecycle`, preserving listener-local `BELIEF/NPC_TOLD`.

Malformed strict social authority fails closed. The exact selected pair is re-read before mutation; there is no post-selection fallback.

## Staged RED / GREEN ledger

### 1. Positive direct route preference

Tests-first RED:

```text
tests-only/spec head: d7df03c11fbaaf9e71a3cfe702cb579c773e229f
VillAIgence CI #3008
run: 35395435641
result: FAILURE as intended
865 tests completed, exactly 1 failed
failure: SettlementKnowledgeFlowLifecycleTest
         > positiveDirectedRouteIsPreferredOverLegacyNeutralTarget()
```

Infrastructure preceding `:common:test` passed: checkout, Java 21, acceptance selector contract, exact release identity and repository security.

Minimal GREEN introduced:

- `SettlementSocialKnowledgeRoutingPolicy`;
- a deterministic listener window capped at four;
- direct speaker→listener strict social reads;
- positive-over-neutral ranking;
- adverse-candidate exclusion;
- exact selected-pair revalidation before transfer.

Equivalent rebased production commits on the final PR ancestry:

```text
4352b7e2dd126891b698214c5ac03cee75b2b57c  feat: add bounded settlement social route policy
6de07f6f2b96f882ba67a7d86766457bfa5dd9f1  feat: route settlement knowledge by bounded social state
```

First GREEN verification:

```text
VillAIgence CI #3028
run: 35395824402
test-and-build: SUCCESS
common tests: SUCCESS
risk-selected GameTests + supported loader builds: SUCCESS
production acceptance/startup: SUCCESS
package verification: SUCCESS
```

### 2. Same-cycle social retarget prevention

Second tests-first RED:

```text
commit: 78e3e833a785b01f616e37813217cc1ed3fcb750
VillAIgence CI #3037
run: 35396260246
result: FAILURE as intended
866 tests completed, exactly 1 failed
failure: sameCycleSocialChangeCannotRetargetSuccessfulSourceToSecondListener()
```

The defect was real: after one successful transfer, changing the social graph during the same settlement cycle could make the same source reach a second listener.

### 3. Preservation failure rejected instead of weakening behavior

The first anti-retarget guard was intentionally too broad: it treated knowledge held anywhere in the route window as a permanent suppression condition.

Verification:

```text
commit: c127c7938ea5a224c99ad86b4fb256888b497980
VillAIgence CI #3039
run: 35396610437
866 tests completed, exactly 1 failed
failure: SettlementKnowledgeFlowPersistenceTest
         > laterCycleMayProgressToAnotherDeterministicTargetWithoutBroadcast()
```

This was not accepted as GREEN. The established contract allows gradual propagation in a later settlement cycle.

Production correction:

```text
commit: b029716a8dbed8c319ec7dedfb5436f32fe188f3
fix: scope social fanout guard to current cycle
```

The corrected guard suppresses rerouting only when equivalent scoped knowledge appeared in the bounded route window during the current cycle interval `[cycleStart, authoritativeGameTime]`. Knowledge learned in an earlier cycle therefore does not block the established later-cycle dissemination path.

Corrected exact-head delivery matrix:

```text
Repository security #2677: SUCCESS
VillAIgence CI #3042: SUCCESS
Production Soak #598: SUCCESS
GitHub Release dry-run #934: SUCCESS
review threads: none
```

### 4. Final authority/boundedness characterization

Tests-only hardening adds explicit coverage for:

- candidate window hard cap = four and no speaker/self candidate;
- positive direct state outside that bounded window is invisible;
- positive candidates outrank neutral while adverse candidates are excluded;
- all-adverse windows produce no route;
- an adverse legacy target may be skipped for an already-bounded neutral candidate before final selection;
- malformed `npc-social-graph.json` suppresses the entire opportunity with no transfer evidence;
- existing reverse-only hostility coverage continues proving directionality;
- existing fresh-root replay and later-cycle progression coverage remains authoritative.

These tests do not widen runtime behavior. Any failure is treated as a contract defect rather than a test to be weakened.

## Persistence and migration

No new world store, format version, field, migration or backfill is introduced.

Existing stores remain unchanged, including:

```text
memory2.json
semantic-memory.json
npc-social-graph.json
```

The same clean-state boundary and recovery policy remain in force.

## Release bookkeeping

PR #178 is registered in `docs/releases/0.4.0-convergence.json` as a `0.4` capability PR. Root `changelog.md` records the user-visible routing and authority guarantees. `docs/releases/NEXT_RELEASE.txt` is intentionally unchanged; this capability does not publish `0.4.0+1.21.1` by itself.
