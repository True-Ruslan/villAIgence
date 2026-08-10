package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcSocialGraphCausalMutationTest {
    @TempDir
    Path tempDir;

    @Test
    void exactReplayReturnsExistingOutcomeWithoutApplyingDeltaTwice() {
        UUID source = new UUID(1L, 1L);
        UUID target = new UUID(2L, 2L);
        UUID cause = new UUID(3L, 3L);
        NpcSocialGraphStore store = store();

        NpcSocialCausalMutation first = store.applyCausalDelta(
                source,
                target,
                cause,
                100L,
                new NpcSocialDelta(8, -2, 1, 3),
                3
        );
        NpcSocialCausalMutation replay = store.applyCausalDelta(
                source,
                target,
                cause,
                100L,
                new NpcSocialDelta(8, -2, 1, 3),
                3
        );

        assertEquals(NpcSocialCausalMutation.Status.APPLIED, first.status());
        assertEquals(new NpcSocialDelta(3, -2, 1, 3), first.boundedRequestedDelta());
        assertEquals(new NpcSocialDelta(3, -2, 1, 3), first.appliedDelta());
        assertEquals(NpcSocialState.NEUTRAL, first.before());
        assertEquals(new NpcSocialState(3, -2, 1, 3), first.after());

        assertEquals(NpcSocialCausalMutation.Status.REPLAYED, replay.status());
        assertEquals(first.mutationId(), replay.mutationId());
        assertEquals(first.before(), replay.before());
        assertEquals(first.after(), replay.after());
        assertEquals(first.boundedRequestedDelta(), replay.boundedRequestedDelta());
        assertEquals(first.appliedDelta(), replay.appliedDelta());
        assertEquals(first.after(), store.get(source, target));

        NpcSocialMutationCursor cursor = store.latestCausalMutation(source).orElseThrow();
        assertEquals(first.mutationId(), cursor.mutationId());
        assertEquals(source, cursor.sourceNpcId());
        assertEquals(target, cursor.targetNpcId());
        assertEquals(cause, cursor.causeEventId());
        assertEquals(100L, cursor.causeGameTime());
        assertEquals(NpcSocialMutationCursor.Outcome.APPLIED, cursor.outcome());
    }

    @Test
    void sameCauseWithDifferentTargetOrBoundedRequestFailsClosed() {
        UUID source = new UUID(10L, 10L);
        UUID target = new UUID(11L, 11L);
        UUID otherTarget = new UUID(12L, 12L);
        UUID cause = new UUID(13L, 13L);
        NpcSocialGraphStore store = store();

        NpcSocialCausalMutation first = store.applyCausalDelta(
                source, target, cause, 200L, new NpcSocialDelta(2, 0, 0, 0), 4
        );
        NpcSocialCausalMutation differentTarget = store.applyCausalDelta(
                source, otherTarget, cause, 200L, new NpcSocialDelta(2, 0, 0, 0), 4
        );
        NpcSocialCausalMutation differentRequest = store.applyCausalDelta(
                source, target, cause, 200L, new NpcSocialDelta(3, 0, 0, 0), 4
        );

        assertEquals(NpcSocialCausalMutation.Status.APPLIED, first.status());
        assertEquals(NpcSocialCausalMutation.Status.CONFLICTING_CAUSE, differentTarget.status());
        assertEquals(NpcSocialCausalMutation.Status.CONFLICTING_CAUSE, differentRequest.status());
        assertEquals(new NpcSocialState(2, 0, 0, 0), store.get(source, target));
        assertEquals(NpcSocialState.NEUTRAL, store.get(source, otherTarget));
    }

    @Test
    void olderCauseIsRejectedAfterNewerCauseAdvancesSourceFrontier() {
        UUID source = new UUID(20L, 20L);
        UUID target = new UUID(21L, 21L);
        NpcSocialGraphStore store = store();
        UUID newerCause = new UUID(22L, 22L);
        UUID olderCause = new UUID(23L, 23L);

        assertEquals(
                NpcSocialCausalMutation.Status.APPLIED,
                store.applyCausalDelta(
                        source, target, newerCause, 400L, new NpcSocialDelta(2, 0, 0, 0), 5
                ).status()
        );
        NpcSocialCausalMutation stale = store.applyCausalDelta(
                source, target, olderCause, 399L, new NpcSocialDelta(9, 0, 0, 0), 5
        );

        assertEquals(NpcSocialCausalMutation.Status.STALE_CAUSE, stale.status());
        assertEquals(new NpcSocialState(2, 0, 0, 0), store.get(source, target));
        assertEquals(newerCause, store.latestCausalMutation(source).orElseThrow().causeEventId());
    }

    @Test
    void equalGameTimeUsesCauseUuidAsDeterministicOrderTieBreaker() {
        UUID source = new UUID(30L, 30L);
        UUID target = new UUID(31L, 31L);
        UUID firstCause = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID laterCause = UUID.fromString("00000000-0000-0000-0000-000000000020");
        NpcSocialGraphStore store = store();

        assertEquals(
                NpcSocialCausalMutation.Status.APPLIED,
                store.applyCausalDelta(
                        source, target, laterCause, 500L, new NpcSocialDelta(1, 0, 0, 0), 5
                ).status()
        );
        assertEquals(
                NpcSocialCausalMutation.Status.STALE_CAUSE,
                store.applyCausalDelta(
                        source, target, firstCause, 500L, new NpcSocialDelta(4, 0, 0, 0), 5
                ).status()
        );
        assertEquals(new NpcSocialState(1, 0, 0, 0), store.get(source, target));
    }

    @Test
    void validNoChangeConsumesCauseFrontier() {
        UUID source = new UUID(40L, 40L);
        UUID target = new UUID(41L, 41L);
        UUID cause = new UUID(42L, 42L);
        NpcSocialGraphStore store = store();

        NpcSocialCausalMutation noChange = store.applyCausalDelta(
                source, target, cause, 600L, NpcSocialDelta.NONE, 5
        );
        NpcSocialCausalMutation replay = store.applyCausalDelta(
                source, target, cause, 600L, NpcSocialDelta.NONE, 5
        );

        assertEquals(NpcSocialCausalMutation.Status.NO_CHANGE, noChange.status());
        assertEquals(NpcSocialMutationCursor.Outcome.NO_CHANGE, store.latestCausalMutation(source).orElseThrow().outcome());
        assertEquals(NpcSocialCausalMutation.Status.REPLAYED, replay.status());
        assertEquals(NpcSocialState.NEUTRAL, store.get(source, target));
    }

    @Test
    void capacityRejectionConsumesCauseAndCannotBecomeEffectiveLater() {
        UUID source = new UUID(50L, 50L);
        NpcSocialGraphStore store = store();
        List<UUID> retained = fillToCapacity(store, source);
        UUID overflowTarget = new UUID(999L, 999L);
        UUID cause = new UUID(51L, 51L);

        NpcSocialCausalMutation rejected = store.applyCausalDelta(
                source, overflowTarget, cause, 700L, new NpcSocialDelta(1, 0, 0, 0), 5
        );
        assertEquals(NpcSocialCausalMutation.Status.CAPACITY_REACHED, rejected.status());
        assertEquals(NpcSocialMutationCursor.Outcome.CAPACITY_REACHED, store.latestCausalMutation(source).orElseThrow().outcome());

        UUID removed = retained.get(0);
        assertEquals(
                NpcSocialGraphMutation.Status.APPLIED,
                store.applyDelta(source, removed, new NpcSocialDelta(-1, 0, 0, 0), 5).status()
        );

        NpcSocialCausalMutation replay = store.applyCausalDelta(
                source, overflowTarget, cause, 700L, new NpcSocialDelta(1, 0, 0, 0), 5
        );
        assertEquals(NpcSocialCausalMutation.Status.REPLAYED, replay.status());
        assertEquals(NpcSocialState.NEUTRAL, store.get(source, overflowTarget));
    }

    @Test
    void frontierAndEdgeReloadTogetherFromFreshStoreInstance() {
        UUID source = new UUID(60L, 60L);
        UUID target = new UUID(61L, 61L);
        UUID cause = new UUID(62L, 62L);
        Path file = tempDir.resolve("reload-social-graph.json");

        NpcSocialGraphStore first = new NpcSocialGraphStore(file);
        NpcSocialCausalMutation applied = first.applyCausalDelta(
                source, target, cause, 800L, new NpcSocialDelta(4, 2, -1, 3), 4
        );

        NpcSocialGraphStore reloaded = new NpcSocialGraphStore(file);

        assertEquals(applied.after(), reloaded.get(source, target));
        assertEquals(applied.mutationId(), reloaded.latestCausalMutation(source).orElseThrow().mutationId());
        assertEquals(
                NpcSocialCausalMutation.Status.REPLAYED,
                reloaded.applyCausalDelta(
                        source, target, cause, 800L, new NpcSocialDelta(4, 2, -1, 3), 4
                ).status()
        );
        assertEquals(applied.after(), reloaded.get(source, target));
    }

    private NpcSocialGraphStore store() {
        return new NpcSocialGraphStore(tempDir.resolve(UUID.randomUUID() + ".json"));
    }

    private static List<UUID> fillToCapacity(NpcSocialGraphStore store, UUID source) {
        List<UUID> targets = new ArrayList<>();
        for (int index = 0; index < 64; index++) {
            UUID target = new UUID(1000L + index, 2000L + index);
            targets.add(target);
            assertEquals(
                    NpcSocialGraphMutation.Status.APPLIED,
                    store.applyDelta(source, target, new NpcSocialDelta(1, 0, 0, 0), 5).status()
            );
        }
        return targets;
    }
}
