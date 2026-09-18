package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementKnowledgeFlowLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void cycleDelegatesToExactNpcTransferAndCreatesOnlyListenerNpcToldBelief() {
        Path world = tempDir.resolve("delegation");
        UUID speaker = id(1);
        UUID listener = id(2);
        UUID player = id(90);
        UUID sourceId = id(100);
        UUID sourceEvidence = id(101);
        long cycleTime = 2_400L;

        SemanticMemoryEntry source = new SemanticMemoryEntry(
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                "The north bridge is destroyed",
                List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L,
                0L,
                91,
                100,
                List.of(sourceEvidence)
        );
        SemanticMemoryStore.forWorld(world).append(source, 64);

        SettlementKnowledgeFlowLifecycle.CycleResult result = SettlementKnowledgeFlowLifecycle.runCycle(
                world,
                7,
                cycleTime,
                List.of(speaker, listener),
                64,
                64
        );

        assertEquals(1, result.opportunities());
        assertEquals(0, result.sociallySuppressedTransfers());
        assertEquals(1, result.attemptedTransfers());
        assertEquals(1, result.successfulTransfers());
        assertEquals(List.of(NpcKnowledgeTransferResult.Status.ADMITTED), result.statuses());

        List<SemanticMemoryEntry> sourceMemory = SemanticMemoryStore.forWorld(world).getRecent(speaker, 64);
        assertEquals(1, sourceMemory.size());
        assertEquals(SemanticMemoryEntry.Kind.FACT, sourceMemory.getFirst().kind());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, sourceMemory.getFirst().provenance());
        assertEquals(100, sourceMemory.getFirst().confidence());

        List<SemanticMemoryEntry> listenerMemory = SemanticMemoryStore.forWorld(world).getRecent(listener, 64);
        assertEquals(1, listenerMemory.size());
        SemanticMemoryEntry transferred = listenerMemory.getFirst();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, transferred.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, transferred.provenance());
        assertEquals("The north bridge is destroyed", transferred.statement());
        assertEquals(List.of(player), transferred.relatedEntities());
        assertEquals(50, transferred.importance());
        assertEquals(50, transferred.confidence());
        assertFalse(listenerMemory.stream().anyMatch(entry -> entry.kind() == SemanticMemoryEntry.Kind.FACT));

        MemoryEvent evidence = MemoryEventStore.forWorld(world).findById(
                listener,
                NpcToldDialogueAdapter.deterministicEvidenceId(speaker, listener, sourceId, cycleTime)
        ).orElseThrow();
        assertEquals(MemoryEvent.Type.DIALOGUE, evidence.type());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, evidence.provenance());
        assertEquals(List.of(listener, speaker), evidence.participants());
        assertEquals(List.of(player), transferred.relatedEntities());
    }

    @Test
    void positiveDirectedRouteIsPreferredOverLegacyNeutralTarget() {
        Path world = tempDir.resolve("social-routing-preference");
        UUID speaker = id(150);
        UUID listenerA = id(151);
        UUID listenerB = id(152);
        UUID listenerC = id(153);
        UUID sourceId = id(154);
        long cycleTime = 3_600L;
        List<UUID> residents = List.of(speaker, listenerA, listenerB, listenerC);

        appendSourceFact(world, speaker, sourceId, "The western mill reopened");

        SettlementKnowledgeFlowSelector.SelectionResult legacySelection =
                SettlementKnowledgeFlowSelector.select(
                        SemanticMemoryStore.forWorld(world),
                        21,
                        cycleTime,
                        residents
                );
        SettlementKnowledgeFlowSelector.Opportunity legacyOpportunity =
                legacySelection.opportunities().stream()
                        .filter(value -> value.sourceSemanticEntryId().equals(sourceId))
                        .findFirst()
                        .orElseThrow();
        UUID legacyNeutralTarget = legacyOpportunity.listenerNpcId();
        UUID respectedTarget = residents.stream()
                .filter(id -> !id.equals(speaker))
                .filter(id -> !id.equals(legacyNeutralTarget))
                .findFirst()
                .orElseThrow();

        NpcSocialGraphStore.forWorld(world).applyDelta(
                speaker,
                respectedTarget,
                new NpcSocialDelta(0, 80, 0, 0),
                100
        );

        SettlementKnowledgeFlowLifecycle.CycleResult result =
                SettlementKnowledgeFlowLifecycle.runCycle(
                        world,
                        21,
                        cycleTime,
                        residents,
                        64,
                        64
                );

        assertEquals(1, result.successfulTransfers());
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(respectedTarget, 64).stream()
                .anyMatch(entry -> SemanticMemoryIdentity.canonicalStatement(entry.statement())
                        .equals("the western mill reopened")));
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(legacyNeutralTarget, 64).isEmpty());
    }

    @Test
    void sameCycleSocialChangeCannotRetargetSuccessfulSourceToSecondListener() {
        Path world = tempDir.resolve("social-routing-replay");
        UUID speaker = id(160);
        UUID listenerA = id(161);
        UUID listenerB = id(162);
        UUID listenerC = id(163);
        UUID sourceId = id(164);
        long cycleTime = 4_200L;
        List<UUID> residents = List.of(speaker, listenerA, listenerB, listenerC);

        appendSourceFact(world, speaker, sourceId, "The northern ford is passable");

        SettlementKnowledgeFlowSelector.SelectionResult legacySelection =
                SettlementKnowledgeFlowSelector.select(
                        SemanticMemoryStore.forWorld(world),
                        22,
                        cycleTime,
                        residents
                );
        UUID legacyNeutralTarget = legacySelection.opportunities().stream()
                .filter(value -> value.sourceSemanticEntryId().equals(sourceId))
                .findFirst()
                .orElseThrow()
                .listenerNpcId();
        List<UUID> alternates = residents.stream()
                .filter(id -> !id.equals(speaker))
                .filter(id -> !id.equals(legacyNeutralTarget))
                .toList();
        UUID firstPositiveTarget = alternates.get(0);
        UUID secondPositiveTarget = alternates.get(1);

        NpcSocialGraphStore social = NpcSocialGraphStore.forWorld(world);
        social.applyDelta(
                speaker,
                firstPositiveTarget,
                new NpcSocialDelta(0, 80, 0, 0),
                100
        );

        SettlementKnowledgeFlowLifecycle.CycleResult first =
                SettlementKnowledgeFlowLifecycle.runCycle(
                        world, 22, cycleTime, residents, 64, 64);
        assertEquals(1, first.successfulTransfers());
        assertEquals(1, listenersKnowing(world, speaker, residents, "the northern ford is passable"));

        social.applyDelta(
                speaker,
                firstPositiveTarget,
                new NpcSocialDelta(0, -80, 0, 0),
                100
        );
        social.applyDelta(
                speaker,
                secondPositiveTarget,
                new NpcSocialDelta(0, 80, 0, 0),
                100
        );

        SettlementKnowledgeFlowLifecycle.CycleResult replay =
                SettlementKnowledgeFlowLifecycle.runCycle(
                        world, 22, cycleTime, residents, 64, 64);

        assertEquals(0, replay.successfulTransfers());
        assertEquals(1, listenersKnowing(world, speaker, residents, "the northern ford is passable"));
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(secondPositiveTarget, 64).stream()
                .noneMatch(entry -> SemanticMemoryIdentity.canonicalStatement(entry.statement())
                        .equals("the northern ford is passable")));
    }

    @Test
    void adverseSpeakerToListenerSocialStateSuppressesExactTransferWithoutFallback() {
        List<NpcSocialDelta> adverseStates = List.of(
                new NpcSocialDelta(0, 0, 75, 0),
                new NpcSocialDelta(-75, 0, 0, 0),
                new NpcSocialDelta(0, 0, 0, -75)
        );

        for (int index = 0; index < adverseStates.size(); index++) {
            Path world = tempDir.resolve("social-suppression-" + index);
            UUID speaker = id(200 + index * 10);
            UUID listener = id(201 + index * 10);
            UUID sourceId = id(202 + index * 10);
            long cycleTime = 4_800L + index;

            appendSourceFact(world, speaker, sourceId, "The east road is blocked " + index);
            NpcSocialGraphStore.forWorld(world).applyDelta(
                    speaker,
                    listener,
                    adverseStates.get(index),
                    100
            );

            SettlementKnowledgeFlowLifecycle.CycleResult result = SettlementKnowledgeFlowLifecycle.runCycle(
                    world,
                    9,
                    cycleTime,
                    List.of(speaker, listener),
                    64,
                    64
            );

            assertEquals(1, result.opportunities());
            assertEquals(1, result.sociallySuppressedTransfers());
            assertEquals(0, result.attemptedTransfers());
            assertEquals(0, result.successfulTransfers());
            assertEquals(List.of(), result.statuses());
            assertTrue(SemanticMemoryStore.forWorld(world).getRecent(listener, 64).isEmpty());
            assertTrue(MemoryEventStore.forWorld(world).getRecent(listener, 64).stream()
                    .noneMatch(event -> event.type() == MemoryEvent.Type.DIALOGUE
                            && event.provenance() == MemoryEvent.Provenance.NPC_TOLD));
        }
    }

    @Test
    void adverseLegacyRouteIsSkippedForDeterministicNeutralCandidate() {
        Path world = tempDir.resolve("adverse-route-skip");
        UUID speaker = id(280);
        UUID listenerA = id(281);
        UUID listenerB = id(282);
        UUID listenerC = id(283);
        UUID sourceId = id(284);
        long cycleTime = 5_400L;
        List<UUID> residents = List.of(speaker, listenerA, listenerB, listenerC);

        appendSourceFact(world, speaker, sourceId, "The eastern ferry is operating");

        SettlementKnowledgeFlowSelector.Opportunity legacyOpportunity =
                SettlementKnowledgeFlowSelector.select(
                                SemanticMemoryStore.forWorld(world),
                                23,
                                cycleTime,
                                residents
                        ).opportunities().stream()
                        .filter(value -> value.sourceSemanticEntryId().equals(sourceId))
                        .findFirst()
                        .orElseThrow();

        NpcSocialGraphStore.forWorld(world).applyDelta(
                speaker,
                legacyOpportunity.listenerNpcId(),
                new NpcSocialDelta(0, 0, 80, 0),
                100
        );

        SettlementKnowledgeFlowLifecycle.CycleResult result =
                SettlementKnowledgeFlowLifecycle.runCycle(
                        world, 23, cycleTime, residents, 64, 64);

        assertEquals(1, result.successfulTransfers());
        assertTrue(SemanticMemoryStore.forWorld(world)
                .getRecent(legacyOpportunity.listenerNpcId(), 64)
                .isEmpty());
        assertEquals(
                1,
                listenersKnowing(world, speaker, residents, "the eastern ferry is operating")
        );
    }

    @Test
    void malformedSocialAuthoritySuppressesWholeOpportunityWithoutFallback() throws IOException {
        Path world = tempDir.resolve("malformed-social-authority");
        UUID speaker = id(290);
        UUID listenerA = id(291);
        UUID listenerB = id(292);
        UUID listenerC = id(293);
        UUID sourceId = id(294);
        long cycleTime = 5_600L;
        List<UUID> residents = List.of(speaker, listenerA, listenerB, listenerC);

        appendSourceFact(world, speaker, sourceId, "The southern gate is guarded");

        Path socialFile = world.resolve("livingworld").resolve("npc-social-graph.json");
        Files.createDirectories(socialFile.getParent());
        Files.writeString(
                socialFile,
                """
                {
                  "version": 1,
                  "edges": {
                    "not-a-canonical-edge": {
                      "trust": 80,
                      "respect": 0,
                      "fear": 0,
                      "affinity": 80
                    }
                  }
                }
                """,
                StandardCharsets.UTF_8
        );

        SettlementKnowledgeFlowLifecycle.CycleResult result =
                SettlementKnowledgeFlowLifecycle.runCycle(
                        world, 24, cycleTime, residents, 64, 64);

        assertEquals(1, result.opportunities());
        assertEquals(1, result.sociallySuppressedTransfers());
        assertEquals(0, result.attemptedTransfers());
        assertEquals(0, result.successfulTransfers());
        assertEquals(
                0,
                listenersKnowing(world, speaker, residents, "the southern gate is guarded")
        );
        assertTrue(residents.stream()
                .filter(id -> !id.equals(speaker))
                .flatMap(id -> MemoryEventStore.forWorld(world).getRecent(id, 64).stream())
                .noneMatch(event -> event.provenance() == MemoryEvent.Provenance.NPC_TOLD));
    }

    @Test
    void reverseOnlyHostilityDoesNotBlockSpeakerToListenerTransfer() {
        Path world = tempDir.resolve("reverse-only-hostility");
        UUID speaker = id(300);
        UUID listener = id(301);
        UUID sourceId = id(302);
        long cycleTime = 6_000L;

        appendSourceFact(world, speaker, sourceId, "The quarry reopened");
        NpcSocialGraphStore.forWorld(world).applyDelta(
                listener,
                speaker,
                new NpcSocialDelta(-75, 0, 0, 0),
                100
        );

        SettlementKnowledgeFlowLifecycle.CycleResult result = SettlementKnowledgeFlowLifecycle.runCycle(
                world,
                10,
                cycleTime,
                List.of(speaker, listener),
                64,
                64
        );

        assertEquals(1, result.opportunities());
        assertEquals(0, result.sociallySuppressedTransfers());
        assertEquals(1, result.attemptedTransfers());
        assertEquals(1, result.successfulTransfers());
        assertEquals(List.of(NpcKnowledgeTransferResult.Status.ADMITTED), result.statuses());
        assertEquals(1, SemanticMemoryStore.forWorld(world).getRecent(listener, 64).size());
    }

    @Test
    void exactSameCycleReplayCannotRetargetOrCreateSecondTransferEvidence() {
        Path world = tempDir.resolve("replay");
        UUID speaker = id(10);
        UUID listenerA = id(11);
        UUID listenerB = id(12);
        UUID listenerC = id(13);
        UUID sourceId = id(110);
        long cycleTime = 3_600L;

        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                "The well is contaminated",
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                200L,
                0L,
                90,
                100,
                List.of(id(111))
        ), 64);

        List<UUID> residents = List.of(speaker, listenerA, listenerB, listenerC);
        SettlementKnowledgeFlowLifecycle.CycleResult first = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 8, cycleTime, residents, 64, 64);
        assertEquals(1, first.successfulTransfers());

        long evidenceBefore = residents.stream()
                .flatMap(npc -> MemoryEventStore.forWorld(world).getRecent(npc, 64).stream())
                .filter(event -> event.type() == MemoryEvent.Type.DIALOGUE
                        && event.provenance() == MemoryEvent.Provenance.NPC_TOLD)
                .count();
        long listenerClaimsBefore = residents.stream()
                .filter(npc -> !npc.equals(speaker))
                .filter(npc -> SemanticMemoryStore.forWorld(world).getRecent(npc, 64).stream()
                        .anyMatch(entry -> SemanticMemoryIdentity.canonicalStatement(entry.statement())
                                .equals("the well is contaminated")))
                .count();

        SettlementKnowledgeFlowLifecycle.CycleResult replay = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 8, cycleTime, residents, 64, 64);

        long evidenceAfter = residents.stream()
                .flatMap(npc -> MemoryEventStore.forWorld(world).getRecent(npc, 64).stream())
                .filter(event -> event.type() == MemoryEvent.Type.DIALOGUE
                        && event.provenance() == MemoryEvent.Provenance.NPC_TOLD)
                .count();
        long listenerClaimsAfter = residents.stream()
                .filter(npc -> !npc.equals(speaker))
                .filter(npc -> SemanticMemoryStore.forWorld(world).getRecent(npc, 64).stream()
                        .anyMatch(entry -> SemanticMemoryIdentity.canonicalStatement(entry.statement())
                                .equals("the well is contaminated")))
                .count();

        assertEquals(evidenceBefore, evidenceAfter);
        assertEquals(listenerClaimsBefore, listenerClaimsAfter);
        assertEquals(1, listenerClaimsAfter);
        assertEquals(0, replay.successfulTransfers());
        assertTrue(replay.opportunities() <= 1);
    }

    private static long listenersKnowing(
            Path world,
            UUID speaker,
            List<UUID> residents,
            String canonicalStatement
    ) {
        return residents.stream()
                .filter(id -> !id.equals(speaker))
                .filter(id -> SemanticMemoryStore.forWorld(world).getRecent(id, 64).stream()
                        .anyMatch(entry -> SemanticMemoryIdentity.canonicalStatement(entry.statement())
                                .equals(canonicalStatement)))
                .count();
    }

    private static void appendSourceFact(Path world, UUID speaker, UUID sourceId, String statement) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                300L,
                0L,
                90,
                100,
                List.of(id(999))
        ), 64);
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
