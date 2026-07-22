package net.conczin.mca.livingworld.voice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** World-local persistent NPC voice identities. */
public final class PersistentNpcVoiceStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ConcurrentMap<Path, PersistentNpcVoiceStore> STORES = new ConcurrentHashMap<>();

    private final Path file;
    private VoiceFile data;

    public static PersistentNpcVoiceStore forWorld(Path worldRoot) {
        Path file = worldRoot.toAbsolutePath().normalize().resolve("livingworld").resolve("voices.json");
        return STORES.computeIfAbsent(file, PersistentNpcVoiceStore::new);
    }

    PersistentNpcVoiceStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
        this.data = loadFailOpen();
    }

    public synchronized NpcVoiceProfile resolve(
            UUID npcId,
            NpcVoiceGender gender,
            NpcVoiceAgeGroup ageGroup,
            NpcVoiceCatalog catalog
    ) {
        String key = npcId.toString();
        NpcVoiceProfile existing = data.profiles.get(key);
        if (existing != null
                && existing.npcId().equals(npcId)
                && existing.gender() == gender
                && existing.ageGroup() == ageGroup
                && catalog.isEligible(existing.voiceId(), gender, ageGroup)) {
            return existing;
        }

        NpcVoiceProfile assigned = new NpcVoiceProfile(npcId, gender, ageGroup, catalog.select(npcId, gender, ageGroup));
        data.profiles.put(key, assigned);
        save();
        return assigned;
    }

    private VoiceFile loadFailOpen() {
        if (!Files.exists(file)) return new VoiceFile();
        try {
            VoiceFile loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), VoiceFile.class);
            if (loaded == null || loaded.version != FORMAT_VERSION || loaded.profiles == null) return new VoiceFile();
            loaded.profiles = new HashMap<>(loaded.profiles);
            loaded.profiles.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
            return loaded;
        } catch (IOException | RuntimeException ignored) {
            return new VoiceFile();
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(
                    temp,
                    GSON.toJson(data),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Voice persistence is fail-open: TTS may continue with the resolved deterministic profile.
        }
    }

    private static final class VoiceFile {
        int version = FORMAT_VERSION;
        Map<String, NpcVoiceProfile> profiles = new HashMap<>();
    }
}
