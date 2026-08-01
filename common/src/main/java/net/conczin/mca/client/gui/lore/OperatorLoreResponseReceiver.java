package net.conczin.mca.client.gui.lore;

import net.conczin.mca.network.s2c.OperatorLoreResponse;

/** Client-only response target implemented by the editor and its modal confirmation screens. */
public interface OperatorLoreResponseReceiver {
    void accept(OperatorLoreResponse response);
}
