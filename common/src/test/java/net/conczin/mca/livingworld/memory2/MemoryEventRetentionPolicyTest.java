package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryEventRetentionPolicyTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000501");

    @Test
    void durabilityIsMonotonicAcrossImportanceConfidenceAndAbsoluteEmotion() {
        MemoryEvent baseline = event(id(1), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 10, 10, 10);
        MemoryEvent moreImportant = event(id(2), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 20, 10, 10);
        MemoryEvent moreConfident = event(id(3), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 10, 10, 20);
        MemoryEvent moreEmotionalPositive = event(id(4), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 10, 20, 10);
        MemoryEvent moreEmotionalNegative = event(id(5), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 10, -20, 10);

        int baseScore = MemoryEventRetentionPolicy.durabilityScore(baseline);
        assertTrue(MemoryEventRetentionPolicy.durabilityScore(moreImportant) >= baseScore);
        assertTrue(MemoryEventRetentionPolicy.durabilityScore(moreConfident) >= baseScore);
        assertTrue(MemoryEventRetentionPolicy.durabilityScore(moreEmotionalPositive) >= baseScore);
        assertEquals(
                MemoryEventRetentionPolicy.durabilityScore(moreEmotionalPositive),
                MemoryEventRetentionPolicy.durabilityScore(moreEmotionalNegative)
        );
    }

    @Test
    void authoritativeProvenanceIsNeverLessDurableThanOtherwiseEqualToldOrInferredEvents() {
        MemoryEvent system = event(id(10), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 50, 0, 50);
        MemoryEvent player = event(id(11), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.PLAYER_TOLD,
                100L, 50, 0, 50);
        MemoryEvent npc = event(id(12), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.NPC_TOLD,
                100L, 50, 0, 50);
        MemoryEvent inferred = event(id(13), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.INFERRED,
                100L, 50, 0, 50);

        int systemScore = MemoryEventRetentionPolicy.durabilityScore(system);
        assertTrue(systemScore >= MemoryEventRetentionPolicy.durabilityScore(player));
        assertTrue(systemScore >= MemoryEventRetentionPolicy.durabilityScore(npc));
        assertTrue(systemScore >= MemoryEventRetentionPolicy.durabilityScore(inferred));
    }

    @Test
    void eventTypeDurabilityFollowsApprovedSocialHistoryOrder() {
        MemoryEvent dialogue = event(id(20), MemoryEvent.Type.DIALOGUE, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 50, 0, 50);
        MemoryEvent observation = event(id(21), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 50, 0, 50);
        MemoryEvent action = event(id(22), MemoryEvent.Type.ACTION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 50, 0, 50);
        MemoryEvent relationshipChange = event(id(23), MemoryEvent.Type.RELATIONSHIP_CHANGE, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 50, 0, 50);
        MemoryEvent relationshipCause = event(id(24), MemoryEvent.Type.RELATIONSHIP_CAUSE, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 50, 0, 50);

        int dialogueScore = MemoryEventRetentionPolicy.durabilityScore(dialogue);
        int observationScore = MemoryEventRetentionPolicy.durabilityScore(observation);
        int actionScore = MemoryEventRetentionPolicy.durabilityScore(action);
        int changeScore = MemoryEventRetentionPolicy.durabilityScore(relationshipChange);
        int causeScore = MemoryEventRetentionPolicy.durabilityScore(relationshipCause);

        assertEquals(observationScore, actionScore);
        assertTrue(observationScore > dialogueScore);
        assertTrue(changeScore > observationScore);
        assertTrue(causeScore > changeScore);
    }

    @Test
    void effectiveScoreDecaysByAuthoritativeGameTimeAndClampsFutureAge() {
        MemoryEvent value = event(id(30), MemoryEvent.Type.RELATIONSHIP_CAUSE, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                1_000_000L, 80, 40, 100);
        long base = (long) MemoryEventRetentionPolicy.durabilityScore(value)
                * MemoryEventRetentionPolicy.DECAY_STEP_TICKS;

        assertEquals(base, MemoryEventRetentionPolicy.effectiveRetentionScore(value, value.gameTime()));
        assertEquals(
                base - MemoryEventRetentionPolicy.DECAY_STEP_TICKS,
                MemoryEventRetentionPolicy.effectiveRetentionScore(
                        value,
                        value.gameTime() + MemoryEventRetentionPolicy.DECAY_STEP_TICKS
                )
        );
        assertEquals(base, MemoryEventRetentionPolicy.effectiveRetentionScore(value, value.gameTime() - 1L));
    }

    @Test
    void oldImportantObservationSurvivesNewerWeakDialoguePressure() {
        MemoryEvent oldImportant = event(id(40), MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 100, 100, 100);
        MemoryEvent middleWeak = event(id(41), MemoryEvent.Type.DIALOGUE, MemoryEvent.Provenance.PLAYER_TOLD,
                200L, 0, 0, 0);
        MemoryEvent newestWeak = event(id(42), MemoryEvent.Type.DIALOGUE, MemoryEvent.Provenance.PLAYER_TOLD,
                300L, 0, 0, 0);

        List<UUID> retained = MemoryEventRetentionPolicy.selectRetained(
                List.of(newestWeak, oldImportant, middleWeak),
                2,
                300L
        ).stream().map(MemoryEvent::id).toList();

        assertEquals(List.of(oldImportant.id(), newestWeak.id()), retained);
    }

    @Test
    void noEventTypeIsImmortalUnderSufficientGameTimeAge() {
        MemoryEvent strongestOld = event(id(50), MemoryEvent.Type.RELATIONSHIP_CAUSE, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                0L, 100, 100, 100);
        long enoughAge = ((long) MemoryEventRetentionPolicy.durabilityScore(strongestOld) + 1L)
                * MemoryEventRetentionPolicy.DECAY_STEP_TICKS;
        MemoryEvent weakNew = event(id(51), MemoryEvent.Type.DIALOGUE, MemoryEvent.Provenance.INFERRED,
                enoughAge, 0, 0, 0);

        assertTrue(
                MemoryEventRetentionPolicy.effectiveRetentionScore(strongestOld, enoughAge)
                        < MemoryEventRetentionPolicy.effectiveRetentionScore(weakNew, enoughAge)
        );
    }

    @Test
    void selectionIsInputOrderIndependentAndExactTiesUseUuidWithStablePersistenceOrder() {
        UUID smaller = UUID.fromString("00000000-0000-0000-0000-000000000100");
        UUID larger = UUID.fromString("00000000-0000-0000-0000-000000000200");
        MemoryEvent smallerId = event(smaller, MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                500L, 50, 20, 50);
        MemoryEvent largerId = event(larger, MemoryEvent.Type.OBSERVATION, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                500L, 50, 20, 50);
        MemoryEvent olderStrong = event(id(62), MemoryEvent.Type.RELATIONSHIP_CHANGE, MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 90, 20, 90);

        List<UUID> forward = MemoryEventRetentionPolicy.selectRetained(
                List.of(largerId, olderStrong, smallerId), 2, 500L
        ).stream().map(MemoryEvent::id).toList();
        List<UUID> reverse = MemoryEventRetentionPolicy.selectRetained(
                List.of(smallerId, olderStrong, largerId), 2, 500L
        ).stream().map(MemoryEvent::id).toList();

        assertEquals(forward, reverse);
        assertEquals(List.of(olderStrong.id(), smaller), forward);
    }

    private static MemoryEvent event(
            UUID id,
            MemoryEvent.Type type,
            MemoryEvent.Provenance provenance,
            long gameTime,
            int importance,
            int emotionalWeight,
            int confidence
    ) {
        return new MemoryEvent(
                id,
                OWNER,
                type,
                "event-" + id,
                List.of(OWNER),
                provenance,
                gameTime,
                1_700_000_000_000L + gameTime,
                importance,
                emotionalWeight,
                confidence,
                List.of()
        );
    }

    private static UUID id(long value) {
        return new UUID(0L, value);
    }
}
