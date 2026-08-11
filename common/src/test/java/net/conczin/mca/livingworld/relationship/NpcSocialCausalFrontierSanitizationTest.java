package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSocialCausalFrontierSanitizationTest {
    @TempDir
    Path tempDir;

    @Test
    void oldV1GraphWithoutCausalFrontierRemainsCompatible() throws Exception {
        UUID source = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID target = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID cause = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Path file = tempDir.resolve("npc-social-graph.json");
        Files.writeString(file, """
                {
                  "version": 1,
                  "edges": {
                    "%s/%s": {"trust": 7, "respect": 0, "fear": 0, "affinity": 0}
                  }
                }
                """.formatted(source, target));

        NpcSocialGraphStore store = new NpcSocialGraphStore(file);

        assertTrue(store.latestCausalMutation(source).isEmpty());
        assertEquals(new NpcSocialState(7, 0, 0, 0), store.get(source, target));
        assertEquals(
                NpcSocialCausalMutation.Status.APPLIED,
                store.applyCausalDelta(
                        source, target, cause, 100L, new NpcSocialDelta(1, 0, 0, 0), 4
                ).status()
        );
    }

    @Test
    void duplicateCanonicalSourceFrontierFailsClosedAndSurvivesUnrelatedSaveReload() throws Exception {
        UUID source = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID target = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID cause = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID mutation = NpcSocialMutationIdentity.forCause(source, cause);
        UUID unrelatedSource = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID unrelatedTarget = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID unrelatedCause = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        Path file = tempDir.resolve("duplicate-frontier.json");

        String cursor = cursorJson(mutation, source, target, cause, 200L, 2, 2);
        Files.writeString(file, """
                {
                  "version": 1,
                  "edges": {},
                  "causalFrontiers": {
                    "%s": %s,
                    "%s": %s
                  }
                }
                """.formatted(source, cursor, source.toString().toUpperCase(Locale.ROOT), cursor));

        NpcSocialGraphStore store = new NpcSocialGraphStore(file);
        assertEquals(
                NpcSocialCausalMutation.Status.FRONTIER_CORRUPT,
                store.applyCausalDelta(
                        source, target, cause, 200L, new NpcSocialDelta(2, 0, 0, 0), 4
                ).status()
        );
        assertEquals(
                NpcSocialCausalMutation.Status.APPLIED,
                store.applyCausalDelta(
                        unrelatedSource,
                        unrelatedTarget,
                        unrelatedCause,
                        201L,
                        new NpcSocialDelta(1, 0, 0, 0),
                        4
                ).status()
        );

        NpcSocialGraphStore reloaded = new NpcSocialGraphStore(file);
        assertEquals(
                NpcSocialCausalMutation.Status.FRONTIER_CORRUPT,
                reloaded.applyCausalDelta(
                        source, target, cause, 200L, new NpcSocialDelta(2, 0, 0, 0), 4
                ).status(),
                "an unrelated save must not erase the corruption marker/replay ambiguity"
        );
        assertEquals(new NpcSocialState(1, 0, 0, 0), reloaded.get(unrelatedSource, unrelatedTarget));
    }

    @Test
    void frontierWhoseMapKeyDoesNotMatchCursorSourceFailsClosed() throws Exception {
        UUID source = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID forgedSource = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID target = UUID.fromString("10000000-0000-0000-0000-000000000003");
        UUID cause = UUID.fromString("10000000-0000-0000-0000-000000000004");
        UUID forgedMutation = NpcSocialMutationIdentity.forCause(forgedSource, cause);
        Path file = tempDir.resolve("mismatch-frontier.json");
        Files.writeString(file, graphWithFrontier(
                source.toString(),
                cursorJson(forgedMutation, forgedSource, target, cause, 300L, 1, 1)
        ));

        NpcSocialGraphStore store = new NpcSocialGraphStore(file);

        assertEquals(
                NpcSocialCausalMutation.Status.FRONTIER_CORRUPT,
                store.applyCausalDelta(
                        source, target, cause, 300L, new NpcSocialDelta(1, 0, 0, 0), 4
                ).status()
        );
        assertTrue(store.latestCausalMutation(source).isEmpty());
    }

    @Test
    void frontierWithInconsistentAppliedDeltaFailsClosed() throws Exception {
        UUID source = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UUID target = UUID.fromString("20000000-0000-0000-0000-000000000002");
        UUID cause = UUID.fromString("20000000-0000-0000-0000-000000000003");
        UUID mutation = NpcSocialMutationIdentity.forCause(source, cause);
        Path file = tempDir.resolve("delta-frontier.json");
        Files.writeString(file, graphWithFrontier(
                source.toString(),
                cursorJson(mutation, source, target, cause, 400L, 4, 99)
        ));

        NpcSocialGraphStore store = new NpcSocialGraphStore(file);

        assertEquals(
                NpcSocialCausalMutation.Status.FRONTIER_CORRUPT,
                store.applyCausalDelta(
                        source, target, cause, 400L, new NpcSocialDelta(4, 0, 0, 0), 4
                ).status()
        );
        assertEquals(NpcSocialState.NEUTRAL, store.get(source, target));
    }

    @Test
    void missingRequiredCursorFieldFailsClosedWithoutDiscardingValidGraphState() throws Exception {
        UUID source = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID target = UUID.fromString("30000000-0000-0000-0000-000000000002");
        UUID cause = UUID.fromString("30000000-0000-0000-0000-000000000003");
        UUID unrelatedSource = UUID.fromString("30000000-0000-0000-0000-000000000004");
        UUID unrelatedTarget = UUID.fromString("30000000-0000-0000-0000-000000000005");
        UUID unrelatedCause = UUID.fromString("30000000-0000-0000-0000-000000000006");
        Path file = tempDir.resolve("missing-required-frontier-field.json");

        Files.writeString(file, """
                {
                  "version": 1,
                  "edges": {
                    "%s/%s": {"trust": 5, "respect": 0, "fear": 0, "affinity": 0}
                  },
                  "causalFrontiers": {
                    "%s": {
                      "sourceNpcId": "%s",
                      "targetNpcId": "%s",
                      "causeEventId": "%s",
                      "causeGameTime": 500,
                      "boundedRequestedDelta": {"trust": 1, "respect": 0, "fear": 0, "affinity": 0},
                      "appliedDelta": {"trust": 1, "respect": 0, "fear": 0, "affinity": 0},
                      "before": {"trust": 4, "respect": 0, "fear": 0, "affinity": 0},
                      "after": {"trust": 5, "respect": 0, "fear": 0, "affinity": 0},
                      "outcome": "APPLIED"
                    }
                  }
                }
                """.formatted(source, target, source, source, target, cause));

        NpcSocialGraphStore store = new NpcSocialGraphStore(file);

        assertEquals(
                new NpcSocialState(5, 0, 0, 0),
                store.get(source, target),
                "one malformed causal cursor must not cause valid directed graph state to be discarded"
        );
        assertEquals(
                NpcSocialCausalMutation.Status.FRONTIER_CORRUPT,
                store.applyCausalDelta(
                        source, target, cause, 500L, new NpcSocialDelta(1, 0, 0, 0), 4
                ).status(),
                "missing replay identity must fail closed for the attributable source"
        );
        assertEquals(
                NpcSocialCausalMutation.Status.APPLIED,
                store.applyCausalDelta(
                        unrelatedSource,
                        unrelatedTarget,
                        unrelatedCause,
                        501L,
                        new NpcSocialDelta(2, 0, 0, 0),
                        4
                ).status(),
                "an unrelated healthy source must remain writable"
        );

        NpcSocialGraphStore reloaded = new NpcSocialGraphStore(file);
        assertEquals(
                new NpcSocialState(5, 0, 0, 0),
                reloaded.get(source, target),
                "an unrelated save must preserve pre-existing valid graph state"
        );
        assertEquals(
                NpcSocialCausalMutation.Status.FRONTIER_CORRUPT,
                reloaded.applyCausalDelta(
                        source, target, cause, 500L, new NpcSocialDelta(1, 0, 0, 0), 4
                ).status(),
                "an unrelated save/reload must not erase malformed-cursor replay ambiguity"
        );
        assertEquals(
                new NpcSocialState(2, 0, 0, 0),
                reloaded.get(unrelatedSource, unrelatedTarget),
                "the unrelated healthy mutation must survive the same reload"
        );
    }

    private static String graphWithFrontier(String sourceKey, String cursor) {
        return "{\n"
                + "  \"version\": 1,\n"
                + "  \"edges\": {},\n"
                + "  \"causalFrontiers\": {\n"
                + "    \"" + sourceKey + "\": " + cursor + "\n"
                + "  }\n"
                + "}\n";
    }

    private static String cursorJson(
            UUID mutation,
            UUID source,
            UUID target,
            UUID cause,
            long gameTime,
            int requestedTrust,
            int appliedTrust
    ) {
        return "{"
                + "\"mutationId\":\"" + mutation + "\","
                + "\"sourceNpcId\":\"" + source + "\","
                + "\"targetNpcId\":\"" + target + "\","
                + "\"causeEventId\":\"" + cause + "\","
                + "\"causeGameTime\":" + gameTime + ","
                + "\"boundedRequestedDelta\":{\"trust\":" + requestedTrust + ",\"respect\":0,\"fear\":0,\"affinity\":0},"
                + "\"appliedDelta\":{\"trust\":" + appliedTrust + ",\"respect\":0,\"fear\":0,\"affinity\":0},"
                + "\"before\":{\"trust\":0,\"respect\":0,\"fear\":0,\"affinity\":0},"
                + "\"after\":{\"trust\":" + requestedTrust + ",\"respect\":0,\"fear\":0,\"affinity\":0},"
                + "\"outcome\":\"APPLIED\""
                + "}";
    }
}
