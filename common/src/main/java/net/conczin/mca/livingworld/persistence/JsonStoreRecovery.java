package net.conczin.mca.livingworld.persistence;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Shared fail-open recovery for world-local JSON stores. */
public final class JsonStoreRecovery {
    private JsonStoreRecovery() {
    }

    public static <T> T loadOrRecover(
            Path file,
            Gson gson,
            Class<T> type,
            Predicate<T> validator,
            Supplier<T> emptyFactory
    ) {
        Path canonical = normalized(file);
        Gson safeGson = Objects.requireNonNull(gson, "gson");
        Class<T> safeType = Objects.requireNonNull(type, "type");
        Predicate<T> safeValidator = Objects.requireNonNull(validator, "validator");
        Supplier<T> safeEmptyFactory = Objects.requireNonNull(emptyFactory, "emptyFactory");
        Path temporary = temporary(canonical);

        try {
            if (Files.exists(canonical)) {
                deleteStaleTemporary(temporary);
                Optional<T> loaded = readValid(
                        canonical,
                        safeGson,
                        safeType,
                        safeValidator
                );
                if (loaded.isPresent()) {
                    return loaded.get();
                }
                moveReplacing(canonical, corruptBackup(canonical));
                T empty = requireEmpty(safeEmptyFactory);
                writeAtomic(canonical, safeGson, empty);
                return empty;
            }

            if (Files.exists(temporary)) {
                Optional<T> recovered = readValid(
                        temporary,
                        safeGson,
                        safeType,
                        safeValidator
                );
                if (recovered.isPresent()) {
                    createParent(canonical);
                    moveReplacing(temporary, canonical);
                    return recovered.get();
                }
                moveReplacing(temporary, temporaryCorruptBackup(canonical));
                T empty = requireEmpty(safeEmptyFactory);
                writeAtomic(canonical, safeGson, empty);
                return empty;
            }

            return requireEmpty(safeEmptyFactory);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to recover VillAIgence JSON store " + canonical,
                    exception
            );
        }
    }

    public static void writeAtomic(Path file, Gson gson, Object value) {
        Path canonical = normalized(file);
        Gson safeGson = Objects.requireNonNull(gson, "gson");
        Object safeValue = Objects.requireNonNull(value, "value");
        Path temporary = temporary(canonical);

        try {
            createParent(canonical);
            Files.writeString(
                    temporary,
                    safeGson.toJson(safeValue) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            moveReplacing(temporary, canonical);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to persist VillAIgence JSON store " + canonical,
                    exception
            );
        }
    }

    private static <T> Optional<T> readValid(
            Path file,
            Gson gson,
            Class<T> type,
            Predicate<T> validator
    ) {
        try {
            if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)) {
                return Optional.empty();
            }
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            T decoded = gson.fromJson(raw, type);
            return decoded != null && validator.test(decoded)
                    ? Optional.of(decoded)
                    : Optional.empty();
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static <T> T requireEmpty(Supplier<T> emptyFactory) {
        return Objects.requireNonNull(
                emptyFactory.get(),
                "emptyFactory returned null"
        );
    }

    private static Path normalized(Path file) {
        return Objects.requireNonNull(file, "file")
                .toAbsolutePath()
                .normalize();
    }

    private static Path temporary(Path canonical) {
        return canonical.resolveSibling(canonical.getFileName() + ".tmp");
    }

    private static Path corruptBackup(Path canonical) {
        return canonical.resolveSibling(canonical.getFileName() + ".corrupt");
    }

    private static Path temporaryCorruptBackup(Path canonical) {
        return canonical.resolveSibling(canonical.getFileName() + ".tmp.corrupt");
    }

    private static void deleteStaleTemporary(Path temporary) throws IOException {
        if (Files.exists(temporary)) {
            Files.delete(temporary);
        }
    }

    private static void createParent(Path canonical) throws IOException {
        Path parent = canonical.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        createParent(target);
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
