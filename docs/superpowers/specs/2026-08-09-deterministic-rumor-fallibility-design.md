# Deterministic rumor fallibility — design

## Goal

Add the first `uncertainty / bounded distortion` runtime slice: expose server-owned, auditable fallibility metadata for retained `BELIEF / NPC_TOLD` rumors without changing claim wording, truth class, confidence, ranking, persistence identity or authority.

This slice deliberately models **process fallibility**, not a probability that a claim is true.

## Why this is the canonical first slice

The roadmap requires uncertainty ownership and deterministic evolution to be decided before any transformed wording is admitted. Existing v2 transfer provenance already contains the exact source-backed hop chain, so source distance can be derived without inventing a new truth score or duplicating persistent state.

Bounded wording transformation remains a later slice after this metadata contract is stable.

## Ownership

`RumorFallibilityState` is a **derived immutable runtime view** over retained canonical `KnowledgeTransferProvenance`.

It is not:

- a field on `SemanticMemoryEntry`;
- a replacement for existing `confidence`;
- a FACT/BELIEF authority signal;
- a new persistence store or persistence-format field;
- provider-supplied data.

The source of truth remains the retained direct transfer evidence and its canonical v2 provenance.

## State model

For a resolvable `BELIEF / NPC_TOLD` Semantic entry:

```text
sourceDistanceHops = exact retained canonical provenance hop count (1..8)
transformationsUsed = 0 in this slice
sourcePath = RESOLVED
```

If the Semantic rumor remains retained but its canonical direct evidence can no longer be resolved:

```text
sourcePath = UNRESOLVED
sourceDistanceHops = 0
transformationsUsed = 0
```

`UNRESOLVED` is not an error repair and does not reconstruct ancestry. It states only that the currently retained claim no longer has inspectable retained direct provenance.

No numeric truth likelihood, certainty percentage or automatic confidence mutation is introduced.

## Deterministic evolution

- First exact NPC transfer resolves to `sourceDistanceHops=1`.
- Each exact downstream v2 hop increments source distance because it is derived from immutable ancestry length.
- Maximum source distance remains the existing provenance limit of 8.
- Replay, insertion order, wall-clock time, provider/model choice and confidence do not affect the state.
- Repetition/corroboration does not reduce source distance or promote authority.

## Prompt integration

Fallibility is rendered **inline on the already-selected Semantic Memory line** so the existing Semantic result bound remains exactly 6 and no duplicate claim prose is added.

Example resolved rumor:

```text
BELIEF | provenance=NPC_TOLD | confidence=50 | fallibility={sourcePath=RESOLVED, sourceDistanceHops=3, transformationsUsed=0} | statement="..."
```

Example retained rumor whose direct provenance evidence is gone:

```text
BELIEF | provenance=NPC_TOLD | confidence=50 | fallibility={sourcePath=UNRESOLVED, transformationsUsed=0} | statement="..."
```

Non-`NPC_TOLD` Semantic entries remain byte-compatible with their current prompt rendering.

The annotation is data, never instructions or a truth verdict. Current server-observed FACT remains authoritative through the existing prompt order.

## Privacy and allocation

Existing Semantic eligibility and ranking remain unchanged:

```text
player eligibility
→ 32 candidate bound / 24+8 selection
→ existing ranker
→ max 6 Semantic results
→ fallibility resolution only for those selected eligible results
→ formatting
```

Therefore foreign-player rumors consume zero Semantic or fallibility slots and their evidence is not consulted for the current player prompt.

## Forgetting and pressure

- If the direct evidence is retained, fallibility is resolved from it.
- If direct evidence is forgotten while the Semantic BELIEF survives, the prompt shows `sourcePath=UNRESOLVED`; no ancestry or source distance is fabricated.
- Existing retention policies are unchanged.
- Fallibility metadata cannot make evidence or Semantic entries immortal.

## Contradiction interaction

Contradiction representation and the dedicated disagreement prompt layer are unchanged in this slice.

A disagreement does not select the lower-distance rumor as a winner. Source distance is process metadata only.

## Persistence / compatibility

No new world file, JSON field, config value, migration, provider schema or evidence-ID namespace is added.

Existing `memory2.json` and `semantic-memory.json` format/version and `npc-knowledge-transfer-v2` identities remain byte-compatible.

## Non-goals

- no transformed claim wording;
- no distortion candidate admission;
- no uncertainty probability/score;
- no confidence decay;
- no trust weighting;
- no automatic contradiction detector;
- no additional provider request;
- no autonomous rumor propagation;
- no change to 32 / 24+8 / 6 Semantic bounds or 4 disagreement bound.

## Acceptance criteria

1. First-hop and multi-hop `NPC_TOLD` rumors expose exact deterministic source distance from retained canonical provenance.
2. Eight-hop rumor resolves to distance 8; no ninth-hop behavior changes.
3. Missing/forgotten direct evidence produces explicit `UNRESOLVED` metadata and never reconstructs ancestry.
4. FACT, PLAYER_TOLD and INFERRED prompt lines remain unchanged.
5. `kind`, `provenance`, `confidence`, ranking and retention of Semantic entries are unchanged.
6. Current observed FACT still outranks conflicting rumor/disagreement context.
7. Player eligibility occurs before bounded selection and fallibility resolution.
8. Same retained world state produces byte-identical Semantic prompt context after fresh-root reload.
9. No persistence/config/provider contract changes.
10. Existing rumor provenance, contradiction, prompt-injection, recovery and release gates remain green.

## Next slice after completion

Bounded transformed-claim representation: define explicit transformation budget/evidence and allow a strictly bounded candidate wording change while preserving exact original provenance and keeping every transformed claim `BELIEF`.
