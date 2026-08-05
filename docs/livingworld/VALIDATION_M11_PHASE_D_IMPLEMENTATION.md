# M11 Phase D — Concurrency and client acceptance implementation

## Purpose

Automate the remaining Operator Lore concurrency and client-state risks without weakening the S10b server-authority or S10c editor contracts.

Phase D proves a deterministic logical two-client conflict lifecycle through the production revision policy and world-local persistence boundary. A real installed two-client Minecraft UI/network canary remains separate.

## Baseline

```text
base branch:       1.21.1
base commit:       4a8275608248b36c3de15c61db8ce51e5b8b665e
implementation PR: #112
```

Protected boundaries:

- permission level, player identity and target resolution remain server-owned;
- C2S packets contain no arbitrary player/villager UUID, dimension ID or village ID;
- the client has no `operator-lore.json` or store access;
- SHA-256 optimistic revisions remain mandatory;
- conflict never triggers an automatic overwrite;
- Operator Lore remains separate from Semantic Memory;
- provider, voice, gameplay and persistence schemas are unchanged.

## Canonical RED

```text
commit:  bd5d14856d49e1a3ea8e2ed52facfbfddc68d214
run:     31015893007 / VillAIgence CI #1520
boundary: :common:compileTestJava
failure: OperatorLoreResolvedAuthority was absent at the two production acceptance call sites
```

The RED reached the intended missing seam. Release identity and repository security passed before the focused common-test compilation failure. Java Pull Request CI #908 and Repository security policy #947 passed independently.

## Implementation

### Resolved authority seam

`OperatorLoreResolvedAuthority` applies the existing permission, revision, payload and persistence contract only after Minecraft server code has resolved a trusted `OperatorLoreKey`.

It is package-private and contains no:

- `ServerPlayer` or entity lookup;
- packet or network ownership;
- client identity fields;
- direct target selection;
- new persistence format.

`OperatorLoreServerAuthority` still owns:

- permission level 2 checks;
- authenticated PLAYER UUID resolution;
- live same-level nearby villager resolution;
- home-village dimension and stable ID derivation;
- canonical entity ID returned to the client;
- controlled error mapping.

Each resolved operation opens the world-local store from the trusted world root. Sequential logical requests therefore observe the latest persisted revision rather than a stale test-owned in-memory copy.

### Deterministic logical-client acceptance

`OperatorLoreConcurrencyAcceptanceTest` exercises two independent `OperatorLoreEditorModel` instances against one world-local store and the production resolved authority seam.

Covered behavior:

1. both clients read the same canonical value and revision;
2. client A writes successfully;
3. client B submits the stale revision;
4. B receives `CONFLICT` with A's canonical value and revision;
5. the stale write performs no file mutation;
6. B retains its draft and explicitly adopts the current revision;
7. B retries and succeeds exactly once;
8. final read returns B's canonical value and new revision;
9. exact replay returns `UNCHANGED` without rewriting the file;
10. unauthorized read/write returns `FORBIDDEN`, discloses no value/revision and performs no mutation;
11. Clear uses the same stale-revision conflict and explicit retry path.

### Client-state and authority coverage

Existing S10c model tests continue to prove:

- response generation N is ignored after generation N+1 becomes active;
- wrong scope/entity and closed-screen responses are ignored;
- conflict preserves the draft and exposes explicit server-version/keep-draft choices;
- `FORBIDDEN`, `NOT_FOUND`, `INVALID` and `ERROR` preserve the draft;
- Save is available only for valid dirty state.

`OperatorLorePhaseDAuthorityPolicyTest` adds source-level tripwires requiring:

- bounded C2S fields with no authority-owned UUID/dimension/village IDs;
- trusted target resolution before the persistence seam;
- package-private resolved authority with no server entity or network ownership;
- no client file/store access or global response mailbox;
- confirmation modals to forward in-flight responses only to their owning editor;
- `ClientHandlerImpl` to dispatch only to the active `OperatorLoreResponseReceiver`.

## Intermediate fixture correction

```text
commit:  820c2cb7f8e2148e07968b951415c8d7fd95b5e1
run:     31016167243 / VillAIgence CI #1522
result: 421 existing tests and three new Phase D scenarios passed
failure: Clear fixture loaded an already-empty canonical value, so beginSave correctly returned empty
```

This was not a production defect. The fixture was corrected by seeding a non-empty initial canonical value before both clients loaded it. Production code was unchanged for this correction.

## Scenario mapping

| Plan scenario | Automated evidence |
|---|---|
| D1 two logical clients | `OperatorLoreConcurrencyAcceptanceTest.twoLogicalClientsRejectStaleWriterAndCompleteExplicitConflictRecovery` |
| D2 exact replay | `exactReplayIsUnchangedAndDoesNotRewriteThePersistentFile` |
| D3 permission isolation | `unauthorizedLogicalClientCannotReadDiscloseOrMutateCanonicalLore` plus existing permission tests |
| D4 target isolation | existing `OperatorLoreTargetPolicyTest` plus Phase D source authority tripwires |
| D5 request generation | existing `OperatorLoreEditorModelTest.staleScopeEntityAndClosedResponsesAreIgnored` |
| D6 conflict state | logical-client acceptance plus existing conflict model test |
| D7 Save/modal ownership | `OperatorLorePhaseDAuthorityPolicyTest.clientOwnsNoOperatorLoreFileStoreOrGlobalResponseMailbox` and S10c screen forwarding |
| D8 revision-protected Clear | `clearUsesTheSameRevisionProtectedWritePath` |

## Acceptance catalog

- `VAI-CONCUR-003` is now the automated logical-client/common-integration scenario.
- `VAI-CONCUR-004` retains the real installed two-client UI/network canary.

The evidence layers remain distinct.

## Manual boundary

Phase D automation does not claim:

- two physical Minecraft clients over a real network;
- pixel/layout quality at every resolution;
- mouse and keyboard behavior on every operating system;
- compatibility with a particular installed modpack;
- Simple Voice Chat UDP/Opus multiplayer behavior.

The installed canary remains:

```text
open the same scope on two clients
→ save on client A
→ submit stale content on client B
→ require explicit conflict
→ reload or keep draft on B
→ save with the current revision
→ reopen and verify the final canonical value
```

## Final exact-head verification

Final run identifiers and the reviewed head are recorded after the implementation, documentation and acceptance catalog are all synchronized and the complete required CI matrix passes.
