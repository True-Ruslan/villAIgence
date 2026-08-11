package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcSocialCausalFrontierAttributionTest {
    @TempDir
    Path tempDir;

    @Test
    void malformedMapKeyWithValidCursorSourceBlocksThatSourceAcrossUnrelatedSaveReload() throws Exception {
        UUID source = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
        UUID target = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
        UUID cause = UUID.fromString("cccccccc-3333-3333-3333-333333333333");
        UUID mutation = NpcSocialMutationIdentity.forCause(source, cause);
        UUID unrelatedSource = UUID.fromString("dddddddd-4444-4444-4444-444444444444");
        UUID unrelatedTarget = UUID.fromString("eeeeeeee-5555-5555-5555-555555555555");
        UUID unrelatedCause = UUID.fromString("ffffffff-6666-6666-6666-666666666666");
        Path file = tempDir.resolve("npc-social-graph.json");

        Files.writeString(file, """
                {
                  "version": 1,
                  "edges": {},
                  "causalFrontiers": {
                    "not-a-uuid": {
                      "mutationId": "%s",
                      "sourceNpcId": "%s",
                      "targetNpcId": "%s",
                      "causeEventId": "%s",
                      "causeGameTime": 100,
                      "boundedRequestedDelta": {"trust": 2, "respect": 0, "fear": 0, "affinity": 0},
                      "appliedDelta": {"trust": 2, "respect": 0, "fear": 0, "affinity": 0},
                      "before": {"trust": 0, "respect": 0, "fear": 0, "affinity": 0},
                      "after": {"trust": 2, "respect": 0, "fear": 0, "affinity": 0},
                      "outcome": "APPLIED"
                    }
                  }
                }
                """.formatted(mutation, source, target, cause));

        NpcSocialGraphStore store = new NpcSocialGraphStore(file);
        assertEquals(
                NpcSocialCausalMutation.Status.FRONTIER_CORRUPT,
                store.applyCausalDelta(
                        source, target, cause, 100L,
                        new NpcSocialDelta(2, 0, 0, 0), 4
                ).status(),
                "malformed key is attributable through cursor.sourceNpcId and must not bypass replay protection"
        );

        assertEquals(
                NpcSocialCausalMutation.Status.APPLIED,
                store.applyCausalDelta(
                        unrelatedSource, unrelatedTarget, unrelatedCause, 101L,
                        new NpcSocialDelta(1, 0, 0, 0), 4
                ).status()
        );

        NpcSocialGraphStore reloaded = new NpcSocialGraphStore(file);
        assertEquals(
                NpcSocialCausalMutation.Status.FRONTIER_CORRUPT,
                reloaded.applyCausalDelta(
                        source, target, cause, 100L,
                        new NpcSocialDelta(2, 0, 0, 0), 4
                ).status(),
                "unrelated save must preserve attributable malformed replay ambiguity"
        );
    }
}
