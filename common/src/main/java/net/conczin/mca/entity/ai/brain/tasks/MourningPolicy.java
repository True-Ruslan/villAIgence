package net.conczin.mca.entity.ai.brain.tasks;

/**
 * Minecraft-independent mourning target and retry rules.
 */
final class MourningPolicy {
    private MourningPolicy() {
    }

    enum Outcome {
        COMPLETE,
        RETRY
    }

    record Candidate(
            boolean oppositeSide,
            int verticalDistance,
            int reservations,
            int manhattanDistance,
            int stableTieBreak
    ) {
    }

    static int compareCandidates(Candidate left, Candidate right) {
        int result = Boolean.compare(left.oppositeSide(), right.oppositeSide());
        if (result != 0) {
            return result;
        }
        result = Integer.compare(left.verticalDistance(), right.verticalDistance());
        if (result != 0) {
            return result;
        }
        result = Integer.compare(left.reservations(), right.reservations());
        if (result != 0) {
            return result;
        }
        result = Integer.compare(left.manhattanDistance(), right.manhattanDistance());
        if (result != 0) {
            return result;
        }
        return Integer.compare(left.stableTieBreak(), right.stableTieBreak());
    }

    static boolean isWithinMourningArea(
            boolean mournable,
            double distanceSquared,
            double maxDistance,
            boolean sameColumn
    ) {
        return mournable && distanceSquared < maxDistance * maxDistance && !sameColumn;
    }

    static Outcome outcome(
            boolean completed,
            boolean hadAssignedSite,
            boolean targetStillMournable,
            boolean periodicCandidateStillExists
    ) {
        boolean effectivePeriodicCandidate = !hadAssignedSite && periodicCandidateStillExists;
        return completed || (!targetStillMournable && !effectivePeriodicCandidate)
                ? Outcome.COMPLETE
                : Outcome.RETRY;
    }

    static long retryTimestamp(long now, long cooldown, long retryDelay) {
        return now - cooldown + retryDelay;
    }

    static boolean isDue(long now, long timestamp, long cooldown) {
        return now - timestamp > cooldown;
    }
}
