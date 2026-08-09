# Contradiction-Aware Prompt Context Implementation Plan

Date: 2026-08-09

Goal: implement one bounded immutable prompt layer for already-recorded live Semantic contradictions without truth arbitration.

## Constraints

- Strict RED → minimal GREEN for each behavior-changing task.
- No automatic contradiction detector.
- No new provider call/schema or public config.
- No persistence/store/version/migration change.
- No FACT/BELIEF/provenance/confidence/ranking mutation.
- Current observed world facts remain the top authority layer.
- Generic episodic prompt exclusion for `SEMANTIC_CONTRADICTION` remains intact.
- Existing Semantic `32 / 24+8 / 6` bounds and rumor 8-hop cap remain unchanged.
- Root `CHANGELOG.md` updates in the runtime PR.
- After runtime merge, only `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` change in a separate docs PR.

## Task 1 — shared safe Semantic claim rendering

Files:
- tests: `SemanticMemoryContextFormatterTest.java` and/or new focused formatter test
- modify after RED: `SemanticMemoryContextFormatter.java`

RED contract:
- expose one package-owned formatting primitive usable by contradiction rendering;
- preserve existing ordinary Semantic context output byte-for-byte;
- same 240-code-point limit, whitespace/control normalization, `$player`/`$villager` neutralization, quote/backslash escaping.

GREEN:
- extract the smallest shared package-private/public-safe method without changing existing prompt output.

## Task 2 — bounded contradiction context formatter/provider

Files:
- create tests: `SemanticContradictionContextFormatterTest.java`, `SemanticContradictionContextProviderTest.java`
- create after RED: `SemanticContradictionContextFormatter.java`, `SemanticContradictionContextProvider.java`

RED contract:
- no output for no live relations;
- format both live claims with kind/provenance/confidence/statement;
- server-authored section explicitly says disagreement is not a verdict and current observations win;
- claim prose uses shared Semantic sanitization;
- max four resolved relations;
- deterministic history order preserved;
- foreign-player relations filtered before the four-result limit;
- forgotten/malformed relation absent through existing history resolution.

GREEN:
- provider delegates to `SemanticContradictionHistory.load(..., 4)`;
- formatter renders data-only lines and fixed authority prose.

## Task 3 — immutable snapshot capture

Files:
- tests: `LivingWorldContextSnapshot` / context wiring tests
- modify after RED: `LivingWorldContextSnapshot.java`, `LivingWorldContextCapture.java`

RED contract:
- new `contradictionContext` snapshot field defensively copies input;
- historical constructors remain source-compatible and default empty;
- Memory 2.0 disabled returns empty contradiction context;
- runtime loading failure fails soft to empty without changing other context.

GREEN:
- load context on server-thread capture through dedicated provider and copy into snapshot.

## Task 4 — deterministic prompt authority placement

Files:
- extend tests: `SnapshotContextPromptPolicyTest.java`, `SnapshotMemoryWiringPolicyTest.java`, `SnapshotLayeredPromptWiringPolicyTest.java` where applicable
- modify after RED: `SnapshotContextPromptPolicy.java`, `OpenAIChatAI.java`

RED contract:

```text
current observations
< Operator Lore
< Semantic Memory
< Semantic contradiction context
< episodic/social history
< structured provider instructions
```

- contradiction section appears exactly once;
- current FACT remains structurally before disagreement;
- contradiction line never renders as generic `VERIFIED` event;
- both claims remain their original kinds; no winner/truth label is introduced;
- old compose overloads remain source-compatible.

GREEN:
- add five-layer compose overload and delegate old overloads with empty contradiction context;
- wire `snapshot.contradictionContext()` exactly once in snapshot prompt construction.

## Task 5 — restart/privacy/pressure/prompt-injection preservation

Files:
- create `SemanticContradictionPromptSimulationTest.java` or equivalent focused preservation tests

Coverage:
- fresh-root reload produces equal dedicated contradiction prompt context;
- forgetting one side removes relation from prompt while remaining historical event stores no claim text;
- current/foreign/shared player scopes are isolated before output bound;
- >200 Semantic + >200 episodic pressure records stay bounded/deterministic;
- malicious statement containing newline/control chars, quotes, backslashes, `$player`, `$villager`, and prompt-like instructions remains one escaped data statement and cannot create a new section/role;
- current observed FACT text remains before conflicting BELIEF/disagreement text;
- eight-hop rumor provenance regression remains green in full suite.

No production change unless an observed RED demonstrates a real defect.

## Task 6 — delivery

- update root `[Unreleased]` with dedicated bounded disagreement prompt layer and unchanged authority/persistence/provider boundaries;
- create `docs/superpowers/evidence/2026-08-09-contradiction-aware-prompt-context-tdd.md` with exact observed RED/GREEN evidence only;
- independent base→head review for authority escalation, prompt injection, privacy leakage, duplicate rendering, forgotten-text resurrection, bound bypass and schema/config drift;
- fresh exact-head mandatory workflows:
  - Repository security policy;
  - VillAIgence CI;
  - Production Soak;
  - GitHub Release dry-run;
  - publication job must remain SKIPPED;
- require P0/P1/P2 = 0/0/0 and zero unresolved review threads;
- squash merge exact verified head;
- separate docs-only reconciliation after merge.
