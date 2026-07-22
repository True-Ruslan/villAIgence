# Persistence Recovery Hardening Design

## Context

VillAIgence 0.1.7+1.21.1 is the current reliability release. The world-local stores for conversation memory, factual events, relationships and persistent NPC voices are critical continuity state under `<world>/livingworld/`.

`PersistentNpcVoiceStore` already treats malformed/unreadable JSON as an empty recoverable store and rewrites valid data on the next mutation. `ConversationMemoryStore`, `WorldEventStore` and `LivingWorldRelationshipStore` currently throw during construction when JSON parsing or file reads fail. Gameplay callers usually catch those failures, but the affected subsystem remains unavailable and repeated accesses can keep failing until the file is manually repaired.

## Goal

Make the three remaining LivingWorld JSON stores fail soft on malformed/unreadable persisted content, matching the established voice-store behavior, and add an explicit operator playtest gate for the remaining 0.1.x multiplayer/voice/restart validation.

## Scope

### Code

- `ConversationMemoryStore.load()` returns a fresh empty version-1 store when persisted JSON cannot be read or parsed.
- `WorldEventStore.load()` returns a fresh empty version-1 store when persisted JSON cannot be read or parsed.
- `LivingWorldRelationshipStore.load()` returns a fresh empty version-1 store when persisted JSON cannot be read or parsed.
- Existing atomic temp-file + replace writes remain unchanged.
- Existing format-version behavior remains unchanged.
- No new schema, config field, dependency or migration is introduced.

### Regression coverage

For each hardened store, tests must prove:

1. malformed JSON does not make store construction fail;
2. reads return the safe empty/neutral state;
3. the next normal mutation persists valid replacement JSON;
4. recreating the store from that file returns the newly persisted state.

`PersistentNpcVoiceStore` already has equivalent corrupt-file regression coverage and requires no behavioral change.

### Operational validation

Add `docs/livingworld/PLAYTEST_CHECKLIST.md` with repeatable checks for:

- concurrent multiplayer Chat/STT/TTS admission behavior;
- repeated voice-dialogue soak;
- provider `429` cooldown recovery;
- restart/reconnect persistence for `memory.json`, `events.json`, `relationships.json`, `voices.json`;
- `/villaigence ai status` diagnostics;
- world backup/restore sanity.

The checklist must distinguish automated CI guarantees from manual server/playtest evidence.

### Project state

Update `docs/PROJECT_STATE.md` so it records:

- `0.1.7+1.21.1` as the published reliability release from commit `8f3095c6e8489e077246d652be51ec3c0ff57cd8`;
- successful release workflow run `29913854688`;
- persistence recovery hardening as implemented once this PR is merged;
- remaining 0.1.x exit work as manual multiplayer/voice/restart field validation rather than missing core reliability architecture;
- `0.2 Memory 2.0` remains the next major development milestone after the validation gate.

## Error-handling decision

Use the existing fail-open convention rather than introducing quarantine/backup-file machinery in this slice. VillAIgence already documents world backup as an operator responsibility, and adding a new recovery file lifecycle would expand schema/UX scope immediately before Memory 2.0. The goal here is to prevent one malformed auxiliary JSON file from repeatedly disabling AI/social subsystems or breaking conversation flow.

## Non-goals

- Memory 2.0 schema or migration.
- Automatic repair of partially valid JSON records.
- New backup rotation or `.corrupt` quarantine files.
- SQLite or transactional multi-store persistence.
- Automated Minecraft multiplayer integration tests requiring a live game server.

## Success criteria

- New corruption-recovery tests pass under `:common:test`.
- Fabric distributable build/smoke-check passes.
- NeoForge/Fabric official Gradle PR CI passes.
- No compatibility-sensitive `mca`, `net.conczin.mca`, `config/livingworld.json` or `<world>/livingworld/` paths are renamed.
- The repository contains a concrete manual playtest checklist that can close the final 0.1.x field-validation gate before Memory 2.0 begins.
