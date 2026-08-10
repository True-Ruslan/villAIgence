package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphMutation;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class NpcSocialMutationLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void exactServerObservedCauseAppliesOnceAndPersistsStructuredAudit() {
        Path world = world("applied");
        UUID source = id(1);
        UUID target = id(2);
        UUID causeId = id(3);
        appendCause(world, source, target, causeId, MemoryEvent.Type.OBSERVATION,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 100L, 100, 100, 256);

        NpcSocialMutationLifecycleResult first = NpcSocialMutationLifecycle.apply(
                world,
                source,
                target,
                causeId,
                new NpcSocialDelta(9, -2, 1, 4),
                3,
                256,
                5000L,
                npcAuthority(source, target)
        );

        assertEquals(NpcSocialMutationLifecycleResult.Status.APPLIED, first.status());
        assertNotNull(first.graphMutation());
        assertEquals(new NpcSocialState(3, -2, 1, 3), first.graphMutation().after());
        assertNotNull(first.auditEvent());
        assertEquals(first.graphMutation().mutationId(), first.auditEvent().id());
        assertEquals(MemoryEvent.Type.NPC_SOCIAL_CHANGE, first.auditEvent().type());
        assertEquals(causeId, first.auditEvent().npcSocialMutation().causeEventId());
        assertEquals(
                first.auditEvent(),
                MemoryEventStore.forWorld(world)
                        .findById(source, first.graphMutation().mutationId())
                        .orElseThrow()
        );

        NpcSocialMutationLifecycleResult replay = NpcSocialMutationLifecycle.apply(
                world,
                source,
                target,
                causeId,
                new NpcSocialDelta(9, -2, 1, 4),
                3,
                256,
                6000L,
                npcAuthority(source, target)
        );

        assertEquals(NpcSocialMutationLifecycleResult.Status.REPLAYED, replay.status());
        assertNull(replay.auditEvent(), "replay must not manufacture a second audit event");
        assertEquals(new NpcSocialState(3, -2, 1, 3), NpcSocialGraphStore.forWorld(world).get(source, target));
        long auditCount = MemoryEventStore.forWorld(world).getRecent(source, 256).stream()
                .filter(event -> event.type() == MemoryEvent.Type.NPC_SOCIAL_CHANGE)
                .count();
        assertEquals(1L, auditCount);
    }

    @Test
    void actionIsAlsoEligibleServerOwnedCause() {
        Path world = world("action");
        UUID source = id(10);
        UUID target = id(11);
        UUID causeId = id(12);
        appendCause(world, source, target, causeId, MemoryEvent.Type.ACTION,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 200L, 80, 100, 256);

        NpcSocialMutationLifecycleResult result = NpcSocialMutationLifecycle.apply(
                world, source, target, causeId,
                new NpcSocialDelta(0, 2, 0, 1),
                4, 256, 1L, npcAuthority(source, target)
        );

        assertEquals(NpcSocialMutationLifecycleResult.Status.APPLIED, result.status());
        assertEquals(new NpcSocialState(0, 2, 0, 1), NpcSocialGraphStore.forWorld(world).get(source, target));
    }

    @Test
    void missingSourceEvidenceFailsBeforeGraphMutation() {
        Path world = world("missing");
        UUID source = id(20);
        UUID target = id(21);

        NpcSocialMutationLifecycleResult result = NpcSocialMutationLifecycle.apply(
                world, source, target, id(22),
                new NpcSocialDelta(5, 0, 0, 0),
                5, 256, 1L, npcAuthority(source, target)
        );

        assertEquals(NpcSocialMutationLifecycleResult.Status.SOURCE_NOT_RETAINED, result.status());
        assertEquals(NpcSocialState.NEUTRAL, NpcSocialGraphStore.forWorld(world).get(source, target));
        assertNull(result.graphMutation());
        assertNull(result.auditEvent());
    }

    @Test
    void unsupportedTypeProvenanceOrMissingTargetFailsBeforeGraphMutation() {
        Path world = world("invalid-evidence");
        UUID source = id(30);
        UUID target = id(31);
        NpcIdentityAuthority authority = npcAuthority(source, target);

        UUID dialogueCause = id(32);
        appendCause(world, source, target, dialogueCause, MemoryEvent.Type.DIALOGUE,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 300L, 50, 100, 256);
        assertEquals(
                NpcSocialMutationLifecycleResult.Status.INVALID_SOURCE_EVENT,
                apply(world, source, target, dialogueCause, authority).status()
        );

        UUID toldCause = id(33);
        appendCause(world, source, target, toldCause, MemoryEvent.Type.OBSERVATION,
                MemoryEvent.Provenance.PLAYER_TOLD, 301L, 50, 100, 256);
        assertEquals(
                NpcSocialMutationLifecycleResult.Status.INVALID_SOURCE_EVENT,
                apply(world, source, target, toldCause, authority).status()
        );

        UUID missingTargetCause = id(34);
        MemoryEventStore.forWorld(world).append(new MemoryEvent(
                missingTargetCause,
                source,
                MemoryEvent.Type.OBSERVATION,
                "source observed something unrelated",
                List.of(id(99)),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                302L,
                302L,
                50,
                0,
                100,
                List.of()
        ), 256);
        assertEquals(
                NpcSocialMutationLifecycleResult.Status.INVALID_SOURCE_EVENT,
                apply(world, source, target, missingTargetCause, authority).status()
        );

        assertEquals(NpcSocialState.NEUTRAL, NpcSocialGraphStore.forWorld(world).get(source, target));
    }

    @Test
    void nonNpcOrSelfPairFailsBeforeSourceOrGraphMutation() {
        Path world = world("identity");
        UUID source = id(40);
        UUID target = id(41);
        UUID causeId = id(42);
        appendCause(world, source, target, causeId, MemoryEvent.Type.OBSERVATION,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 400L, 50, 100, 256);

        assertEquals(
                NpcSocialMutationLifecycleResult.Status.INVALID_NPC,
                NpcSocialMutationLifecycle.apply(
                        world, source, target, causeId,
                        new NpcSocialDelta(1, 0, 0, 0),
                        4, 256, 1L,
                        npcAuthority(source)
                ).status()
        );
        assertEquals(
                NpcSocialMutationLifecycleResult.Status.INVALID_REQUEST,
                NpcSocialMutationLifecycle.apply(
                        world, source, source, causeId,
                        new NpcSocialDelta(1, 0, 0, 0),
                        4, 256, 1L,
                        npcAuthority(source)
                ).status()
        );
        assertEquals(NpcSocialState.NEUTRAL, NpcSocialGraphStore.forWorld(world).get(source, target));
    }

    @Test
    void nullRequestPartsFailClosed() {
        Path world = world("nulls");
        UUID source = id(50);
        UUID target = id(51);
        UUID cause = id(52);
        NpcIdentityAuthority authority = npcAuthority(source, target);

        assertEquals(NpcSocialMutationLifecycleResult.Status.INVALID_REQUEST,
                NpcSocialMutationLifecycle.apply(null, source, target, cause,
                        NpcSocialDelta.NONE, 4, 256, 1L, authority).status());
        assertEquals(NpcSocialMutationLifecycleResult.Status.INVALID_REQUEST,
                NpcSocialMutationLifecycle.apply(world, null, target, cause,
                        NpcSocialDelta.NONE, 4, 256, 1L, authority).status());
        assertEquals(NpcSocialMutationLifecycleResult.Status.INVALID_REQUEST,
                NpcSocialMutationLifecycle.apply(world, source, null, cause,
                        NpcSocialDelta.NONE, 4, 256, 1L, authority).status());
        assertEquals(NpcSocialMutationLifecycleResult.Status.INVALID_REQUEST,
                NpcSocialMutationLifecycle.apply(world, source, target, null,
                        NpcSocialDelta.NONE, 4, 256, 1L, authority).status());
        assertEquals(NpcSocialMutationLifecycleResult.Status.INVALID_REQUEST,
                NpcSocialMutationLifecycle.apply(world, source, target, cause,
                        null, 4, 256, 1L, authority).status());
        assertEquals(NpcSocialMutationLifecycleResult.Status.INVALID_REQUEST,
                NpcSocialMutationLifecycle.apply(world, source, target, cause,
                        NpcSocialDelta.NONE, 4, 256, 1L, null).status());
    }

    @Test
    void noChangeAndCapacityReachedConsumeCauseWithoutAudit() {
        Path noChangeWorld = world("no-change");
        UUID source = id(60);
        UUID target = id(61);
        UUID noChangeCause = id(62);
        appendCause(noChangeWorld, source, target, noChangeCause, MemoryEvent.Type.OBSERVATION,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 500L, 100, 100, 256);

        NpcSocialMutationLifecycleResult noChange = NpcSocialMutationLifecycle.apply(
                noChangeWorld, source, target, noChangeCause,
                NpcSocialDelta.NONE, 4, 256, 1L, npcAuthority(source, target)
        );
        assertEquals(NpcSocialMutationLifecycleResult.Status.NO_CHANGE, noChange.status());
        assertNull(noChange.auditEvent());
        assertEquals(
                NpcSocialMutationLifecycleResult.Status.REPLAYED,
                NpcSocialMutationLifecycle.apply(
                        noChangeWorld, source, target, noChangeCause,
                        NpcSocialDelta.NONE, 4, 256, 2L, npcAuthority(source, target)
                ).status()
        );

        Path capacityWorld = world("capacity");
        UUID capacitySource = id(70);
        UUID overflowTarget = id(71);
        NpcSocialGraphStore graph = NpcSocialGraphStore.forWorld(capacityWorld);
        for (int index = 0; index < 64; index++) {
            UUID retainedTarget = new UUID(7000L + index, 8000L + index);
            assertEquals(
                    NpcSocialGraphMutation.Status.APPLIED,
                    graph.applyDelta(capacitySource, retainedTarget, new NpcSocialDelta(1, 0, 0, 0), 4).status()
            );
        }
        UUID capacityCause = id(72);
        appendCause(capacityWorld, capacitySource, overflowTarget, capacityCause, MemoryEvent.Type.ACTION,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 600L, 100, 100, 256);

        NpcSocialMutationLifecycleResult capacity = NpcSocialMutationLifecycle.apply(
                capacityWorld, capacitySource, overflowTarget, capacityCause,
                new NpcSocialDelta(1, 0, 0, 0), 4, 256, 1L,
                npcAuthority(capacitySource, overflowTarget)
        );
        assertEquals(NpcSocialMutationLifecycleResult.Status.CAPACITY_REACHED, capacity.status());
        assertNull(capacity.auditEvent());
        assertEquals(NpcSocialState.NEUTRAL, graph.get(capacitySource, overflowTarget));
    }

    @Test
    void appliedMutationReportsWhenBoundedRetentionRejectsAudit() {
        Path world = world("audit-pressure");
        UUID source = id(80);
        UUID target = id(81);
        UUID cause = id(82);
        appendCause(world, source, target, cause, MemoryEvent.Type.OBSERVATION,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 700L, 100, 100, 1);

        NpcSocialMutationLifecycleResult result = NpcSocialMutationLifecycle.apply(
                world, source, target, cause,
                new NpcSocialDelta(2, 0, 0, 0), 4,
                1,
                1234L,
                npcAuthority(source, target)
        );

        assertEquals(NpcSocialMutationLifecycleResult.Status.APPLIED_AUDIT_NOT_RETAINED, result.status());
        assertEquals(new NpcSocialState(2, 0, 0, 0), NpcSocialGraphStore.forWorld(world).get(source, target));
        assertNull(result.auditEvent());
        assertEquals(cause, MemoryEventStore.forWorld(world).getRecent(source, 10).getFirst().id());
    }

    private NpcSocialMutationLifecycleResult apply(
            Path world,
            UUID source,
            UUID target,
            UUID cause,
            NpcIdentityAuthority authority
    ) {
        return NpcSocialMutationLifecycle.apply(
                world, source, target, cause,
                new NpcSocialDelta(1, 0, 0, 0),
                4, 256, 1L, authority
        );
    }

    private void appendCause(
            Path world,
            UUID source,
            UUID target,
            UUID causeId,
            MemoryEvent.Type type,
            MemoryEvent.Provenance provenance,
            long gameTime,
            int importance,
            int confidence,
            int maxEvents
    ) {
        MemoryEventStore.forWorld(world).append(new MemoryEvent(
                causeId,
                source,
                type,
                "server-owned social cause",
                List.of(source, target),
                provenance,
                gameTime,
                gameTime,
                importance,
                0,
                confidence,
                List.of()
        ), maxEvents);
    }

    private Path world(String name) {
        return tempDir.resolve(name);
    }

    private static UUID id(long value) {
        return new UUID(value, value);
    }

    private static NpcIdentityAuthority npcAuthority(UUID... ids) {
        Set<UUID> accepted = Set.of(ids);
        return accepted::contains;
    }
}
