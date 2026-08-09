# Semantic Contradiction Representation — TDD Evidence

Date: 2026-08-09

PR: #137 — `feat: represent semantic contradictions`

Base before this slice:

```text
1.21.1
2e8dde22924b3f07ec7122181bd8a8ff3a8e6489
```

Canonical design and execution plan:

```text
docs/superpowers/specs/2026-08-09-semantic-contradictions-design.md
docs/superpowers/plans/2026-08-09-semantic-contradictions.md
```

The final synchronized design commit is `fa15664894eb096544bab6ad179897fbcddffd0d`; the final synchronized execution-plan commit is `30c4d9fa649722fa9f57bb54d733a1957f4d0349`.

This ledger distinguishes observed tests-only RED evidence, minimal GREEN implementation evidence, preservation-only GREEN stages, and the later exact-head delivery gate. It does not promote source/candidate automation into installed-release acceptance. The current installed release boundary remains `0.2.0+1.21.1`.

---

## Task 1 — stable Semantic logical claim identity

### RED

Tests-only commit:

```text
9c44dbbba1c4359173ab0901386c6e48580d721c
```

Observed workflow:

```text
VillAIgence CI #2198
run 31308818776
job 93233328300
```

Observed failure:

```text
:common:compileTestJava FAILED
10 compile errors
reason: cannot find symbol SemanticMemoryIdentity
```

The failure was limited to the intentionally missing shared logical-identity API; repository/security/release-identity steps before compilation were not the cause.

### GREEN

Implementation sequence introduced `SemanticMemoryIdentity` and refactored `SemanticMemoryConsolidator` to delegate existing canonicalization without changing `semantic-consolidated-v1` output.

Final Task 1 implementation head:

```text
980402b4216b20a7dc3ce01a160a5edd1f8a5e4c
```

Observed workflow:

```text
VillAIgence CI #2202
run 31308961724
conclusion: SUCCESS
```

The tests prove that source-event union does not change logical claim identity, NFKC/case/whitespace/scope canonicalization matches the pre-existing consolidation contract, kind/provenance remain part of identity, and existing deterministic consolidated entry IDs stay byte-compatible.

---

## Task 2 — structured contradiction event, bounded retention and prompt isolation

### RED

Tests-only commit:

```text
2e05a3e34d78b5698c813afaa494ec7bbf9e623c
```

Observed workflow:

```text
VillAIgence CI #2208
run 31309140945
job 93234121568
```

Observed failure:

```text
:common:compileTestJava FAILED
27 compile errors
```

Failures were the expected missing contradiction model surface: `SemanticContradiction`, `MemoryEvent.semanticContradiction()` and `MemoryEvent.Type.SEMANTIC_CONTRADICTION`.

### GREEN

Implementation added:

- optional structured `SemanticContradiction` payload;
- canonical immutable `ClaimSnapshot`s without claim prose;
- source-compatible `MemoryEvent` constructors;
- `SEMANTIC_CONTRADICTION` type;
- bounded observation/action-equivalent retention contribution;
- explicit exclusion from generic `Memory2ContextProvider` prompt retrieval.

Final Task 2 implementation head:

```text
0fe92a8eb19c3d30b583d76bce8bc93258e7892d
```

Observed workflow:

```text
VillAIgence CI #2216
run 31309323524
common/mock-provider suite: SUCCESS
```

The event remains in `memory2.json`, is never eligible for `SemanticMemoryIngestionAdapter.toFact(...)`, and cannot accidentally appear as a generic `VERIFIED` episodic prompt line.

---

## Task 3 — canonical construction and fail-closed integrity

### RED

Tests-only commit:

```text
289cd09bdfe986d4ce4fe3d698e74cff6713e59b
```

Observed workflow:

```text
VillAIgence CI #2220
run 31309453092
job 93234878986
```

Observed failure:

```text
:common:compileTestJava FAILED
18 compile errors
reason: missing SemanticContradictionAdapter / SemanticContradictionPolicy
```

Before adding production behavior, the tests-only RED was strengthened in:

```text
2216caf6c6d6322f3e83ba12cc7c9aad10ff1ac8
```

That hardening made the deterministic event UUID commit to the full canonical stored snapshots — logical claim ID, detected concrete Semantic entry ID, kind, provenance and canonical scope — rather than only the pair of logical claim IDs. This ensures those persisted snapshot mutations fail offline integrity validation without requiring live Semantic state.

### GREEN

Final Task 3 implementation head:

```text
ac3566542048e6dce06776d20826164ed88c8325
```

Observed workflow:

```text
VillAIgence CI #2226
run 31309670100
common/mock-provider suite: SUCCESS
```

The final `semantic-contradiction-v1` event is order-independent for A/B versus B/A at the same authoritative game time, binds the exact structured snapshots, rejects same logical content/owner mismatch/scope mismatch, and preserves each source claim's original kind/provenance.

---

## Task 4 — exact-ID source-backed lifecycle

### RED

Tests-only commit:

```text
e075ba41c0a24490a4b3b74fa170727e6396e59e
```

Observed workflow:

