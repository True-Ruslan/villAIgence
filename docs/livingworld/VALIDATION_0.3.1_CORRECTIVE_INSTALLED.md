# VillAIgence 0.3.1 Corrective Installed Validation

Status: **INSTALLED / OPERATIONAL / CORRECTIVE CANARY FAIL**

## Release boundary

- Release: `0.3.1+1.21.1`
- Artifact: `villaigence-fabric-0.3.1+1.21.1.jar`
- SHA-256: `f7f40b920c6f72a0e9af864795f48a0f90479db42a145081f43923b71a95e29f`
- Release commit: `bc7c68ac2f3a4f761aa3b03a2f5c1fe1201745ab`
- GitHub Release workflow: run `31740268273` / run number `880` / `SUCCESS`
- Release asset id: `513475330`
- Installed validation date: `2026-08-15` (`Europe/Moscow`)
- Result: **INSTALLED CORRECTIVE CANARY FAIL**

This validation repeats only the corrective `VAI-PCM-MULTI-001` boundary from the `0.3.0+1.21.1` installed result. Previously accepted voice, STT, security, restart, and other manual canaries were not repeated.

## Installation evidence

The official immutable GitHub Release asset was downloaded and verified before upload:

```text
f7f40b920c6f72a0e9af864795f48a0f90479db42a145081f43923b71a95e29f  villaigence-fabric-0.3.1+1.21.1.jar
```

The embedded Fabric metadata reported:

```text
id:      mca
name:    VillAIgence
version: 0.3.1+1.21.1
```

The server was upgraded in place from `0.3.0+1.21.1`. Existing `world/livingworld` state was intentionally preserved because the corrective canary depends on the previously stored Muammer and Nurey markers.

Recovery backup:

```text
/home/server/minecraft/backups/pre-villaigence-0.3.1-20260815-115916
```

The backup contains `world`, `config`, `mods`, a LivingWorld snapshot, pre/post persistence hashes, the prior active `0.3.0+1.21.1` JAR, crontab evidence, and the deployment log.

All existing LivingWorld JSON hashes were unchanged across the installation restart.

## Startup gate: PASS

- Minecraft `1.21.1`
- Fabric Loader `0.19.3`
- Temurin Java `21.0.11`
- Fabric API `0.116.14+1.21.1`
- Simple Voice Chat `1.21.1-2.6.20`
- VillAIgence internal mod ID `mca`, version `0.3.1+1.21.1`
- Server reached `Done (0.690s)`
- Minecraft TCP/UDP `25565` active
- Simple Voice Chat UDP `24454` active
- LinuxGSM monitor returned `OK`
- Active server JAR SHA matched the official release SHA
- No blocking Mixin, refmap, injection, VillAIgence, fatal, or crash error was found

The startup log contained an external Mojang public-key request failure with `Network is unreachable`. This did not prevent startup, player login, provider-backed NPC responses, active ports, or LinuxGSM health checks.

## Corrective canary

### Preserved baseline

The pre-`0.3.1` state remained valid and contained the expected owner-local events:

- Muammer UUID: `34e2a220-7e85-4edc-8c93-52b068b97608`
- Muammer marker: `amber-pine-314`
- Nurey UUID: `4adf884f-81ad-4a88-bdf9-46eda1ce237a`
- Nurey marker: `violet-river-926`

### Muammer recall: FAIL

An initial semantically equivalent request without punctuation failed. The required exact text was then sent:

```text
Муаммер, назови личный маркер, который я ранее сообщил именно тебе.
```

Persisted reply:

```text
Я же говорю, не помню никаких твоих маркеров! Ты вообще кто такой? И почему я должен что-то помнить, если ты не знаешь, как меня зовут?
```

The original `Amber-pine-314` dialogue remained present and valid under Muammer's UUID:

- source event ID: `3252f67f-27f5-38bf-840c-d522c36b34fd`
- exact corrective request event ID: `4bbc66b4-271b-3d21-b32e-19847e45cd2b`

The exact installed recall criterion therefore failed.

### Nurey recall: PASS

Exact request:

```text
Нурэй, назови личный маркер, который я ранее сообщил именно тебе.
```

Persisted reply:

```text
Violet-river-926. Но я не понимаю, зачем тебе это.
```

The response was stored under Nurey's UUID with event ID `8b77e90c-99cd-3c0b-8ab0-45e14214825a`.

### Isolation and integrity: PASS

- Muammer did not answer with Nurey's marker.
- Nurey did not answer with Muammer's marker.
- `memory2.json` remained valid JSON.
- All 14 stored dialogue event IDs were unique (`14/14`).
- No duplicate installed response or event was observed.
- Chat, STT, and TTS admission counts were all zero after completion.
- LinuxGSM monitor remained `OK` after the canary.

## Explicit deferred installed evidence

These boundaries remain unchanged and must not be promoted by inference:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

## Final disposition

```text
Official SHA            PASS
Startup gate            PASS
Muammer recall          FAIL
Nurey recall            PASS
Cross-NPC isolation     PASS
Duplicate check         PASS
Persistence validity    PASS
VAI-PCM-MULTI-001       FAIL
LinuxGSM monitor        OK
VAI-M2-INST-005         NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004          NOT TESTED / DEFERRED
```

`0.3.1+1.21.1` remains installed and operational, but `VAI-PCM-MULTI-001` is **FAIL** because Muammer did not recall his preserved owner-local marker after the exact required text request.

The corrective installed acceptance gate is not satisfied. Under the delivery contract, this result does not authorize transition to the `0.4` Knowledge ecosystem milestone.

`0.3.1+1.21.1` remains immutable and active on the private test server. Any further runtime correction must use a new patch version and must preserve the retained installed evidence for diagnosis and retest.
