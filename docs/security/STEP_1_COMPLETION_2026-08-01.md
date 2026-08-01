# Step 1 Security and Supply-Chain Hardening — Completion Record

**Completion date:** 2026-08-01  
**Final implementation merge:** PR #70 — `88a20d86e8b08e4b5eaf60da943a63e750f2b545`  
**Final accepted release artifact:** `0.1.17+1.21.1`  
**Final artifact SHA-256:** `b33af40f7a2696dc679c49e0fc544f6b5df99e0aa600ea5c767bc5a9747da1ab`  
**Final marker:** `V0117_SEC004_ARTIFACT_AND_EVIDENCE_PASS`

## Completion statement

Step 1 Security and supply-chain hardening is complete within its defined scope.

```text
SEC-001 Closed
SEC-002 Closed
SEC-003 Closed
SEC-004 Closed
SEC-005 Closed
SEC-006 Closed
SEC-007 Closed
SEC-008 Closed
SEC-009 Closed
```

The closure is supported by repository implementation, targeted regression tests, common/Fabric/NeoForge CI, package verification, repository security policy, controlled real-server runs and exact-release-artifact acceptance where appropriate.

## Evidence chain

```text
0.1.15+1.21.1
→ production Chat/STT/TTS
→ endpoint and credential policy
→ TTS fail-soft persistence
→ restart durability
→ closes SEC-001 / SEC-002

0.1.16+1.21.1
→ declared/chunked/error response bounds
→ ten-minute total body deadline
→ provider no-redirect behavior
→ voice duration clamp
→ 128 MiB aggregate PCM budget
→ production restoration and restart
→ closes SEC-003 / SEC-007

0.1.17+1.21.1
→ verification HTTP success
→ 64 KiB declared/chunked bounds
→ 307 not followed
→ HTTPS loopback rejected before connection
→ closes SEC-004
```

Supply-chain, CI and repository closure evidence is retained in the H3, H4 and H5 follow-ups.

## Established baseline

Future work must preserve:

- server-side, endpoint-bound credentials;
- fixed production verification origin;
- no authenticated redirects;
- byte and deadline bounds for external responses;
- bounded voice capture and aggregate PCM;
- immutable and verified build inputs;
- least-privilege workflows;
- deterministic secret/source/script policy;
- removal of obsolete inherited utilities;
- explicit, sanitized acceptance tooling.

Step 1 completion does not exempt future changes from review. Newly introduced risk must be tracked as a new scoped finding or hardening phase.

## Next product priority

The next optimal development step is additive, deterministic and reversible migration of legacy `memory.json` dialogue into Memory 2.0 `DIALOGUE` events. Security maintenance remains a parallel fail-closed baseline rather than the primary product milestone.
