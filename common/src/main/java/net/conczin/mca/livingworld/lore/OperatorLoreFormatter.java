package net.conczin.mca.livingworld.lore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OperatorLoreFormatter {
    private OperatorLoreFormatter() {
    }

    public static List<String> format(OperatorLoreSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<String> lines = new ArrayList<>(4);
        add(lines, "Server-authored world lore:", snapshot.world());
        add(lines, "Server-authored villager lore:", snapshot.villager());
        add(lines, "Server-authored player lore:", snapshot.player());
        add(lines, "Server-authored village lore:", snapshot.village());
        return List.copyOf(lines);
    }

    private static void add(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(label + "\n" + value);
        }
    }
}