```text
VillAIgence CI #2228
run 31309801872
job 93235721581
```

Observed failure:

```text
:common:compileTestJava FAILED
31 compile errors
reason: missing SemanticContradictionLifecycle / SemanticContradictionResult
```

### GREEN

Final Task 4 implementation head:

```text
0eb2a7a657f4d027b5954d1501edacec9f7e6971
```

Observed workflow:

```text
VillAIgence CI #2232
run 31309927862
common/mock-provider suite: SUCCESS
```

Lifecycle guarantees exercised by tests:

- caller provides server-owned world/NPC/Semantic-entry IDs only;
- both exact Semantic entries are read and authoritatively reread before event construction;
- `SAME_CLAIM`, `SCOPE_MISMATCH`, `SOURCE_NOT_RETAINED`, `REJECTED` and `EVENT_NOT_RETAINED` are explicit;
- exact replay with reversed A/B order at the same tuple/time is byte-idempotent in `memory2.json`;
- later authoritative game time creates distinct process evidence;
- event pressure cannot mutate or roll back either Semantic claim.

---

## Task 5 — live resolved contradiction history

### RED

Initial tests-only commit:

```text
90687d1a57adc629e09fd2b0a36f09ae4408e18c
```

Observed workflow:

```text
VillAIgence CI #2234
run 31310065299
job 93236391353
```

Observed failure:

```text
:common:compileTestJava FAILED
12 compile errors
reason: missing SemanticContradictionHistory
```

Before production implementation, a tests-only correction removed an invalid assumption that a FACT would always sort into the first canonical snapshot position:

```text
61d4f9122690205f03c2372c4bab3e04712c9077
```

This changed no desired product behavior; it made the assertion use logical identity rather than incidental ordering.

### GREEN

Final Task 5 implementation head:

```text
20db98e47b4d09734c1e4465149551bc7fb34398
```

Observed workflow:

```text
VillAIgence CI #2238
run 31310223950
common/mock-provider suite: SUCCESS
```

Resolved history:

- accepts only retained canonically valid contradiction evidence;
- resolves current claims by stable logical claim identity rather than stale concrete entry IDs;
- revalidates current kind/provenance/scope against stored snapshots;
- survives deterministic Semantic source-union consolidation;
- disappears from the resolved view when either logical claim is forgotten;
- applies existing global/private/shared player eligibility before result limiting;
- orders newest event first, then event UUID ascending;
- never parses event summary or recovers claim prose from contradiction evidence.

---

## Task 6 — restart, pressure, privacy and truth preservation

Preservation-only test commits:

```text
59a4695cf15616be8d61943762e1628e6d38d0ca  fresh-root persistence
25e0ff6c589d982a7fb4aa180b099dc905e65883  deterministic pressure/privacy simulation
```

Observed workflow for the final preservation head:

```text
VillAIgence CI #2242
run 31310402293
common/mock-provider suite: SUCCESS
```

No Task 6 production correction was required.

Coverage includes:

- copy of `memory2.json` and `semantic-memory.json` into a fresh world root with exact contradiction-evidence equality;
- assertion that `memory2.json` contains structured contradiction metadata but not duplicated source claim text;
- global/private/shared player scopes across independent NPCs;
- 240 unrelated Semantic entries and 240 unrelated episodic events under hard capacity pressure;
- forward/reverse pressure insertion producing the same relevant retained snapshot;
- FACT/SYSTEM_OBSERVED and BELIEF/NPC_TOLD kind/provenance/confidence remaining unchanged after contradiction recording;
- bounded Semantic and episodic prompt outputs;
- contradiction evidence excluded from generic episodic context;
- existing prompt guarantees `Current observed factual context wins on conflict.` and `Confidence never converts a BELIEF into a FACT.` remaining intact;
- the complete common suite retaining the existing provenance-aware rumor regressions, including the eight-hop boundary.

---

## Delivery boundary

This slice intentionally does **not** implement:

```text
automatic semantic contradiction detection
LLM/provider truth arbitration
contradiction winner selection
confidence / uncertainty decay
claim text distortion
trust-weighted epistemology
autonomous rumor spread
dedicated contradiction prompt prose
new public config
new persistence store or schema version
migration / backfill / dual reader
```

`SEMANTIC_CONTRADICTION` is process evidence only. `SYSTEM_OBSERVED` describes that the server recorded a relation between two exact retained claims; it does not make either claim true and cannot be ingested as Semantic FACT.

## Final exact-head gate

The final exact-head delivery matrix is intentionally not pre-filled here because writing its future run IDs into this file would itself change the commit being verified. The immutable final evidence is recorded on PR #137 after the last repository-content commit and must include all of:

```text
Repository security policy     SUCCESS
VillAIgence CI                 SUCCESS
VillAIgence Production Soak    SUCCESS
VillAIgence GitHub Release     SUCCESS
github-release publication     SKIPPED
independent review P0/P1/P2    0 / 0 / 0
unresolved review threads      0
```

No GitHub Release is published by this development PR. Exact installed evidence for `0.2.0+1.21.1` remains unchanged.