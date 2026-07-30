package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SemanticMemoryRetentionPolicyTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void calculatesDurabilityFromAllExistingSemanticSignals() {
        assertEquals(25, SemanticMemoryRetentionPolicy.durabilityScore(
                belief(id(1), MemoryEvent.Provenance.INFERRED, 0L, 0, 0, 0)
        ));
        assertEquals(425, SemanticMemoryRetentionPolicy.durabilityScore(
                belief(id(2), MemoryEvent.Provenance.INFERRED, 0L, 100, 0, 0)
        ));
        assertEquals(275, SemanticMemoryRetentionPolicy.durabilityScore(
                belief(id(3), MemoryEvent.Provenance.INFERRED, 0L, 0, 100, 0)
        ));
        assertEquals(200, SemanticMemoryRetentionPolicy.durabilityScore(
                fact(id(4), 0L, 0, 0, 0)
        ));
        assertEquals(100, SemanticMemoryRetentionPolicy.durabilityScore(
                belief(id(5), MemoryEvent.Provenance.PLAYER_TOLD, 0L, 0, 0, 0)
        ));
        assertEquals(75, SemanticMemoryRetentionPolicy.durabilityScore(
                belief(id(6), MemoryEvent.Provenance.NPC_TOLD, 0L, 0, 0, 0)
        ));
        assertEquals(175, SemanticMemoryRetentionPolicy.durabilityScore(
                belief(id(7), MemoryEvent.Provenance.INFERRED, 0L, 0, 0, 6)
        ));
        assertEquals(175, SemanticMemoryRetentionPolicy.durabilityScore(
                belief(id(8), MemoryEvent.Provenance.INFERRED, 0L, 0, 0, 8)
        ));
    }

    @Test
    void appliesDeterministicGameTimeDecayAndClampsFutureAgeToZero() {
        SemanticMemoryEntry entry = fact(id(10), 1_000_000L, 50, 80, 2);
        long base = (long) SemanticMemoryRetentionPolicy.durabilityScore(entry)
                * SemanticMemoryRetentionPolicy.DECAY_STEP_TICKS;

        assertEquals(base, SemanticMemoryRetentionPolicy.effectiveRetentionScore(entry, entry.gameTime()));
        assertEquals(
                base - SemanticMemoryRetentionPolicy.DECAY_STEP_TICKS,
                SemanticMemoryRetentionPolicy.effectiveRetentionScore(
                        entry,
                        entry.gameTime() + SemanticMemoryRetentionPolicy.DECAY_STEP_TICKS
                )
        );
        assertEquals(base, SemanticMemoryRetentionPolicy.effectiveRetentionScore(entry, entry.gameTime() - 1L));
    }

    @Test
    void keepsOlderImportantKnowledgeOverNewerWeakKnowledge() {
        SemanticMemoryEntry olderStrong = fact(id(20), 100L, 100, 100, 1);
        SemanticMemoryEntry newerWeak = fact(
                id(21),
                100L + SemanticMemoryRetentionPolicy.DECAY_STEP_TICKS * 10L,
                0,
                0,
                1
        );

        List<SemanticMemoryEntry> retained = SemanticMemoryRetentionPolicy.selectRetained(
                List.of(newerWeak, olderStrong),
                1,
                newerWeak.gameTime()
        );

        assertEquals(List.of(olderStrong.id()), retained.stream().map(SemanticMemoryEntry::id).toList());
    }

    @Test
    void confidenceAndIndependentEvidenceAffectRetentionWithoutChangingImportance() {
        SemanticMemoryEntry highConfidence = fact(id(30), 100L, 40, 100, 1);
        SemanticMemoryEntry lowConfidence = fact(id(31), 200L, 40, 0, 1);
        SemanticMemoryEntry corroborated = fact(id(32), 100L, 40, 50, 6);
        SemanticMemoryEntry singleSource = fact(id(33), 200L, 40, 50, 1);

        assertEquals(
                List.of(highConfidence.id()),
                SemanticMemoryRetentionPolicy.selectRetained(
                        List.of(lowConfidence, highConfidence), 1, 200L
                ).stream().map(SemanticMemoryEntry::id).toList()
        );
        assertEquals(
                List.of(corroborated.id()),
                SemanticMemoryRetentionPolicy.selectRetained(
                        List.of(singleSource, corroborated), 1, 200L
                ).stream().map(SemanticMemoryEntry::id).toList()
        );
    }

    @Test
    void authoritativeProvenanceOutranksToldAndInferredBeliefs() {
        SemanticMemoryEntry fact = fact(id(40), 100L, 20, 20, 1);
        SemanticMemoryEntry playerTold = belief(
                id(41), MemoryEvent.Provenance.PLAYER_TOLD, 100L, 20, 20, 1
        );
        SemanticMemoryEntry npcTold = belief(
                id(42), MemoryEvent.Provenance.NPC_TOLD, 100L, 20, 20, 1
        );
        SemanticMemoryEntry inferred = belief(
                id(43), MemoryEvent.Provenance.INFERRED, 100L, 20, 20, 1
        );

        List<SemanticMemoryEntry> retained = SemanticMemoryRetentionPolicy.selectRetained(
                List.of(inferred, npcTold, playerTold, fact),
                1,
                100L
        );

        assertEquals(List.of(fact.id()), retained.stream().map(SemanticMemoryEntry::id).toList());
    }

    @Test
    void selectionIsInputOrderIndependentAndReturnsPersistenceStableOrder() {
        SemanticMemoryEntry oldestStrong = fact(id(50), 100L, 90, 90, 2);
        SemanticMemoryEntry middle = fact(id(51), 200L, 50, 50, 1);
        SemanticMemoryEntry newestWeak = fact(id(52), 300L, 0, 0, 1);

        List<UUID> forward = ids(SemanticMemoryRetentionPolicy.selectRetained(
                List.of(oldestStrong, middle, newestWeak), 2, 300L
        ));
        List<UUID> reverse = ids(SemanticMemoryRetentionPolicy.selectRetained(
                List.of(newestWeak, middle, oldestStrong), 2, 300L
        ));

        assertEquals(forward, reverse);
        assertEquals(List.of(oldestStrong.id(), middle.id()), forward);
    }

    @Test
    void keepsAllUniqueEntriesUnderCapacityAndBreaksExactTiesByUuid() {
        SemanticMemoryEntry first = fact(id(60), 100L, 50, 50, 1);
        SemanticMemoryEntry second = fact(id(61), 200L, 50, 50, 1);
        List<SemanticMemoryEntry> underCapacity = SemanticMemoryRetentionPolicy.selectRetained(
                List.of(second, first, first),
                4,
                200L
        );

        assertEquals(List.of(first.id(), second.id()), ids(underCapacity));

        UUID smaller = UUID.fromString("00000000-0000-0000-0000-000000000100");
        UUID larger = UUID.fromString("00000000-0000-0000-0000-000000000200");
        SemanticMemoryEntry smallerId = fact(smaller, 500L, 50, 50, 1);
        SemanticMemoryEntry largerId = fact(larger, 500L, 50, 50, 1);

        assertEquals(
                List.of(smaller),
                ids(SemanticMemoryRetentionPolicy.selectRetained(
                        List.of(largerId, smallerId), 1, 500L
                ))
        );
    }

    private static SemanticMemoryEntry fact(
            UUID id,
            long gameTime,
            int importance,
            int confidence,
            int sourceCount
    ) {
        return entry(
                id,
                SemanticMemoryEntry.Kind.FACT,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                importance,
                confidence,
                sourceCount
        );
    }

    private static SemanticMemoryEntry belief(
            UUID id,
            MemoryEvent.Provenance provenance,
            long gameTime,
            int importance,
            int confidence,
            int sourceCount
    ) {
        return entry(
                id,
                SemanticMemoryEntry.Kind.BELIEF,
                provenance,
                gameTime,
                importance,
                confidence,
                sourceCount
        );
    }

    private static SemanticMemoryEntry entry(
            UUID id,
            SemanticMemoryEntry.Kind kind,
            MemoryEvent.Provenance provenance,
            long gameTime,
            int importance,
            int confidence,
            int sourceCount
    ) {
        List<UUID> sourceIds = new ArrayList<>();
        for (int index = 0; index < sourceCount; index++) {
            sourceIds.add(new UUID(1L, index + 1L));
        }
        return new SemanticMemoryEntry(
                id,
                OWNER,
                kind,
                "memory-" + id,
                List.of(OWNER),
                provenance,
                gameTime,
                1_700_000_000_000L + gameTime,
                importance,
                confidence,
                sourceIds
        );
    }

    private static UUID id(long value) {
        return new UUID(0L, value);
    }

    private static List<UUID> ids(List<SemanticMemoryEntry> entries) {
        return entries.stream()
                .sorted(Comparator
                        .comparingLong(SemanticMemoryEntry::gameTime)
                        .thenComparingLong(SemanticMemoryEntry::createdAtEpochMillis)
                        .thenComparing(entry -> entry.id().toString()))
                .map(SemanticMemoryEntry::id)
                .toList();
    }
}
