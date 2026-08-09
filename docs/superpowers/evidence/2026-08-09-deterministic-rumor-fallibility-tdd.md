# Deterministic rumor fallibility — TDD evidence

## Scope

This ledger records the tests-first development evidence for PR #141, the first canonical sub-slice of `uncertainty / bounded distortion`.

The runtime contract is intentionally narrow:

```text
retained BELIEF / NPC_TOLD
+ retained canonical v2 transfer provenance
→ server-derived fallibility metadata
→ existing selected Semantic prompt slot
```

No claim wording is transformed in this slice. No truth probability, confidence mutation, FACT promotion, persistence/config/provider schema, new prompt slot or automatic contradiction detector is introduced.

Base:

```text
8329b886e792a368de1b92caf8904ab52bae3558
```

## Task 1 — pure fallibility model

### RED

Tests-only head:

```text
faba693af2653f06e0fedd423dc0033ca35a8fdf
```

VillAIgence CI #2308 / run `31319213089` reached `:common:compileTestJava` and failed with exactly 11 compile errors. Every error was the expected missing `RumorFallibilityState` / `RumorFallibilityPolicy` API from the new test contract.

Production code had not been added.

### GREEN

Minimal implementation head:

```text
53e56a55abc817a0eb4c7664ab28702e2c8819f0
```

The model permits only:

```text
RESOLVED   sourceDistanceHops=1..8  transformationsUsed=0
UNRESOLVED sourceDistanceHops=0     transformationsUsed=0
```

`RumorFallibilityPolicy` derives resolved distance only from provenance that already passes `KnowledgeTransferProvenancePolicy.valid(...)`.

VillAIgence CI #2312 / run `31319375044` passed the full common/mock-provider suite.

## Task 2 — retained-source resolver

### RED

Tests-only head:

```text
758b452036c489c7ebfe672ca5e6a1461685872b
```

VillAIgence CI #2314 / run `31319511221` reached `:common:compileTestJava` and failed with exactly 5 errors, all for the intentionally absent `RumorFallibilityResolver`.

### First GREEN candidate and fixture correction

Minimal resolver head:

```text
bca5863ee9125569270bb32f0889b1857fe46ea2
```

The resolver is read-only and delegates canonical ancestry selection to the existing `KnowledgeTransferProvenanceResolver`.

VillAIgence CI #2316 / run `31319668754` compiled production and tests, then reported one failing new test. The failure was a `NoSuchElementException` before the resolver call: the test attempted to find the first transfer's old Semantic entry ID after a second corroborating branch had correctly triggered Semantic consolidation and changed the retained entry identity.

This was a test-fixture assumption, not a runtime fallibility defect. Production was not changed.

Test-only correction head:

```text
01fdd0eafd269d31413260367f92c78eeca0612d
```

The test now locates the current retained consolidated `BELIEF / NPC_TOLD` by its logical claim properties and verifies both direct evidence IDs before resolving fallibility.

VillAIgence CI #2318 / run `31319851382` passed the full common/mock-provider suite. This proves:

- newest-valid canonical direct branch is reused;
- source distance follows that selected branch;
- forgotten/missing direct provenance becomes explicit `UNRESOLVED`;
- FACT, PLAYER_TOLD and INFERRED entries are not rumor-fallibility candidates.

## Task 3 — inline Semantic prompt annotation

### Behavioral RED

Tests-only head:

```text
723d49927551ffeb5a734b5fbde53c24c594f0b3
```

VillAIgence CI #2320 / run `31320009387` compiled successfully and executed 643 common tests. Exactly two new assertions failed:

- resolved NPC_TOLD line lacked fallibility metadata;
- unresolved retained NPC_TOLD line lacked fallibility metadata.

The ordinary PLAYER_TOLD byte-compatibility assertion already passed.

### Minimal production candidate

Head:

```text
920558041f8600e6805e23dedc0be1624d84e821
```

Changes were deliberately limited to:

- a fallibility-aware formatter overload;
- inline rendering before the existing safe statement field;
- `SemanticMemoryContextProvider` invoking that overload only after existing player eligibility, long-horizon candidate selection and rank-to-6 completion.

Existing `format(List<RankedSemanticMemory>)` remains available for ordinary callers.

VillAIgence CI #2324 / run `31320214405` compiled successfully but the same two exact-string tests remained red. Inspection showed the fixture expected `confidence=100`, while the existing canonical `NpcKnowledgeTransferLifecycle` intentionally admits every listener transfer BELIEF with `importance=50`, `confidence=50`.

Only the test expectations were corrected. No confidence or transfer production semantics were changed.

### Conditional guidance RED

Test head:

```text
2e84208ae3db29060f32300fcd595869be8a4c47
```

The corrected resolved/unresolved line assertions passed. A strengthened ordinary-prompt assertion intentionally required that fallibility explanatory guidance must not appear when the selected Semantic context contains no fallibility metadata.

VillAIgence CI #2326 / run `31320411500` executed 643 common tests and failed exactly one assertion:

```text
nonRumorSemanticPromptRenderingRemainsByteCompatibleAndAddsNoFallibilityGuidance
```

This exposed that the new guidance line was unconditional.

### GREEN

Production head:

```text
3866dc9b21de50816682e0b91fe437bc4799e5d7
```

The formatter now adds the fallibility guidance only when at least one already-selected Semantic line contains `fallibility={...}`. No other prompt prose or ranking behavior changed.

VillAIgence CI #2328 / run `31320624642` passed the full common/mock-provider suite.

## Task 4 — preservation simulation

Preservation-only head:

```text
0dcf14406cedad395f3f9f749a2f2dadadc4f1e5
```

No production correction was added for this task.

The deterministic simulation covers:

- an exact 8-hop rumor chain and `sourceDistanceHops=8`;
- `transformationsUsed=0` at the current no-distortion boundary;
- >200 Semantic noise entries;
- >200 episodic noise events;
- high-score foreign-player private Semantic noise excluded before current-player allocation;
- existing max-6 Semantic result bound;
- existing 240-code-point normalization/template neutralization/quote/backslash escaping;
- current server-observed FACT prompt precedence;
- fresh-root `memory2.json` + `semantic-memory.json` reload with byte-identical selected Semantic context;
- retained Semantic rumor kind/provenance/confidence unchanged across reload;
- direct evidence forgetting degrading only to `sourcePath=UNRESOLVED`, with no fabricated distance or ancestry resurrection.

VillAIgence CI #2330 / run `31320788093` passed the full common/mock-provider suite for this preservation head.

## Contract conclusions

The implementation evidence supports the following exact claims:

1. Fallibility is derived server-side from retained canonical v2 provenance, not supplied by the provider or client.
2. `sourceDistanceHops` is process distance only and never a truth score.
3. `transformationsUsed` is structurally fixed to zero in this slice; wording distortion is not yet supported.
4. Loss of direct provenance evidence is represented as `UNRESOLVED`; ancestry is not reconstructed.
5. Semantic FACT/BELIEF kind, provenance, confidence, ranking, retention and existing bounds are unchanged.
6. Player visibility filtering still occurs before bounded selection and fallibility resolution.
7. Ordinary non-rumor Semantic rendering remains unchanged and receives no extra fallibility guidance.
8. Current server-observed factual context remains authoritative.
9. No persistence/config/provider/release identity contract is changed by the runtime design.

## Delivery evidence

Final exact-head security, CI, Production Soak, GitHub Release dry-run, independent review and merge evidence is recorded in PR #141 after the repository-content head is frozen. It is intentionally not predeclared here before those runs complete.
