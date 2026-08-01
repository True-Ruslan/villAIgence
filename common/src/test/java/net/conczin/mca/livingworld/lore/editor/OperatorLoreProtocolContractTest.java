package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.livingworld.lore.OperatorLoreScope;
import net.conczin.mca.network.c2s.OperatorLoreReadRequest;
import net.conczin.mca.network.c2s.OperatorLoreWriteRequest;
import net.conczin.mca.network.s2c.OperatorLoreResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperatorLoreProtocolContractTest {
    @Test
    void requestIdIsPreservedAcrossReadWriteAndResponseModels() {
        OperatorLoreReadRequest read = new OperatorLoreReadRequest(41, "WORLD", -1);
        OperatorLoreWriteRequest write = new OperatorLoreWriteRequest(
                42,
                "PLAYER",
                -1,
                "a".repeat(OperatorLoreEditorPolicy.REVISION_HEX_LENGTH),
                "Lore"
        );
        OperatorLoreEditorResult result = new OperatorLoreEditorResult(
                OperatorLoreScope.WORLD,
                -1,
                OperatorLoreEditorResult.Status.OK,
                "Lore",
                OperatorLoreEditorPolicy.revision("Lore")
        );
        OperatorLoreResponse response = new OperatorLoreResponse(43, result);

        assertEquals(41, read.requestId());
        assertEquals(42, write.requestId());
        assertEquals(43, response.requestId());
        assertEquals(result, response.toResult());
    }
}
