package net.conczin.mca.livingworld.persistence;

import net.conczin.mca.livingworld.lore.OperatorLoreKey;
import net.conczin.mca.livingworld.lore.WorldOperatorLoreStore;
import net.conczin.mca.livingworld.memory2.MemoryEventStore;
import net.conczin.mca.livingworld.memory2.SemanticMemoryStore;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import net.conczin.mca.livingworld.voice.NpcVoiceAgeGroup;
import net.conczin.mca.livingworld.voice.NpcVoiceCatalog;
import net.conczin.mca.livingworld.voice.NpcVoiceGender;
import net.conczin.mca.livingworld.voice.PersistentNpcVoiceStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentStoreRecoveryIntegrationTest {
    private static final UUID NPC_ID = UUID.fromString(
            "dc3145cb-7921-48e8-a00d-f951a4c80fa0"
    );
    private static final UUID PLAYER_ID = UUID.fromString(
            "d2932adb-aa33-4c7f-8170-b98f38323311"
    );

    @TempDir
    Path worldRoot;

    @Test
    void allProductionStoresBackUpMalformedBytesAndRegenerateCanonicalJson()
            throws IOException {
        Path livingWorld = worldRoot.resolve("livingworld");
        Files.createDirectories(livingWorld);
        Map<String, byte[]> corruptByStore = new LinkedHashMap<>();
        corruptByStore.put("memory2.json", new byte[0]);
        corruptByStore.put("semantic-memory.json", bytes("[]"));
        corruptByStore.put(
                "relationships.json",
                bytes("{\"version\":2,\"relationships\":{}}")
        );
        corruptByStore.put("voices.json", bytes("null"));
        corruptByStore.put(
                "operator-lore.json",
                bytes("{\"version\":1,\"world\":[],\"villagers\":{}}")
        );
        for (Map.Entry<String, byte[]> entry : corruptByStore.entrySet()) {
            Files.write(livingWorld.resolve(entry.getKey()), entry.getValue());
        }

        MemoryEventStore.forWorld(worldRoot).getRecent(NPC_ID, 8);
        SemanticMemoryStore.forWorld(worldRoot).getRecent(NPC_ID, 8);
        LivingWorldRelationshipStore.forWorld(worldRoot).get(NPC_ID, PLAYER_ID);
        PersistentNpcVoiceStore.forWorld(worldRoot).resolve(
                NPC_ID,
                NpcVoiceGender.NEUTRAL,
                NpcVoiceAgeGroup.ADULT,
                fixtureCatalog()
        );
        WorldOperatorLoreStore.forWorld(worldRoot).get(OperatorLoreKey.world());

        for (Map.Entry<String, byte[]> entry : corruptByStore.entrySet()) {
            Path canonical = livingWorld.resolve(entry.getKey());
            Path backup = canonical.resolveSibling(canonical.getFileName() + ".corrupt");
            assertArrayEquals(
                    entry.getValue(),
                    Files.readAllBytes(backup),
                    "Backup must preserve exact corrupt bytes for " + entry.getKey()
            );
            String regenerated = Files.readString(canonical).stripLeading();
            assertTrue(
                    regenerated.startsWith("{"),
                    "Canonical store must be a JSON object after recovery: " + entry.getKey()
            );
            assertTrue(
                    regenerated.contains("\"version\""),
                    "Canonical store must declare a schema version: " + entry.getKey()
            );
        }
    }

    private static NpcVoiceCatalog fixtureCatalog() {
        return new NpcVoiceCatalog(new NpcVoiceCatalog.VoicePools(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("recovery-fixture-voice"),
                ""
        ));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
