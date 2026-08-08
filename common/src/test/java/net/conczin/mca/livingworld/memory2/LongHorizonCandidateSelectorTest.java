package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongHorizonCandidateSelectorTest {
    private static final Comparator<Fixture> NEWEST_FIRST = Comparator
            .comparingLong(Fixture::gameTime).reversed()
            .thenComparing(fixture -> fixture.id().toString(), Comparator.reverseOrder());
    private static final Comparator<Fixture> DURABLE_FIRST = Comparator
            .comparingInt(Fixture::durability).reversed()
            .thenComparing(Comparator.comparingLong(Fixture::gameTime).reversed())
            .thenComparing(fixture -> fixture.id().toString());

    @Test
    void reservesOneQuarterOfThirtyTwoCandidatesForDurableOlderRecords() {
        List<Fixture> input = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            int durability = i == 0 ? 1_000 : i;
            input.add(fixture(i, 1_000L + i, durability));
        }

        List<Fixture> selected = LongHorizonCandidateSelector.select(
                input,
                32,
                NEWEST_FIRST,
                DURABLE_FIRST,
                Fixture::id
        );

        assertEquals(32, selected.size());
        assertTrue(ids(selected).contains(fixture(0, 1_000L, 1_000).id()));
        for (int i = 16; i < 40; i++) {
            assertTrue(ids(selected).contains(fixture(i, 1_000L + i, i).id()));
        }
        for (int i : List.of(9, 10, 11, 12, 13, 14, 15)) {
            assertTrue(ids(selected).contains(fixture(i, 1_000L + i, i).id()));
        }
    }

    @Test
    void limitOneSelectsNewestOnly() {
        Fixture oldDurable = fixture(1, 10L, 1_000);
        Fixture newestWeak = fixture(2, 20L, 0);

        List<Fixture> selected = LongHorizonCandidateSelector.select(
                List.of(oldDurable, newestWeak),
                1,
                NEWEST_FIRST,
                DURABLE_FIRST,
                Fixture::id
        );

        assertEquals(List.of(newestWeak.id()), ids(selected));
    }

    @Test
    void limitTwoSelectsOneRecentAndOneDurable() {
        Fixture oldDurable = fixture(1, 10L, 1_000);
        Fixture middleWeak = fixture(2, 20L, 1);
        Fixture newestWeak = fixture(3, 30L, 0);

        List<Fixture> selected = LongHorizonCandidateSelector.select(
                List.of(oldDurable, middleWeak, newestWeak),
                2,
                NEWEST_FIRST,
                DURABLE_FIRST,
                Fixture::id
        );

        assertEquals(2, selected.size());
        assertTrue(ids(selected).contains(newestWeak.id()));
        assertTrue(ids(selected).contains(oldDurable.id()));
    }

    @Test
    void deDuplicatesRecentAndDurablePoolsAndNeverExceedsLimit() {
        Fixture newestAlsoDurable = fixture(1, 30L, 1_000);
        Fixture middle = fixture(2, 20L, 10);
        Fixture old = fixture(3, 10L, 5);

        List<Fixture> selected = LongHorizonCandidateSelector.select(
                List.of(newestAlsoDurable, middle, old),
                2,
                NEWEST_FIRST,
                DURABLE_FIRST,
                Fixture::id
        );

        assertEquals(2, selected.size());
        assertEquals(2, ids(selected).stream().distinct().count());
    }

    @Test
    void selectionIsInputOrderIndependent() {
        List<Fixture> forward = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            forward.add(fixture(i, 1_000L + i, i == 0 ? 1_000 : i));
        }
        List<Fixture> reverse = new ArrayList<>(forward);
        Collections.reverse(reverse);

        List<UUID> forwardIds = ids(LongHorizonCandidateSelector.select(
                forward, 32, NEWEST_FIRST, DURABLE_FIRST, Fixture::id
        ));
        List<UUID> reverseIds = ids(LongHorizonCandidateSelector.select(
                reverse, 32, NEWEST_FIRST, DURABLE_FIRST, Fixture::id
        ));

        assertEquals(forwardIds, reverseIds);
    }

    private static Fixture fixture(long idValue, long gameTime, int durability) {
        return new Fixture(new UUID(0L, idValue + 1L), gameTime, durability);
    }

    private static List<UUID> ids(List<Fixture> values) {
        return values.stream().map(Fixture::id).toList();
    }

    private record Fixture(UUID id, long gameTime, int durability) {
    }
}
