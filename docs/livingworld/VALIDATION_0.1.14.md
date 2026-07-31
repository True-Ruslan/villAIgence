# VillAIgence 0.1.14+1.21.1 Live-Server Validation

## Result

```text
V0114_FINAL_RESTART_VERIFICATION_PASS
```

**Status:** PASS for deterministic Semantic Memory forgetting/decay, source durability, persistence and per-NPC isolation, with one explicitly retained automated-only subcriterion.

**Validation date:** 2026-07-31

**Minecraft:** 1.21.1

**Release:** `0.1.14+1.21.1`

**Release tag / tested commit:**

```text
c45aea45dd915b24ba236344feef30559c7171bb
```

GitHub comparison confirmed that `0.1.14+1.21.1` is an ancestor of the current `1.21.1` branch and identifies the exact tested release payload. Later security and supply-chain commits are not part of this checkpoint.

---

## Validation scope

The test targeted PR #56 deterministic pressure-based Semantic Memory retention:

```text
entries <= capacity
→ keep all valid unique semantic entries

entries > capacity
→ consolidate first
→ calculate deterministic retention ranking
→ keep the strongest bounded set
```

The controlled semantic capacity was temporarily set to:

```text
3
```

After the pressure scenario it was restored to the normal value:

```text
256
```

The final restart and hash comparison were performed after restoring the normal capacity.

---

## Retention-pressure evidence

### Older confirmed FACT survived

The older corroborated FACT owned by Basiliso remained in Semantic Memory after capacity pressure.

Confirmed:

```text
older confirmed FACT retained                              PASS
semantic UUID preserved                                   PASS
sourceEventIds preserved                                  PASS
source durability affected the retention outcome          PASS
```

The retained entry therefore survived because of the implemented deterministic retention policy rather than newest-only trimming.

### Decay ordering among otherwise equal entries

The test produced entries whose remaining retention inputs were equivalent enough for age to determine their order.

```text
decay ordering observed                                   PASS
newer equal-ranked entry retained ahead of older peer     PASS
```

This confirms the game-time age component participates in real capacity decisions.

### Weak relationship FACT was displaced

A weak `RELATIONSHIP_CHANGE`-derived FACT owned by Casimiro was removed when capacity pressure selected stronger knowledge.

```text
weak existing relationship FACT evicted                   PASS
predicted pressure behavior observed                      PASS
```

This proves real eviction of an already persisted weak semantic entry.

### Consolidation and source evidence remained intact

The relevant Basiliso and Casimiro semantic identities remained internally consistent through pressure and restart.

```text
semantic UUID identity preserved                          PASS
sourceEventIds preserved exactly as stored                PASS
consolidated evidence not split                           PASS
```

No confidence mutation, FACT/BELIEF conversion or provenance rewriting was observed.

---

## NPC isolation

Retention pressure was exercised for two different NPC owners.

Confirmed:

```text
Basiliso pressure evaluated independently                 PASS
Casimiro pressure evaluated independently                 PASS
ownerNpcId separation                                     PASS
cross-NPC eviction or reinforcement                       none
```

Knowledge belonging to one NPC did not evict, merge with or reinforce the other NPC's Semantic Memory.

---

## Rejected-new-append boundary

The only live criterion not completed was byte-identical persistence after rejection of a **new** weak candidate.

Three different social scenarios were attempted, but the active Chat model did not produce a `RELATIONSHIP_CHANGE`. Therefore no new weak semantic candidate reached `SemanticMemoryStore.append`, and the no-rewrite behavior could not be exercised through the normal live gameplay pipeline.

The following remains true:

```text
rejected-new-append byte stability live-proven            no
rejected-new-append byte stability automated-proven       yes
existing weak FACT eviction under pressure live-proven    yes
```

Automated evidence remains the PR #56 store regression test:

```text
rejectedWeakAppendDoesNotRewriteSemanticFile
```

Exact feature head and CI:

```text
c08b47431b6a121deae4be8410be1e4fe4c5126a
VillAIgence CI #764 / 30573965448 — SUCCESS
Java Pull Request CI #307 / 30573965439 — SUCCESS
```

This boundary does not invalidate the live proof of forgetting/decay, source durability, eviction, persistence or NPC isolation. It prevents claiming that the rejected-new-candidate no-rewrite branch itself was exercised on the real server.

---

## Restart persistence

After restoring capacity from `3` to `256`, hashes were recorded before and after the final restart.

All five files were byte-identical:

```text
memory.json                                                PASS
memory2.json                                               PASS
semantic-memory.json                                       PASS
relationships.json                                         PASS
voices.json                                                PASS
```

Semantic identities and evidence also survived:

```text
Basiliso semantic UUID                                     PASS
Basiliso sourceEventIds                                    PASS
Casimiro semantic UUID/state                               PASS
Casimiro sourceEventIds/state                              PASS
```

No retention decision was reversed by restart and no unexpected rewrite occurred.

---

## Chat, voice and operations

```text
Chat                                                       SUCCESS
STT                                                        SUCCESS
TTS                                                        SUCCESS
Simple Voice Chat connection                               PASS
Opus initialization                                        PASS
UDP 24454                                                  LISTENING
UDP 25565                                                  LISTENING
LinuxGSM monitor                                           ACTIVE
server state                                               STARTED
```

No regressions were observed in the text or voice pipelines.

---

## Error review

```text
VillAIgence errors                                         none
memory/save errors                                         none
OutOfMemory errors                                         none
```

The final restart completed without corruption, recovery or persistence failures.

---

## Security validation boundary

The `0.1.14+1.21.1` tag points to:

```text
c45aea45dd915b24ba236344feef30559c7171bb
```

The current `1.21.1` branch later advanced through Step 1 H1–H5 security and supply-chain hardening. Those commits are not part of the tested `0.1.14` payload.

Therefore this checkpoint does **not** close the controlled H1/H2 runtime validation required by:

```text
docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md
```

A later security candidate must validate provider endpoint policy, bounded network responses and voice resource limits independently.

---

## Final conclusion

`0.1.14+1.21.1` is the latest confirmed live-server Memory 2.0 checkpoint.

Live-proven:

```text
deterministic retention pressure                          PASS
older corroborated FACT survival                          PASS
source-evidence durability                                PASS
decay ordering                                            PASS
existing weak FACT eviction                               PASS
per-NPC isolation                                         PASS
restart-safe persistence                                  PASS
Chat / STT / TTS / Voice Chat / Opus                     PASS
server / monitor / ports                                  PASS
```

Automated-only subcriterion:

```text
new weak append rejected without rewriting semantic file
```

The next release-validation priority is the controlled H1/H2 security scenario against a build containing the post-`0.1.14` security hardening commits.