package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticContradictionCorruptionTest {
    @TempDir
    Path tempDir;

    @Test
    void malformedPersistedSnapshotIsIgnoredWithoutThrowing() throws Exception {
        Path world = tempDir.resolve("malformed-world");
        Path livingWorld = world.resolve("livingworld");
        Files.createDirectories(livingWorld);

        UUID npc = id(1);
        UUID player = id(90);
        String json = """
                {
                  "version": 1,
                  "eventsByNpc": {
                    "%s": [
                      {
                        "id": "%s",
                        "ownerNpcId": "%s",
                        "type": "SEMANTIC_CONTRADICTION",
                        "summary": "Semantic contradiction recorded",
                        "participants": ["%s"],
                        "provenance": "SYSTEM_OBSERVED",
                        "gameTime": 200,
                        "createdAtEpochMillis": 0,
                        "importance": 60,
                        "emotionalWeight": 0,
                        "confidence": 100,
                        "relationshipReasons": [],
                        "semanticContradiction": {
                          "first": {
                            "logicalClaimId": "%s",
                            "detectedSemanticEntryId": "%s",
                            "kind": "FACT",
                            "provenance": "SYSTEM_OBSERVED"
                          },
                          "second": {
                            "logicalClaimId": "%s",
                            "detectedSemanticEntryId": "%s",
                            "kind": "BELIEF",
                            "provenance": "NPC_TOLD",
                            "relatedEntities": ["%s"]
                          }
                        }
                      }
                    ]
                  }
                }
                """.formatted(
                npc,
                id(500),
                npc,
                npc,
                id(100),
                id(101),
                id(200),
                id(201),
                player
        );
        Files.writeString(livingWorld.resolve("memory2.json"), json);

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> history = assertDoesNotThrow(
                () -> SemanticContradictionHistory.load(world, npc, player, 8)
        );

        assertTrue(history.isEmpty());
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
