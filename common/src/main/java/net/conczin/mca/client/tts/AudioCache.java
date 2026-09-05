package net.conczin.mca.client.tts;

import net.conczin.mca.MCA;
import net.conczin.mca.client.tts.sound.PCMAudioStream;
import net.conczin.mca.util.MaxSizeHashMap;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class AudioCache {
    private static final int MIN_SIZE = 128;
    private static final int MAX_IN_MEMORY_ENTRIES = 64;
    private static final String CACHE_DIR = "tts_cache/";
    public static Map<String, PCMAudioStream> inMemory = Collections.synchronizedMap(new MaxSizeHashMap<>(MAX_IN_MEMORY_ENTRIES));

    private static void setInMemoryAudio(String identifier, ByteBuffer buffer) {
        // Collections.synchronizedMap only synchronizes individual calls, so a compound
        // check-then-act needs to synchronize on the map itself to stay race-free.
        synchronized (inMemory) {
            PCMAudioStream existing = inMemory.get(identifier);
            if (existing != null) {
                existing.setBuffer(buffer);
            } else {
                inMemory.put(identifier, new PCMAudioStream(buffer));
            }
        }
    }

    public static PCMAudioStream getPCMAudioStream(String identifier) {
        PCMAudioStream cached = inMemory.get(identifier);
        return cached != null ? cached : new PCMAudioStream(readFromDisk(identifier));
    }

    public static boolean get(String identifier, Consumer<OutputStream> retriever, boolean persistent) {
        if (persistent) {
            return cachedRetrieve(identifier, retriever);
        } else {
            ByteBuffer byteBuffer = retrieve(retriever);
            if (byteBuffer == null) {
                return false;
            } else {
                AudioCache.setInMemoryAudio(identifier, byteBuffer);
                return true;
            }
        }
    }

    private static ByteBuffer retrieve(Consumer<OutputStream> retriever) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            retriever.accept(baos);
            return ByteBuffer.wrap(baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    public static ByteBuffer readFromDisk(String identifier) {
        File cacheFile = new File(CACHE_DIR, identifier);
        if (!isSane(cacheFile)) return null;

        try (FileInputStream fis = new FileInputStream(cacheFile)) {
            return ByteBuffer.wrap(fis.readAllBytes());
        } catch (IOException e) {
            MCA.LOGGER.error("Failed to retrieve cached audio file: {}", identifier, e);
            return null;
        }
    }

    public static boolean cachedRetrieve(String identifier, Consumer<OutputStream> retriever) {
        try {
            File cacheFile = new File(CACHE_DIR, identifier);
            if (isSane(cacheFile)) {
                return true;
            } else {
                //noinspection ResultOfMethodCallIgnored
                cacheFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                    retriever.accept(fos);
                }
                return isSane(cacheFile);
            }
        } catch (IOException e) {
            MCA.LOGGER.error("Failed to cache audio file: {}", identifier, e);
            return false;
        }
    }

    private static boolean isSane(File cacheFile) {
        return cacheFile.exists() && cacheFile.length() > MIN_SIZE;
    }

    public static String getHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(text.getBytes())).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format(Locale.ROOT, "%0" + (bytes.length << 1) + "X", bi);
    }
}
