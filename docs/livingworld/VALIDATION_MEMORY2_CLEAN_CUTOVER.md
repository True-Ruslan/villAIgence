# Memory 2.0 Persistent Dialogue Clean Cutover Validation

Date: 2026-08-07
PR: #119 — `feat: cut persistent dialogue over to Memory 2.0`
Branch: `feat/memory2-clean-cutover`
Latest official release while this work is unreleased: `0.1.26+1.21.1`

## Decision boundary

VillAIgence is pre-1.0 and the supported deployment for this development package is an operator-only test server that may be rebuilt from a clean world/LivingWorld state. Preserving experimental legacy conversation history has no supported-user requirement.

The previous additive `memory.json` migration plan is therefore cancelled. This package performs a direct clean cutover:

```text
memory2.json = sole persistent dialogue-memory source
memory.json  = no current runtime/recovery role
```

No importer, migration checkpoint ledger, dual persistent reads, summary parser or destructive legacy conversion is implemented.

## Canonical RED

Tests were committed before the production contract existed.

```text
VillAIgence CI: #1748
run:            31160054411
result:         RED as expected
failure stage:  :common:compileTestJava
```

The compiler failed because the tests intentionally required the not-yet-existing:

- `MemoryEvent.DialogueExchange`;
- `MemoryEvent.dialogue()`;
- `Memory2DialogueHistory`.

Security/release-contract setup before that compile boundary remained green. The RED therefore established the missing clean-cutover API rather than reproducing an unrelated environment failure.

## Implemented contract

### Structured DIALOGUE

New DIALOGUE events retain the existing bounded episodic `summary` and additionally contain:

```text
DialogueExchange
├── playerMessage
└── npcReply
```

The structured payload and summary use the same independently normalized/bounded utterances. Prompt reconstruction reads the structured payload only; delimiter-like text inside an utterance cannot change roles.

Historical summary-only DIALOGUE events with no structured payload are ignored for prompt reconstruction rather than parsed or guessed.

### Exact dialogue retrieval

`Memory2DialogueHistory` requires:

```text
ownerNpcId == current NPC
AND type == DIALOGUE
AND participants == exact current NPC/player pair
AND DialogueExchange exists
```

Filtering occurs **before** result limiting. Newer ACTION, OBSERVATION or RELATIONSHIP_CHANGE events cannot starve eligible recent dialogue merely by occupying the generic event tail.

Eligible exchanges are restored to chronological order and emitted as alternating `user` / `assistant` messages before the existing Working Memory hard bounds are applied.

### Exactly-once persistence

The existing production post-success boundary remains authoritative:

```text
ChatAI
→ successful usable answer
→ Memory2DialogueLifecycle
→ Memory2DialogueIngestor
→ MemoryEventStore
```

The inherited `PersistentChatMemory` type is retained only as a temporary no-storage call-surface adapter for `OpenAIChatAI`:

- it reads `Memory2DialogueHistory` only;
- it never resolves or opens `memory.json`;
- both `append(...)` methods intentionally perform no persistent write;
- the actual write remains the single post-success Memory 2.0 lifecycle above.

The old `ConversationMemoryStore`, `MemoryMessage`, and their dedicated tests are removed.

A source-policy regression test prevents reintroduction of the old store/path or a second persistence writer through this compatibility seam.

## Automated acceptance contract

Focused common tests cover:

- structured round-trip with delimiter-like text;
- exact NPC isolation;
- exact player isolation;
- chronological user/assistant reconstruction;
- filter-before-limit behavior under newer non-dialogue events;
- Working Memory 12-message cap;
- summary-only historical DIALOGUE exclusion;
- legacy persistent-store closure policy.

The production fixture now creates persistent conversation state through Memory 2.0 and verifies it can be recalled again.

Current canonical corruption/recovery stores:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

The destructive recovery matrix exercises exactly five current cases:

```text
memory2-empty
semantic-wrong-root
relationships-incompatible-schema
voices-stale-temp
operator-lore-invalid-orphan-temp
```

For each case the harness requires:

- recovery startup succeeds;
- expected corrupt backup preserves exact bytes where applicable;
- unaffected sibling stores remain byte-identical;
- a second startup is idempotent;
- the recovered canonical store remains valid.

## Intermediate exact-production evidence

On PR head `8f05cba56bb09bd6f0bd0fc1ad40d2a009e700bf`:

- common test execution reached **459 tests PASS**;
- Java PR CI built both NeoForge and Fabric successfully;
- repository security passed;
- server GameTests and loader builds passed;
- production acceptance contract tests passed;
- exact production JAR startup, clean stop/save and same-world restart passed;
- lifecycle/grave/restart evidence passed;
- production voice transport evidence passed;
- the destructive recovery script produced **five PASS cases** with correct backup/idempotency/unaffected-store evidence.

That workflow was still reported FAIL because a stale workflow postcondition expected six recovery cases. This was a CI contract defect, not a failed recovery case. The postcondition was corrected to five for current CI/nightly/release workflows.

## Version-aware immutable release recovery

The release-recovery controller is intentionally different from current-branch CI.

After resolving a release tag it checks out the **immutable target release commit** and executes that release's own scripts/tests. `0.1.26` historically has a six-store recovery matrix, while releases after this clean cutover may have five.

Therefore release recovery does not hardcode either count. It requires:

```text
report status == PASS
cases is a non-empty list
all case statuses == PASS
```

The immutable target commit's own recovery contract tests define exact case/store coverage. This preserves recovery of historical `0.1.26` without imposing later persistence assumptions on it.

## Required PR #119 completion gate

Before merge, the current exact PR head must pass all applicable mandatory workflows, including:

- repository security policy;
- common tests;
- Java PR CI / Fabric + NeoForge;
- server GameTests;
- exact production startup/stop/save/restart;
- five-store destructive recovery;
- production soak;
- release dry-run/package identity where triggered;
- version-aware immutable release-recovery validation where triggered;
- independent full-diff review with no unresolved P0/P1/P2 finding.

PR #119 is the live source of exact final-head workflow identities because documentation-only corrections can advance the branch head during review.

## Installed boundary — NOT YET CLAIMED

Automated exact-JAR evidence is not installed operator-server acceptance.

After merge and after an exact candidate/release package is explicitly authorized, installed validation must use a **clean test-world/LivingWorld state** and verify at minimum:

```text
first text dialogue persists
→ next text turn recalls it
→ voice turn writes the same DIALOGUE model
→ server restart
→ same NPC/player dialogue remains recallable
→ another NPC/player does not receive that history
```

Until that operator test is performed, the clean cutover is **automation-complete, installed acceptance pending**.

`VAI-CONCUR-004` remains separately **NOT TESTED / DEFERRED** until two real graphical clients are available.

## Next product slice

Do not return to legacy migration unless a new supported-user data-preservation requirement appears.

Next 0.2 work:

```text
controlled BELIEF producers
→ provenance/admission for PLAYER_TOLD / NPC_TOLD / INFERRED
→ deterministic consolidation/conflict handling
→ trustworthy causal relationship reasons
→ long-horizon recall
→ NPC-to-NPC knowledge transfer
→ rumor propagation with provenance and uncertainty
```
