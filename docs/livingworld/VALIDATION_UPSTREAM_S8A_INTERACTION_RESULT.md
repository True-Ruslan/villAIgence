# MCA 7.7.32 Selective Synchronization — S8a Relationship Gift Results

**VillAIgence base:** `8dccf686f5f8ab245979e7ab64e1d75b87d14623`  
**Upstream target:** `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`  
**Source commit:** `792a1cfe801c985915f570e59f5f2293ef205f29`

## Contract

```text
InteractionResult.PASS    -> not handled; continue normal gift processing; consume 0
InteractionResult.FAIL    -> handled rejection; consume 0
InteractionResult.CONSUME -> handled success; consume exactly 1
other non-PASS result     -> handled; consume 0 unless explicitly CONSUME
```

## Scope

Changed only:

- special gift result interface and loader-independent dispatch policy;
- bouquet, engagement ring, wedding ring and matchmaker ring return values;
- the special-gift dispatch block in `BreedableRelationship`;
- focused unit tests.

The ordinary gift analysis, saturation, inventory, mood, hearts, cake, infection, dye and age-up branches remain unchanged.

## TDD evidence

### Invalid initial RED boundary

The first test head `3c0c536b5b7646161b8277d44f0a5b6902913950` imported Minecraft API directly into common unit tests. CI #1058 correctly demonstrated that the test boundary was loader-coupled, so it was replaced before production code.

### Canonical RED

```text
head: 7991b6fb22d7b77a03c9b24bb7c12a9f66f6a3e2
VillAIgence CI #1059 / 30699938253 — EXPECTED FAILURE
boundary: common:compileTestJava
reason: SpecialGiftResultPolicy absent at all test call sites
Java Pull Request CI #571 — SUCCESS
Repository security policy #323 — SUCCESS
```

### GREEN production head

```text
head: ac1fd608bed7d9b640f00579107c9748ccff1221
VillAIgence CI #1068 / 30700220548              SUCCESS
Java Pull Request CI #580 / 30700219267       SUCCESS
Repository security policy #341 / 30700220106 SUCCESS
```

## Accumulated installed-server acceptance

```text
1. Offer each relationship item under an invalid condition (baby, relative, low hearts, married, engaged elsewhere, incompatible).
   Expected: rejection dialogue, item count unchanged, no relationship mutation.
2. Offer bouquet to an eligible non-partner.
   Expected: promise created, mood +5, exactly one bouquet consumed.
3. Offer bouquet to an existing romantic partner.
   Expected: special handler returns PASS and ordinary gift logic evaluates the bouquet.
4. Offer engagement ring to an eligible player/NPC pair.
   Expected: engagement created, mood +10, exactly one ring consumed.
5. Offer engagement ring to an already engaged pair.
   Expected: failure dialogue, no item consumed.
6. Offer wedding ring to an eligible engaged pair.
   Expected: marriage created, mood +15, exactly one ring consumed.
7. Use matchmaker ring with fewer than two rings, invalid target, or no nearby partner.
   Expected: failure dialogue, no automatic special-gift consumption.
8. Use matchmaker ring successfully in survival and creative modes.
   Expected: two-NPC marriage; legacy explicit survival deduction plus dispatcher consumption preserve the intended total cost; creative behavior remains unchanged.
9. Confirm ordinary gifts, cake, golden apple, dyes, wet sponge and child age-up still behave as before.
```

## Current boundary

```text
implementation: COMPLETE
unit tests: PASS
Fabric build/package verification: PASS
NeoForge build: PASS
repository security policy: PASS
installed-server acceptance: PENDING cumulative release-candidate run
```
