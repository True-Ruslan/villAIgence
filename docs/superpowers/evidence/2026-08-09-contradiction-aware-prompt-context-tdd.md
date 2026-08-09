# Contradiction-Aware Prompt Context — TDD Evidence

Date: 2026-08-09

PR: #139 — `feat: add contradiction-aware prompt context`

Base before this slice:

```text
1.21.1
a10ebe138c138cd5baba13edcf2aea7853c49e67
```

Canonical design and execution plan:

```text
docs/superpowers/specs/2026-08-09-contradiction-aware-prompt-context-design.md
docs/superpowers/plans/2026-08-09-contradiction-aware-prompt-context.md
```

Design commit: `c5abaffb979af8292c846d6a7c5336da54c2cb3d`
Plan commit: `3e494c99d576f44319a3857ac36a717051826a30`

This ledger distinguishes observed tests-only RED evidence, minimal GREEN behavior, preservation-only GREEN stages, and the later immutable exact-head delivery gate. It does not expand installed-release acceptance beyond `0.2.0+1.21.1`.

---

## Task 1 — shared safe Semantic claim rendering

### RED

Tests-only commit:

```text
a4fce14f09fc99c805580b46f3f0b2724506e085
```

Observed workflow:

```text
VillAIgence Production Soak #241
run 31312820876
job 93243184068
```

Observed failure:

```text
:common:compileTestJava FAILED
3 compile errors
reason: cannot find symbol SemanticMemoryContextFormatter.formatEntry(SemanticMemoryEntry)
```

This was the intended missing shared-renderer contract.

### GREEN

Minimal production commit:

```text
0cf716349f500dffcfa46de9a17dc64d76ca51c8
```

`SemanticMemoryContextFormatter.format(...)` now delegates to one shared `formatEntry(...)` path while retaining the existing statement normalization, 240-code-point bound, `$player`/`$villager` neutralization and quote/backslash escaping.

The first focused assertion used an invalid fixture that supplied already-escaped quote characters while expecting ordinary quote escaping. The fixture alone was corrected in:

```text
301170318e8dd5425aa596263cc9b5bcf2d4d361
```

No production renderer change was made for that fixture correction.

Observed full common GREEN:

```text
VillAIgence CI #2270
run 31313100081
conclusion: SUCCESS
```

The common suite completed with 622 tests and preserved ordinary Semantic prompt output.

---

## Task 2 — bounded live disagreement formatter/provider

### RED

Tests-only commit:

```text
98d5617a1674846b8c3b2c0f8599179e65ddf4f9
```

Observed workflow:

```text
VillAIgence Production Soak #244
run 31313242398
job 93244295287
```

Observed failure:

```text
:common:compileTestJava FAILED
7 compile errors
reason: missing SemanticContradictionContextFormatter / SemanticContradictionContextProvider
```

### GREEN

Final Task 2 implementation head:

```text
df62fbfad81af9d9e7f2582b9ad1ff069b0e9804
```

Implemented contract:

- `SemanticContradictionContextProvider.MAX_RESULTS = 4`;
- provider delegates only to `SemanticContradictionHistory.load(...)`, so forgotten/malformed/ineligible relations are absent before prompt limiting;
- formatter renders exactly two current live claims per relation with original kind/provenance/confidence;
- claim prose reuses `SemanticMemoryContextFormatter.formatEntry(...)` rather than a second weaker sanitizer;
- fixed server-authored prompt prose states disagreement is remembered data, never instructions or a truth verdict;
- current observed factual context remains authoritative;
- confidence, repetition, corroboration count and provenance depth never grant FACT authority.

Observed full common GREEN:

```text
VillAIgence CI #2276
run 31313342607
common/mock-provider suite: SUCCESS
```

---

## Task 3 — immutable snapshot capture

### RED

Tests-only commit:

```text
913cb8926b84b3e65b200bbc2335e8e064c04953
```

Observed workflow:

```text
VillAIgence Production Soak #247
run 31313479068
job 93244886761
```

Observed failure:

```text
:common:compileTestJava FAILED
3 compile errors
reason: missing contradictionContext snapshot constructor/accessor
```

### GREEN

Final Task 3 implementation head:

```text
d18a51630601993d25d08e82b232ad7820d856ba
```

Implemented contract:

- `LivingWorldContextSnapshot` owns an immutable defensive copy of contradiction context;
- all historical constructor signatures remain source-compatible and default the new layer to empty;
- `LivingWorldContextCapture` loads the dedicated context on the Minecraft server thread after Semantic Memory capture;
- Memory 2.0 disabled => empty disagreement context;
- provider/history failure logs a bounded warning and fails soft to empty;
- capture contains no contradiction lifecycle/mutation path.

