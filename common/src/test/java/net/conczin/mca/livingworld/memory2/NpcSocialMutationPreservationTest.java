package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import net.conczin.mca.livingworld.relationship.NpcSocialCausalMutation;
import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSocialMutationPreservationTest {
    @TempDir
    Path tempDir;

    @Test
    void exactLifecycleReplayIsByteIdempotentAndDoesNotDuplicateAudit() throws Exception {
        Path world = tempDir.resolve("replay-world");
        UUID source = id(1);
        UUID target = id(2);
        UUID cause = id(3);
        appendCause(world, source, target, cause, 100L, 256);

        NpcIdentityAuthority authority = npcAuthority(source, target);
        NpcSocialMutationLifecycleResult first = NpcSocialMutationLifecycle.apply(
                world, source, target, cause,
                new NpcSocialDelta(4, -1, 2, 3),
                4, 256, 1_000L, authority
        );
        assertEquals(NpcSocialMutationLifecycleResult.Status.APPLIED, first.status());

        Path graphFile = world.resolve("livingworld/npc-social-graph.json");
        byte[] graphBeforeReplay = Files.readAllBytes(graphFile);
        long auditCountBefore = auditCount(world, source);

        NpcSocialMutationLifecycleResult replay = NpcSocialMutationLifecycle.apply(
                world, source, target, cause,
                new NpcSocialDelta(4, -1, 2, 3),
                4, 256, 2_000L, authority
        );

        assertEquals(NpcSocialMutationLifecycleResult.Status.REPLAYED, replay.status());
        assertArrayEquals(graphBeforeReplay, Files.readAllBytes(graphFile));
        assertEquals(auditCountBefore, auditCount(world, source));
        assertEquals(1L, auditCountBefore);
        assertEquals(first.graphMutation().after(), NpcSocialGraphStore.forWorld(world).get(source, target));
    }

    @Test
    void forgottenSourceAndAuditDoNotRollbackOrBypassAtomicGraphReplayGuard() {
        Path world = tempDir.resolve("forgetting-world");
        UUID source = id(10);
        UUID target = id(11);
        UUID cause = id(12);
        appendCause(world, source, target, cause, 200L, 256);
        NpcIdentityAuthority authority = npcAuthority(source, target);

        NpcSocialMutationLifecycleResult applied = NpcSocialMutationLifecycle.apply(
                world, source, target, cause,
                new NpcSocialDelta(3, 1, 0, -1),
                4, 256, 3_000L, authority
        );
        assertEquals(NpcSocialMutationLifecycleResult.Status.APPLIED, applied.status());
        NpcSocialState expected = applied.graphMutation().after();
        UUID auditId = applied.auditEvent().id();

        MemoryEventStore memory = MemoryEventStore.forWorld(world);
        memory.append(new MemoryEvent(
                id(13),
                source,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                "stronger retained replacement",
                List.of(target),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10_000L,
                10_000L,
                100,
                100,
                100,
                List.of()
        ), 1);

        assertTrue(memory.findById(source, cause).isEmpty());
        assertTrue(memory.findById(source, auditId).isEmpty());

        NpcSocialMutationLifecycleResult lifecycleRetry = NpcSocialMutationLifecycle.apply(
                world, source, target, cause,
                new NpcSocialDelta(3, 1, 0, -1),
                4, 256, 4_000L, authority
        );
        assertEquals(NpcSocialMutationLifecycleResult.Status.SOURCE_NOT_RETAINED, lifecycleRetry.status());
        assertEquals(expected, NpcSocialGraphStore.forWorld(world).get(source, target));
        assertTrue(memory.findById(source, auditId).isEmpty(), "retry must not resurrect forgotten audit history");

        NpcSocialCausalMutation lowLevelReplay = NpcSocialGraphStore.forWorld(world).applyCausalDelta(
                source,
                target,
                cause,
                200L,
                new NpcSocialDelta(3, 1, 0, -1),
                4
        );
        assertEquals(NpcSocialCausalMutation.Status.REPLAYED, lowLevelReplay.status());
        assertEquals(expected, NpcSocialGraphStore.forWorld(world).get(source, target));
    }

    @Test
    void socialLifecycleDoesNotMutateNpcPlayerRelationshipsOrCreateSemanticStore() throws Exception {
        Path world = tempDir.resolve("compat-world");
        UUID source = id(20);
        UUID target = id(21);
        UUID player = id(22);
        UUID cause = id(23);

        LivingWorldRelationshipStore relationships = LivingWorldRelationshipStore.forWorld(world);
        relationships.applyDelta(
                source,
                player,
                new LivingWorldRelationshipDelta(7, 3, -2, 5),
                10
        );
        Path relationshipFile = world.resolve("livingworld/relationships.json");
        byte[] relationshipBefore = Files.readAllBytes(relationshipFile);
        Path semanticFile = world.resolve("livingworld/semantic-memory.json");
        assertFalse(Files.exists(semanticFile));

        appendCause(world, source, target, cause, 300L, 256);
        assertEquals(
                NpcSocialMutationLifecycleResult.Status.APPLIED,
                NpcSocialMutationLifecycle.apply(
                        world, source, target, cause,
                        new NpcSocialDelta(2, 1, 0, 2),
                        4, 256, 5_000L, npcAuthority(source, target)
                ).status()
        );

        assertArrayEquals(relationshipBefore, Files.readAllBytes(relationshipFile));
        assertEquals(
                new LivingWorldRelationshipState(7, 3, -2, 5),
                relationships.get(source, player)
        );
        assertFalse(Files.exists(semanticFile), "social process evidence must not create Semantic FACT/BELIEF state");
    }

    @Test
    void frontierWithoutAuditIsNotBackfilledByLifecycleReplay() {
        Path world = tempDir.resolve("crash-window-world");
        UUID source = id(30);
        UUID target = id(31);
        UUID cause = id(32);
        appendCause(world, source, target, cause, 400L, 256);

        NpcSocialCausalMutation graphOnly = NpcSocialGraphStore.forWorld(world).applyCausalDelta(
                source,
                target,
                cause,
                400L,
                new NpcSocialDelta(2, 0, 1, 0),
                4
        );
        assertEquals(NpcSocialCausalMutation.Status.APPLIED, graphOnly.status());
        assertEquals(0L, auditCount(world, source));

        NpcSocialMutationLifecycleResult replay = NpcSocialMutationLifecycle.apply(
                world, source, target, cause,
                new NpcSocialDelta(2, 0, 1, 0),
                4, 256, 6_000L, npcAuthority(source, target)
        );

        assertEquals(NpcSocialMutationLifecycleResult.Status.REPLAYED, replay.status());
        assertEquals(0L, auditCount(world, source), "Memory audit is not the transaction ledger and is not reconstructed");
        assertEquals(graphOnly.after(), NpcSocialGraphStore.forWorld(world).get(source, target));
    }

    @Test
    void multiSourceOrderedPressureSurvivesFreshRootReload() throws Exception {
        Path world = tempDir.resolve("pressure-world-a");
        List<Expectation> expectations = new ArrayList<>();

        for (int sourceIndex = 0; sourceIndex < 12; sourceIndex++) {
            UUID source = new UUID(100L + sourceIndex, 1_000L + sourceIndex);
            UUID target = new UUID(10_000L + sourceIndex, 20_000L + sourceIndex);
            NpcIdentityAuthority authority = npcAuthority(source, target);

            UUID firstCause = new UUID(30_000L + sourceIndex * 2L, 40_000L + sourceIndex * 2L);
            UUID secondCause = new UUID(30_001L + sourceIndex * 2L, 40_001L + sourceIndex * 2L);
            appendCause(world, source, target, firstCause, 1_000L + sourceIndex * 10L, 512);
            appendCause(world, source, target, secondCause, 1_001L + sourceIndex * 10L, 512);

            assertEquals(
                    NpcSocialMutationLifecycleResult.Status.APPLIED,
                    NpcSocialMutationLifecycle.apply(
                            world, source, target, firstCause,
                            new NpcSocialDelta(1, 0, 0, 1),
                            4, 512, 7_000L + sourceIndex, authority
                    ).status()
            );
            NpcSocialMutationLifecycleResult second = NpcSocialMutationLifecycle.apply(
                    world, source, target, secondCause,
                    new NpcSocialDelta(2, 1, 0, 0),
                    4, 512, 8_000L + sourceIndex, authority
            );
            assertEquals(NpcSocialMutationLifecycleResult.Status.APPLIED, second.status());

            assertEquals(
                    NpcSocialMutationLifecycleResult.Status.STALE_CAUSE,
                    NpcSocialMutationLifecycle.apply(
                            world, source, target, firstCause,
                            new NpcSocialDelta(1, 0, 0, 1),
                            4, 512, 9_000L + sourceIndex, authority
                    ).status()
            );
            expectations.add(new Expectation(source, target, second.graphMutation().after(), secondCause));
        }

        Path freshRoot = tempDir.resolve("pressure-world-b");
        Files.createDirectories(freshRoot.resolve("livingworld"));
        Files.copy(
                world.resolve("livingworld/npc-social-graph.json"),
                freshRoot.resolve("livingworld/npc-social-graph.json"),
                StandardCopyOption.REPLACE_EXISTING
        );
        Files.copy(
                world.resolve("livingworld/memory2.json"),
                freshRoot.resolve("livingworld/memory2.json"),
                StandardCopyOption.REPLACE_EXISTING
        );

        NpcSocialGraphStore reloadedGraph = NpcSocialGraphStore.forWorld(freshRoot);
        for (Expectation expected : expectations) {
            assertEquals(expected.state(), reloadedGraph.get(expected.source(), expected.target()));
            assertEquals(
                    expected.latestCause(),
                    reloadedGraph.latestCausalMutation(expected.source()).orElseThrow().causeEventId()
            );
            assertEquals(2L, auditCount(freshRoot, expected.source()));
        }
    }

    private void appendCause(Path world, UUID source, UUID target, UUID cause, long gameTime, int maxEvents) {
        MemoryEventStore.forWorld(world).append(new MemoryEvent(
                cause,
                source,
                MemoryEvent.Type.OBSERVATION,
                "server-owned social cause",
                List.of(source, target),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                gameTime,
                80,
                0,
                100,
                List.of()
        ), maxEvents);
    }

    private static long auditCount(Path world, UUID source) {
        return MemoryEventStore.forWorld(world).getRecent(source, 512).stream()
                .filter(event -> event.type() == MemoryEvent.Type.NPC_SOCIAL_CHANGE)
                .count();
    }

    private static NpcIdentityAuthority npcAuthority(UUID... ids) {
        Set<UUID> accepted = Set.of(ids);
        return accepted::contains;
    }

    private static UUID id(long value) {
        return new UUID(value, value);
    }

    private record Expectation(UUID source, UUID target, NpcSocialState state, UUID latestCause) {
    }
}
