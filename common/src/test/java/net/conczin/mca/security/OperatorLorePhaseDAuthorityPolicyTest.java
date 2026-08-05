package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorLorePhaseDAuthorityPolicyTest {
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void c2sPacketsRemainBoundedAndContainNoAuthorityOwnedIdentity() throws IOException {
        String readRequest = read(
                "common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreReadRequest.java"
        );
        String writeRequest = read(
                "common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreWriteRequest.java"
        );
        String packets = readRequest + writeRequest;

        assertTrue(readRequest.contains("int requestId"));
        assertTrue(readRequest.contains("String scope"));
        assertTrue(readRequest.contains("int villagerEntityId"));
        assertTrue(writeRequest.contains("String expectedRevision"));
        assertTrue(writeRequest.contains("OperatorLoreEditorPolicy.MAX_CODE_POINTS"));

        assertFalse(packets.contains("UUID"));
        assertFalse(packets.contains("playerId"));
        assertFalse(packets.contains("villagerUuid"));
        assertFalse(packets.contains("dimensionId"));
        assertFalse(packets.contains("villageId"));
    }

    @Test
    void minecraftAuthorityResolvesTrustedTargetBeforeThePersistenceSeam() throws IOException {
        String authority = read(
                "common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreServerAuthority.java"
        );
        String resolved = read(
                "common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreResolvedAuthority.java"
        );

        assertTrue(authority.contains("player.hasPermissions(REQUIRED_PERMISSION_LEVEL)"));
        assertTrue(authority.contains("player.serverLevel().getEntity(villagerEntityId)"));
        assertTrue(authority.contains("OperatorLoreTargetPolicy.canResolve("));
        assertTrue(authority.contains("OperatorLoreKey.player(player.getUUID())"));
        assertTrue(authority.contains("villager.level().dimension().location().toString()"));
        assertTrue(authority.contains("OperatorLoreResolvedAuthority.read("));
        assertTrue(authority.contains("OperatorLoreResolvedAuthority.write("));

        assertTrue(resolved.contains("final class OperatorLoreResolvedAuthority"));
        assertFalse(resolved.contains("public final class OperatorLoreResolvedAuthority"));
        assertFalse(resolved.contains("import net.minecraft.server.level.ServerPlayer"));
        assertFalse(resolved.contains("import net.minecraft.world.entity.Entity"));
        assertFalse(resolved.contains("import net.conczin.mca.network"));
        assertTrue(resolved.contains("key.scope() != scope"));
        assertTrue(resolved.contains("WorldOperatorLoreStore.forWorld(worldRoot)"));
        assertTrue(resolved.contains("OperatorLoreEditorPolicy.decideWrite("));
    }

    @Test
    void clientOwnsNoOperatorLoreFileStoreOrGlobalResponseMailbox() throws IOException {
        Path clientRoot = ROOT.resolve(
                "common/src/main/java/net/conczin/mca/client/gui/lore"
        );
        String clientSources;
        try (Stream<Path> files = Files.walk(clientRoot)) {
            StringBuilder combined = new StringBuilder();
            files.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> combined.append(readUnchecked(path)).append('\n'));
            clientSources = combined.toString();
        }

        String confirm = read(
                "common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreConfirmScreen.java"
        );
        String handler = read(
                "common/src/main/java/net/conczin/mca/network/ClientHandlerImpl.java"
        );

        assertFalse(clientSources.contains("WorldOperatorLoreStore"));
        assertFalse(clientSources.contains("operator-lore.json"));
        assertFalse(clientSources.contains("java.nio.file"));
        assertFalse(clientSources.contains("static OperatorLoreResponse"));

        assertTrue(confirm.contains("implements OperatorLoreResponseReceiver"));
        assertTrue(confirm.contains("owner.accept(response);"));
        assertTrue(handler.contains("Screen screen = client.screen;"));
        assertTrue(handler.contains("screen instanceof OperatorLoreResponseReceiver receiver"));
        assertTrue(handler.contains("receiver.accept(response);"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to read " + path, failure);
        }
    }
}
