# MCA 7.7.32 Selective Synchronization — S9 Operator Lore Store

**VillAIgence base:** `15ee14321f1b8d846ddb2df7c24c5159a53e6938`  
**Upstream target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`  
**Storage file:** `<world>/livingworld/operator-lore.json`

## Architecture contract

Operator lore is explicit server/operator-authored setting and character context. It is not:

- an authoritative current server observation;
- episodic Memory 2.0 dialogue/event memory;
- semantic FACT or BELIEF memory;
- a generated personality profile;
- an instruction to execute arbitrary actions.

S9 introduces persistence and formatting only. No prompt, networking or client UI integration is included.

## Schema v1

```json
{
  "version": 1,
  "world": "",
  "villagers": {},
  "players": {},
  "villages": {}
}
```

Stable keys:

```text
WORLD     -> singleton world value
VILLAGER  -> villager UUID
PLAYER    -> player UUID
VILLAGE   -> dimension + stable village ID
```

## Persistence guarantees

- isolated by normalized world root;
- deterministic field order and sorted map keys;
- maximum 4,096 Unicode code points per scope;
- CRLF and CR normalized to LF;
- normal Unicode, newline and tab preserved;
- other C0 control characters rejected;
- whitespace-only values remove the scoped entry;
- exact replay performs no file rewrite;
- temporary file is replaced atomically when the filesystem supports it;
- safe replace fallback is used when atomic move is unavailable;
- malformed data is copied to `operator-lore.json.corrupt`;
- malformed data fails open to a valid empty v1 file.

## Provenance formatting

Non-empty values are emitted only with explicit labels:

```text
Server-authored world lore:
Server-authored villager lore:
Server-authored player lore:
Server-authored village lore:
```

Blank scopes are omitted. Formatting does not ingest or classify the value as semantic memory.

## TDD evidence

### Invalid provisional RED boundary

The first test head `251307a5a527e5b05254a72bcd2aa317bd1b6a61` directly imported Gson into common test sources. CI #1084 correctly showed that the test boundary added a test-only loader dependency. The Gson assertions were replaced with UTF-8 JSON text assertions before production code.

### Canonical RED

```text
head: 93513ffc0fa170942f9ed374423082fcc4a774c8
VillAIgence CI #1085 / 30701896620 — EXPECTED FAILURE
boundary: common:compileTestJava
reason: S9 lore store, key, snapshot and formatter classes absent
Java Pull Request CI #594 / 30701896625       SUCCESS
Repository security policy #375 / 30701896616 SUCCESS
```

### GREEN production head

```text
head: afaec5b07bdeebd454763739e19178c698f0b7f4
VillAIgence CI #1090 / 30702162481              SUCCESS
Java Pull Request CI #599 / 30702162509       SUCCESS
Repository security policy #385 / 30702162477 SUCCESS
```

The GREEN gate executed:

```text
:common:test
:fabric:build
:neoforge:build
Fabric distributable-package verification
repository security policy
```

## Automated test matrix

```text
empty store returns blank scopes
all scopes round-trip independently
snapshot preserves four provenance domains
world roots are isolated
village IDs are dimension-aware
line endings normalize without losing Unicode
forbidden controls are rejected
4,096-code-point truncation does not split emoji
exact replay does not change file mtime
blank value removes scoped entry
saved JSON is inspectable and versioned
temporary file is removed after replacement
malformed JSON is backed up and recovered
formatter labels provenance and omits blanks
```

## Accumulated installed-server acceptance

S9 is not yet reachable from gameplay/UI. Validate its persistence together with S10 integration:

```text
1. Write distinct world, villager, player and village values through the server-authoritative editor/command layer.
2. Confirm operator-lore.json is under the active world root, not global config.
3. Restart and confirm exact values and scope isolation.
4. Confirm equal replay does not alter file hash/mtime.
5. Confirm village ID 7 in overworld and nether remains distinct.
6. Enter normal Unicode, tabs and multiline text; confirm normalization.
7. Enter a value longer than 4,096 code points; confirm bounded storage without broken surrogate pairs.
8. Corrupt a copied-world file and restart; confirm .corrupt backup and empty valid recovery.
9. Confirm memory.json, memory2.json, semantic-memory.json, events.json, relationships.json and voices.json are unchanged by lore writes.
10. Confirm no semantic entry is created merely because lore is read or formatted.
```

## Current boundary

```text
persistence implementation: COMPLETE
normalization and formatting: COMPLETE
unit tests: PASS
Fabric build/package verification: PASS
NeoForge build: PASS
repository security policy: PASS
prompt integration: NOT INCLUDED
server networking/editor: NOT INCLUDED
client UI: NOT INCLUDED
installed-server acceptance: PENDING with S10 release-candidate run
```
