package net.conczin.mca.livingworld.voice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersistentNpcVoiceStoreTest {
    @TempDir
    Path tempDir;

    private static NpcVoiceCatalog catalog() {
        return new NpcVoiceCatalog(new NpcVoiceCatalog.VoicePools(
                List.of("child-m"), List.of("child-f"), List.of("child-n"),
                List.of("teen-m"), List.of("teen-f"), List.of("teen-n"),
                List.of("adult-m"), List.of("adult-f"), List.of("adult-n"),
                List.of("global"), "legacy"
        ));
    }

    @Test
    void persistsAndReloadsStableVoiceProfile() {
        Path file = tempDir.resolve("livingworld").resolve("voices.json");
        UUID npc = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        PersistentNpcVoiceStore first = new PersistentNpcVoiceStore(file);
        NpcVoiceProfile created = first.resolve(npc, NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.ADULT, catalog());

        PersistentNpcVoiceStore reloaded = new PersistentNpcVoiceStore(file);
        NpcVoiceProfile loaded = reloaded.resolve(npc, NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.ADULT, catalog());

        assertEquals(created, loaded);
        assertEquals("adult-f", loaded.voiceId());
        assertTrue(Files.exists(file));
    }

    @Test
    void ageBucketTransitionReassignsVoiceButStableBucketDoesNot() {
        Path file = tempDir.resolve("livingworld").resolve("voices.json");
        UUID npc = new UUID(21L, 22L);
        PersistentNpcVoiceStore store = new PersistentNpcVoiceStore(file);

        NpcVoiceProfile child = store.resolve(npc, NpcVoiceGender.MALE, NpcVoiceAgeGroup.CHILD, catalog());
        NpcVoiceProfile sameChild = store.resolve(npc, NpcVoiceGender.MALE, NpcVoiceAgeGroup.CHILD, catalog());
        NpcVoiceProfile teen = store.resolve(npc, NpcVoiceGender.MALE, NpcVoiceAgeGroup.TEEN, catalog());

        assertEquals(child, sameChild);
        assertEquals("child-m", child.voiceId());
        assertEquals("teen-m", teen.voiceId());
        assertNotEquals(child.voiceId(), teen.voiceId());
    }

    @Test
    void genderChangeReassignsToCompatiblePool() {
        Path file = tempDir.resolve("livingworld").resolve("voices.json");
        UUID npc = new UUID(31L, 32L);
        PersistentNpcVoiceStore store = new PersistentNpcVoiceStore(file);

        assertEquals("adult-m", store.resolve(npc, NpcVoiceGender.MALE, NpcVoiceAgeGroup.ADULT, catalog()).voiceId());
        assertEquals("adult-f", store.resolve(npc, NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.ADULT, catalog()).voiceId());
    }

    @Test
    void corruptFileFailsOpenAndIsReplacedOnNextResolve() throws Exception {
        Path file = tempDir.resolve("livingworld").resolve("voices.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{broken", StandardCharsets.UTF_8);

        PersistentNpcVoiceStore store = new PersistentNpcVoiceStore(file);
        NpcVoiceProfile profile = store.resolve(new UUID(41L, 42L), NpcVoiceGender.NEUTRAL, NpcVoiceAgeGroup.ADULT, catalog());

        assertEquals("adult-n", profile.voiceId());
        String repaired = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(repaired.contains("adult-n"));
        assertFalse(repaired.contains("{broken"));
    }
}
