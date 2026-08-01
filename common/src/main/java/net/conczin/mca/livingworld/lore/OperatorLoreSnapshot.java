package net.conczin.mca.livingworld.lore;

public record OperatorLoreSnapshot(
        String world,
        String villager,
        String player,
        String village
) {
    public OperatorLoreSnapshot {
        world = blankIfNull(world);
        villager = blankIfNull(villager);
        player = blankIfNull(player);
        village = blankIfNull(village);
    }

    private static String blankIfNull(String value) {
        return value == null ? "" : value;
    }
}
