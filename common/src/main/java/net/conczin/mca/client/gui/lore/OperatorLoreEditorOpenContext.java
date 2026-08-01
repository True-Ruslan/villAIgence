package net.conczin.mca.client.gui.lore;

/** Immutable client presentation context supplied by a server-authored GUI-open packet. */
public record OperatorLoreEditorOpenContext(int villagerEntityId) {
    public boolean hasVillagerTarget() {
        return villagerEntityId >= 0;
    }
}
