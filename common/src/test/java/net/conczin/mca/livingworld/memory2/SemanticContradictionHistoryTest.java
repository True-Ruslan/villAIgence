package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticContradictionHistoryTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesCurrentClaimsAndSurvivesSourceUnionConsolidation() {
        Path world = tempDir.resolve("consolidation");
        UUID npc = id(1);
        UUID player = id(90);
        SemanticMemoryEntry fact = fact(id(101), npc, "Gate open", List.of(player), 80, 100, id(901));
        SemanticMemoryEntry belief = belief(id(102), npc, "Gate closed", List.of(player), 50, 50, id(902));
        seed(world, 64, fact, belief);
        SemanticContradictionResult recorded = record(world, npc, fact, belief, 200L);

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> before =
                SemanticContradictionHistory.load(world, npc, player, 8);
        assertEquals(1, before.size());
        assertEquals(recorded.eventId(), before.getFirst().evidence().id());
        assertEquals(SemanticMemoryIdentity.logicalClaimId(fact),
                SemanticMemoryIdentity.logicalClaimId(before.getFirst().first()));

        SemanticMemoryEntry corroboratingFact = fact(
                id(103), npc, " gate   OPEN ", List.of(player), 90, 100, id(903));
        SemanticMemoryStore.forWorld(world).append(corroboratingFact, 64);
        SemanticMemoryEntry mergedFact = SemanticMemoryStore.forWorld(world)
                .findMatching(npc, entry -> SemanticMemoryIdentity.logicalClaimId(entry)
                        .equals(SemanticMemoryIdentity.logicalClaimId(fact)))
                .orElseThrow();
        assertNotEquals(fact.id(), mergedFact.id());
        assertEquals(2, mergedFact.sourceEventIds().size());

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> after =
                SemanticContradictionHistory.load(world, npc, player, 8);
        assertEquals(1, after.size());
        assertEquals(mergedFact.id(), after.getFirst().first().id());
        assertEquals(recorded.eventId(), after.getFirst().evidence().id());
    }

    @Test
    void forgottenLiveClaimHidesHistoricalRelationWithoutResurrectingText() {
        Path world = tempDir.resolve("forgetting");
        UUID npc = id(10);
        SemanticMemoryEntry weak = belief(id(110), npc, "Mine unsafe", List.of(), 5, 5, id(910));
        SemanticMemoryEntry strong = fact(id(111), npc, "Mine safe", List.of(), 100, 100, id(911));
        seed(world, 64, weak, strong);
        SemanticContradictionResult recorded = record(world, npc, weak, strong, 100L);
        assertEquals(1, SemanticContradictionHistory.load(world, npc, id(90), 8).size());

        SemanticMemoryEntry replacement = fact(id(112), npc, "Town bell repaired", List.of(), 100, 100, id(912));
        SemanticMemoryStore.forWorld(world).append(replacement, 2);

        assertTrue(SemanticMemoryStore.forWorld(world).findById(npc, weak.id()).isEmpty());
        assertTrue(MemoryEventStore.forWorld(world).findById(npc, recorded.eventId()).isPresent());
        assertTrue(SemanticContradictionHistory.load(world, npc, id(90), 8).isEmpty());
    }

    @Test
    void privacyFiltersBeforeLimitAcrossGlobalPrivateAndSharedScopes() {
        UUID playerA = id(90);
        UUID playerB = id(91);
        UUID entity = id(92);

        assertVisibility(tempDir.resolve("global"), 1000, List.of(), playerA, playerB, true, true);
        assertVisibility(tempDir.resolve("private"), 2000, List.of(playerA), playerA, playerB, true, false);
        assertVisibility(tempDir.resolve("shared"), 3000, List.of(entity, playerA), playerA, playerB, true, false);

        Path filtered = tempDir.resolve("filter-before-limit");
        UUID npc = id(4000);
        SemanticMemoryEntry visibleA = belief(id(4001), npc, "Visible A", List.of(playerA), 50, 50, id(4101));
        SemanticMemoryEntry visibleB = belief(id(4002), npc, "Visible B", List.of(playerA), 50, 50, id(4102));
        SemanticMemoryEntry foreignA = belief(id(4003), npc, "Foreign A", List.of(playerB), 50, 50, id(4103));
        SemanticMemoryEntry foreignB = belief(id(4004), npc, "Foreign B", List.of(playerB), 50, 50, id(4104));
        seed(filtered, 64, visibleA, visibleB, foreignA, foreignB);
        SemanticContradictionResult visible = record(filtered, npc, visibleA, visibleB, 100L);
        record(filtered, npc, foreignA, foreignB, 900L);

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> result =
                SemanticContradictionHistory.load(filtered, npc, playerA, 1);
        assertEquals(1, result.size());
        assertEquals(visible.eventId(), result.getFirst().evidence().id());
    }

    @Test
    void newestFirstThenEventUuidAscendingAndMalformedEvidenceIsIgnored() {
        Path world = tempDir.resolve("ordering");
        UUID npc = id(20);
        SemanticMemoryEntry a = belief(id(201), npc, "A true", List.of(), 50, 50, id(920));
        SemanticMemoryEntry b = belief(id(202), npc, "A false", List.of(), 50, 50, id(921));
        SemanticMemoryEntry c = belief(id(203), npc, "B true", List.of(), 50, 50, id(922));
        SemanticMemoryEntry d = belief(id(204), npc, "B false", List.of(), 50, 50, id(923));
        seed(world, 64, a, b, c, d);
        SemanticContradictionResult first = record(world, npc, a, b, 500L);
        SemanticContradictionResult second = record(world, npc, c, d, 500L);

        MemoryEvent valid = MemoryEventStore.forWorld(world).findById(npc, first.eventId()).orElseThrow();
        MemoryEvent malformed = new MemoryEvent(
                id(999), valid.ownerNpcId(), valid.type(), "tampered", valid.participants(), valid.provenance(),
                valid.gameTime() + 100L, valid.createdAtEpochMillis(), valid.importance(), valid.emotionalWeight(),
                valid.confidence(), valid.relationshipReasons(), valid.dialogue(), valid.relationshipTransition(),
                valid.relationshipCause(), valid.knowledgeTransferProvenance(), valid.semanticContradiction());
        MemoryEventStore.forWorld(world).append(malformed, 64);

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> result =
                SemanticContradictionHistory.load(world, npc, id(90), 8);
        assertEquals(2, result.size());
        List<UUID> expected = new ArrayList<>(List.of(first.eventId(), second.eventId()));
        expected.sort(Comparator.comparing(UUID::toString));
        assertEquals(expected, result.stream().map(entry -> entry.evidence().id()).toList());
        assertFalse(result.stream().anyMatch(entry -> entry.evidence().id().equals(malformed.id())));
    }

    private void assertVisibility(
            Path world,
            int base,
            List<UUID> scope,
            UUID playerA,
            UUID playerB,
            boolean visibleA,
            boolean visibleB
    ) {
        UUID npc = id(base);
        SemanticMemoryEntry first = belief(id(base + 1), npc, "Claim one " + base, scope, 50, 50, id(base + 101));
        SemanticMemoryEntry second = belief(id(base + 2), npc, "Claim two " + base, scope, 50, 50, id(base + 102));
        seed(world, 64, first, second);
        record(world, npc, first, second, 100L);
        assertEquals(visibleA, !SemanticContradictionHistory.load(world, npc, playerA, 8).isEmpty());
        assertEquals(visibleB, !SemanticContradictionHistory.load(world, npc, playerB, 8).isEmpty());
    }

    private static SemanticContradictionResult record(
            Path world,
            UUID npc,
            SemanticMemoryEntry first,
            SemanticMemoryEntry second,
            long gameTime
    ) {
        SemanticContradictionResult result = SemanticContradictionLifecycle.record(
                world, npc, first.id(), second.id(), gameTime, 64);
        assertEquals(SemanticContradictionResult.Status.RECORDED, result.status());
        return result;
    }

    private static void seed(Path world, int capacity, SemanticMemoryEntry... entries) {
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        for (SemanticMemoryEntry entry : entries) store.append(entry, capacity);
    }

    private static SemanticMemoryEntry fact(
            UUID id,
            UUID owner,
            String statement,
            List<UUID> scope,
            int importance,
            int confidence,
            UUID source
    ) {
        return new SemanticMemoryEntry(
                id, owner, SemanticMemoryEntry.Kind.FACT, statement, scope,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 10L, 0L, importance, confidence, List.of(source));
    }

    private static SemanticMemoryEntry belief(
            UUID id,
            UUID owner,
            String statement,
            List<UUID> scope,
            int importance,
            int confidence,
            UUID source
    ) {
        return new SemanticMemoryEntry(
                id, owner, SemanticMemoryEntry.Kind.BELIEF, statement, scope,
                MemoryEvent.Provenance.NPC_TOLD, 10L, 0L, importance, confidence, List.of(source));
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
