# Bounded settlement social-topology routing — design

Date: 2026-09-19  
Track: 0.4 Knowledge ecosystem

## Goal

Let existing directed NPC↔NPC social state influence **which already-eligible settlement resident** receives one bounded knowledge-transfer opportunity, without turning social state into truth authority or graph-wide routing.

## Existing boundary

`SettlementKnowledgeFlowSelector` already bounds one cycle to 16 residents, four speakers, two source candidates per speaker and four opportunities. It currently hashes each source directly to one listener. `SettlementKnowledgeFlowLifecycle` then reads the exact speaker→listener edge strictly and may suppress that single route.

That means adverse state can block sharing, but positive direct state cannot help choose a better route among the already-bounded residents.

## New routing contract

For each selected source claim:

1. Construct a deterministic listener window independent of social state.
2. The listener window contains at most **four** distinct residents and never the speaker.
3. If any listener in that exact window already carries the same canonical statement + exact related-entity scope, emit no new opportunity. This preserves one successful fan-out on same-cycle replay even if social state later changes.
4. Strictly read only speaker→candidate direct social state. The bounded candidate window is validated/read in one strict batch parse; reverse edges never influence the route.
5. Classify candidates with the existing server-owned `PersonalitySocialInfluencePolicy`.
6. Exclude `FEARFUL`, `DISTRUSTFUL` and `ANTIPATHETIC` candidates.
7. Prefer positive `AFFILIATIVE` / `RESPECTFUL` candidates over `NEUTRAL`; preserve deterministic candidate order within a tier.
8. After one listener is chosen, revalidate that **exact** pair with a fresh strict read before transfer. If authority becomes unsafe/adverse, suppress the opportunity; never retarget or fallback after selection.

## Authority and safety

- LLM/provider has no role in candidate generation, ranking, identity, truth or mutation.
- No graph-neighborhood enumeration: only the pre-bounded settlement listener window is read.
- A social edge cannot promote BELIEF to FACT, mutate confidence/provenance or create source credibility.
- Missing graph persistence is explicit neutral through the existing strict reader.
- Malformed/unsafe graph persistence fails closed for the whole opportunity.
- No new world store or persistence schema is introduced.
- No public configuration is introduced.
- Existing `NpcKnowledgeTransferLifecycle` remains the sole transfer/provenance admission path.

## Replay invariant

The existing `MAX_FANOUT_PER_SOURCE_PER_CYCLE = 1` remains authoritative. Social reranking must not allow a source to reach a second listener when the cycle is replayed after one successful transfer. The durable listener Semantic claim acts as the bounded replay marker inside the deterministic route window; the selector stops before reranking when that knowledge is already present.

## TDD sequence

1. RED: positive direct route must beat the legacy neutral target.
2. RED: adverse routes are excluded while neutral deterministic order remains stable.
3. RED: route window hard-caps at four; positive state outside the window is invisible.
4. RED: reverse-only social state does not affect speaker→listener routing.
5. RED: successful same-cycle replay cannot retarget after social-state changes.
6. RED: malformed strict social authority suppresses the opportunity with no fallback.
7. GREEN: minimal pure routing policy + bounded selector/lifecycle integration.
8. Full Memory 2.0 / relationship regression, security, CI, Production Soak and release dry-run.

## Delivery bookkeeping

Because this is a `feat:` 0.4 capability, the final PR must also:

- add its PR number to `docs/releases/0.4.0-convergence.json`;
- update root `changelog.md` `[Unreleased]`;
- record staged TDD evidence;
- preserve `NEXT_RELEASE.txt` unchanged until a separate release request.