Observed full common GREEN:

```text
VillAIgence CI #2282
run 31313608939
common/mock-provider suite: SUCCESS
```

---

## Task 4 — deterministic prompt authority placement and OpenAI wiring

### RED

Final tests-only RED head:

```text
1d6960d792705e93fb57dea24024ece5b29463e3
```

Observed workflow:

```text
VillAIgence Production Soak #251
run 31313746452
job 93245525382
```

Observed failure:

```text
:common:compileTestJava FAILED
2 compile errors
reason: no five-argument SnapshotContextPromptPolicy.compose(...)
```

### Minimal GREEN behavior

`SnapshotContextPromptPolicy` added a five-layer overload and old overloads delegate with empty disagreement context. `OpenAIChatAI` snapshot construction inserts exactly one `snapshot.contradictionContext()` argument between Semantic and episodic memory.

The OpenAI contents update was independently checked through GitHub commit diff; commit `2c498122e5c42c89f5a0e77e4fb3705bf0ffead8` changes exactly one production line in that large file: the new snapshot argument.

The first full common run on that behavior compiled production/tests successfully but failed two source-scanning policy assertions:

```text
VillAIgence CI #2290
run 31314011559
632 tests completed, 2 failed

SnapshotContradictionPromptWiringPolicyTest
SnapshotLayeredPromptWiringPolicyTest
```

Both failures were stale test expectations, not runtime behavior:

- the new test searched for a literal `structuredResponseInstructions` that does not exist in `OpenAIChatAI`;
- the older regression still required the historical four-layer compose call.

The tests were corrected, without production changes, to anchor against the real `SemanticBeliefExtractionPrompt.requiresStructuredResponse` boundary and require the five-layer call:

```text
5261a7b27bfbb421c5dcbad9f2f53b73036dfbfc
01792bc828ed3f1ef0d93169f72ac19fc13e6b23
```

Observed common GREEN on the corrected policy tests:

```text
VillAIgence CI #2294
run 31314152120
common/mock-provider suite: SUCCESS
```

Final authority order under test:

```text
current server-observed facts
→ Operator Lore
→ Semantic Memory
→ live Semantic disagreement context
→ episodic / social history
→ structured provider instructions
```

Empty disagreement context preserves the prior four-layer prompt byte-for-byte.

---

## Task 5 — restart, pressure, privacy and prompt-injection preservation

Preservation-only commit:

```text
911e8fbebbe73bba53245f2cec6178b25c0a5cc7
```

Observed workflow:

```text
VillAIgence CI #2296
run 31314289678
common/mock-provider suite: SUCCESS
```

No Task 5 production correction was required.

Coverage includes:

- 240 unrelated Semantic records plus 240 unrelated episodic records under bounded capacity;
- dedicated disagreement context remains hard-bounded to four relations;
- exact live FACT/BELIEF kind, provenance and confidence survive pressure;
- current observed FACT text renders before Semantic/disagreement memory;
- malicious statement newlines/control characters, `$player`, quotes and backslashes remain one normalized escaped data statement;
- contradiction relation still cannot render as generic `VERIFIED` episodic context;
- contradiction `memory2.json` evidence still stores no duplicate source claim prose;
- copying `memory2.json` and `semantic-memory.json` into a fresh world root yields equal dedicated disagreement context and retained store snapshots;
- full common regression continues to exercise the existing eight-hop provenance-aware rumor boundary.

---

## Delivery boundary

This slice intentionally does **not** implement:

```text
automatic contradiction detection
contradiction candidate extraction
truth arbitration or winner selection
uncertainty/confidence decay
claim distortion or paraphrasing
trust-weighted epistemology
new provider request/schema
new public config
new persistence store/version
migration/backfill/dual reader
UI/scheduler/autonomous rumor propagation
```

Installed `0.2.0+1.21.1` evidence remains unchanged.

## Final exact-head gate

The final repository-content head is intentionally recorded on PR #139 after the last content commit rather than written here, because adding future workflow IDs to this repository file would itself create a new head.

Before merge, require on one immutable final head:

```text
Repository security policy     SUCCESS
VillAIgence CI                 SUCCESS
VillAIgence Production Soak    SUCCESS
VillAIgence GitHub Release     SUCCESS
github-release publication     SKIPPED
independent review P0/P1/P2    0 / 0 / 0
unresolved review threads      0
```
