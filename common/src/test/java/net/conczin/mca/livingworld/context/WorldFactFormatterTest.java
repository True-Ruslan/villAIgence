package net.conczin.mca.livingworld.context;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldFactFormatterTest {
    @Test
    void itemFactsAreAggregatedAndBounded() {
        List<String> facts = WorldFactFormatter.summarizeItems(
                List.of("minecraft:bread", "minecraft:bread", "minecraft:iron_ingot", "minecraft:apple"),
                2
        );

        assertEquals(List.of("minecraft:bread x2", "minecraft:iron_ingot x1"), facts);
    }

    @Test
    void snapshotCopiesMutableCollections() {
        List<String> context = new ArrayList<>(List.of("context"));
        List<String> facts = new ArrayList<>(List.of("fact"));
        List<LivingWorldContextSnapshot.ActionDescriptor> actions = new ArrayList<>(List.of(
                new LivingWorldContextSnapshot.ActionDescriptor("follow-player", "Follow the player")
        ));

        LivingWorldContextSnapshot snapshot = new LivingWorldContextSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "Player", "Villager",
                context, facts, actions, 12L, 34L, Path.of("world"),
                false, false, "English"
        );

        context.add("mutated");
        facts.add("mutated");
        actions.clear();

        assertEquals(List.of("context"), snapshot.contextLines());
        assertEquals(List.of("fact"), snapshot.worldFacts());
        assertEquals(1, snapshot.availableActions().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.contextLines().add("x"));
    }
}
