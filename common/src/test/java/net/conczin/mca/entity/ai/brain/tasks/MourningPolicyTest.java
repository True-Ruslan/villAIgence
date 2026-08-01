package net.conczin.mca.entity.ai.brain.tasks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MourningPolicyTest {
    @Test
    void prefersApproachSideBeforeOtherCandidateCosts() {
        MourningPolicy.Candidate approachSide = new MourningPolicy.Candidate(false, 2, 5, 9, 100);
        MourningPolicy.Candidate oppositeSide = new MourningPolicy.Candidate(true, 0, 0, 1, 0);

        assertTrue(MourningPolicy.compareCandidates(approachSide, oppositeSide) < 0);
    }

    @Test
    void prefersLowerVerticalOffsetThenFewerReservations() {
        MourningPolicy.Candidate level = new MourningPolicy.Candidate(false, 0, 3, 8, 10);
        MourningPolicy.Candidate raised = new MourningPolicy.Candidate(false, 1, 0, 1, 0);
        MourningPolicy.Candidate free = new MourningPolicy.Candidate(false, 0, 0, 12, 20);

        assertTrue(MourningPolicy.compareCandidates(level, raised) < 0);
        assertTrue(MourningPolicy.compareCandidates(free, level) < 0);
    }

    @Test
    void distanceAndStableTieBreakMakeSelectionDeterministic() {
        MourningPolicy.Candidate near = new MourningPolicy.Candidate(false, 0, 0, 3, 99);
        MourningPolicy.Candidate far = new MourningPolicy.Candidate(false, 0, 0, 4, 0);
        MourningPolicy.Candidate lowerTie = new MourningPolicy.Candidate(false, 0, 0, 3, 7);

        assertTrue(MourningPolicy.compareCandidates(near, far) < 0);
        assertTrue(MourningPolicy.compareCandidates(lowerTie, near) < 0);
    }

    @Test
    void mourningAreaRequiresLiveGraveNearbyAndDifferentColumn() {
        assertTrue(MourningPolicy.isWithinMourningArea(true, 8.9D, 3.0D, false));
        assertFalse(MourningPolicy.isWithinMourningArea(false, 1.0D, 3.0D, false));
        assertFalse(MourningPolicy.isWithinMourningArea(true, 9.1D, 3.0D, false));
        assertFalse(MourningPolicy.isWithinMourningArea(true, 1.0D, 3.0D, true));
    }

    @Test
    void completedDialogueAlwaysCompletesMourning() {
        assertEquals(
                MourningPolicy.Outcome.COMPLETE,
                MourningPolicy.outcome(true, true, true, true)
        );
    }

    @Test
    void vanishedAssignedTargetCompletesWithoutWeeklyRetryLoop() {
        assertEquals(
                MourningPolicy.Outcome.COMPLETE,
                MourningPolicy.outcome(false, true, false, false)
        );
    }

    @Test
    void reachableUnfinishedTargetRetriesLater() {
        assertEquals(
                MourningPolicy.Outcome.RETRY,
                MourningPolicy.outcome(false, true, true, false)
        );
    }

    @Test
    void periodicCandidateWithoutAssignedSiteRetriesLater() {
        assertEquals(
                MourningPolicy.Outcome.RETRY,
                MourningPolicy.outcome(false, false, false, true)
        );
    }

    @Test
    void retryTimestampDefersNextAttemptByConfiguredDelay() {
        long now = 100_000L;
        long cooldown = 24_000L * 7L;
        long retryDelay = 1_200L;
        long timestamp = MourningPolicy.retryTimestamp(now, cooldown, retryDelay);

        assertFalse(MourningPolicy.isDue(now + retryDelay, timestamp, cooldown));
        assertTrue(MourningPolicy.isDue(now + retryDelay + 1L, timestamp, cooldown));
    }
}
