package net.conczin.mca.livingworld.voice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.conczin.mca.livingworld.persistence.GsonJsonStoreCodec;
import net.conczin.mca.livingworld.persistence.JsonStoreRecovery;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** World-local persistent NPC voice identities. */
public final class PersistentNpcVoiceStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonStoreRecovery.Codec<VoiceFile> CODEC =
            new GsonJsonStoreCodec<>(GSON, VoiceFile.class);
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
        try {
            VoiceFile loaded = JsonStoreRecovery.loadOrRecover(
                    file,
                    CODEC,
                    value -> value != null
                            && value.version == FORMAT_VERSION
                            && value.profiles != null,
                    VoiceFile::new
            );
            loaded.profiles = new HashMap<>(loaded.profiles);
            loaded.profiles.entrySet().removeIf(
                    entry -> entry.getKey() == null || entry.getValue() == null
            );
            return loaded;
        } catch (UncheckedIOException ignored) {
            return new VoiceFile();
        }
    }

    private void save() {
        try {
            JsonStoreRecovery.writeAtomic(file, CODEC, data);
        } catch (UncheckedIOException ignored) {
            // Voice persistence is fail-open: TTS may continue with the resolved deterministic profile.
        }
    }

    private static final class VoiceFile {
        int version = FORMAT_VERSION;
        Map<String, NpcVoiceProfile> profiles = new HashMap<>();
    }
}
