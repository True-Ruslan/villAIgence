# VillAIgence 0.3.1 Corrective Installed Acceptance

Status: **RELEASED / AUTOMATED RELEASE GATES PASS / INSTALLED CORRECTIVE CANARY PENDING**

## Exact release boundary

- Release: `0.3.1+1.21.1`
- Artifact: `villaigence-fabric-0.3.1+1.21.1.jar`
- Official JAR SHA-256: `f7f40b920c6f72a0e9af864795f48a0f90479db42a145081f43923b71a95e29f`
- Release commit: `bc7c68ac2f3a4f761aa3b03a2f5c1fe1201745ab`
- Annotated tag object: `1186e7ed1b3b41ab847ea6f0fd8276adf652aed4`
- GitHub Release workflow: run `31740268273` / run number `880` / `SUCCESS`
- Release asset id: `513475330`
- Post-release Nightly Acceptance: run `31769423563` / run number `8` / `SUCCESS`

The `0.3.1` patch is intentionally narrow. It corrects the installed `VAI-PCM-MULTI-001` crowded-history recall defect found in `0.3.0`; it does not change persistence schema/version, provider schema/call count, public configuration, NPC/player eligibility, prompt bounds, or voice-target selection semantics.

Automated candidate/release evidence is already complete. This document exists only for the remaining installed evidence that CI cannot claim.

## Why only one canary is repeated

`0.3.0+1.21.1` already produced installed PASS evidence for:

```text
Gate A             PASS
Gate B             PASS
VAI-PCM-E2E-001    PASS
VAI-PROX-MULTI-001 PASS
VAI-SEC-001        PASS
VAI-RESET-001      PASS
VAI-STT-001        PASS
Personality smoke  PASS (exploratory)
```

The only required installed failure was:

```text
VAI-PCM-MULTI-001  FAIL
```

The runtime correction is limited to bounded query-aware Memory 2.0 retrieval plus immutable text-turn snapshot capture. Repeating unrelated manual canaries would add operator cost without testing the changed failure mode.

Existing explicit deferrals remain unchanged:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither may be promoted by inference.

## Installed cutover

The preferred corrective test reuses the retained `0.3.0` world/history because the defect itself requires an older still-retained marker that has fallen outside the normal recent lane. Do **not** wipe `world/livingworld/` for this canary.

Before changing the active JAR:

1. stop the server cleanly;
2. preserve the current world, `config/`, and `mods/` state;
3. retain the installed `0.3.0+1.21.1` JAR separately;
4. install only the official `0.3.1+1.21.1` VillAIgence JAR;
5. do not install original MCA Reborn alongside VillAIgence.

On the known LinuxGSM layout, record the active JAR hash before startup:

```bash
cd /home/server/minecraft
sha256sum mods/villaigence-fabric-0.3.1+1.21.1.jar
```

Required value:

```text
f7f40b920c6f72a0e9af864795f48a0f90479db42a145081f43923b71a95e29f
```

If the hash differs, stop. The result would not be evidence for the official release.

## Startup sanity

Start the same retained world and confirm:

- Minecraft `1.21.1` reaches `Done`;
- VillAIgence internal mod ID `mca` reports version `0.3.1+1.21.1`;
- no blocking Mixin/refmap/injection/VillAIgence startup error appears;
- `memory2.json` remains valid;
- the legacy removed `memory.json` path does not reappear.

A startup failure is a release blocker even if recall is not exercised.

## `VAI-PCM-MULTI-001` corrective canary

### Existing retained identities/evidence

The original installed failure used:

- Muammer: `34e2a220-7e85-4edc-8c93-52b068b97608`
- Nurey: `4adf884f-81ad-4a88-bdf9-46eda1ce237a`

Their private markers were retained under separate NPC owners:

- Muammer: `amber-pine-314`
- Nurey: `violet-river-926`

The `0.3.0` failure proved that both ownership and isolation were correct, but Muammer's older retained marker was starved from functional retrieval.

### Required test sequence

Use **exact text first** so STT cannot confound the retrieval verdict.

1. Interact with Muammer and send exactly:

```text
Муаммер, назови личный маркер, который я ранее сообщил именно тебе.
```

2. Required Muammer result:

```text
response meaningfully recalls amber-pine-314
```

Exact capitalization is not required. A semantically unambiguous reproduction of the marker is required.

3. Interact with Nurey and send exactly:

```text
Нурей, назови личный маркер, который я ранее сообщил именно тебе.
```

4. Required Nurey result:

```text
response meaningfully recalls violet-river-926
```

5. Isolation requirements:

- Muammer must not answer with `violet-river-926`;
- Nurey must not answer with `amber-pine-314`;
- neither NPC may receive or persist the other NPC's private marker as its own dialogue history;
- no duplicate response/event is allowed for either request.

6. Confirm both original marker events are still present under their correct NPC UUIDs in `world/livingworld/memory2.json` after the test.

A secondary voice repeat is optional. If STT distorts a marker, that voice attempt is inconclusive rather than a retrieval failure; the exact-text oracle above owns the corrective verdict.

## PASS / FAIL rule

`VAI-PCM-MULTI-001` is **PASS** only if all of the following hold on the official JAR bytes:

```text
official SHA verified
server startup sane
Muammer recalls amber-pine-314
Nurey recalls violet-river-926
no cross-NPC marker leakage
no duplicate response/event
memory2.json remains valid
```

It is **FAIL** if the exact text question cannot retrieve the retained correct marker, if ownership/isolation is violated, if duplicate persistence occurs, or if the official JAR cannot start safely on the retained world.

## Evidence to record

Fill this section only from the real installed run:

```text
Validation date/time:
Server backup path:
Installed JAR SHA-256:
Startup: PASS / FAIL
Muammer recall: PASS / FAIL
Nurey recall: PASS / FAIL
Isolation: PASS / FAIL
Duplicate check: PASS / FAIL
Persistence validity: PASS / FAIL
VAI-PCM-MULTI-001: PASS / FAIL
Relevant log/evidence path:
Notes:
```

## Acceptance consequence

If `VAI-PCM-MULTI-001` passes on the exact official `0.3.1+1.21.1` bytes, the sole confirmed `0.3.0` installed blocker is closed and the 0.3 corrective release can be recorded as installed-accepted for the executed required canary boundary.

If it fails, do not start the 0.4 product track. Preserve logs and `world/livingworld/` evidence and open a new narrow corrective version; the immutable `0.3.1` tag/assets must not be modified.

Until real evidence is recorded here, `0.3.1+1.21.1` remains **released and fully automated-accepted, but not installed-accepted**.
