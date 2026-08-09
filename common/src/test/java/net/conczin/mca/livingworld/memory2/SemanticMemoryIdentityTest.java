package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SemanticMemoryIdentityTest {
    private static final UUID NPC = id(1);
    private static final UUID PLAYER_A = id(2);
    private static final UUID PLAYER_B = id(3);

    @Test
    void logicalClaimIdentityIgnoresSourceEvidenceAndCanonicalizesStatementAndScope() {
        SemanticMemoryEntry first = belief(
                id(10),
                "  The\tBRIDGE   is ＯＰＥＮ ",
                List.of(PLAYER_B, PLAYER_A, PLAYER_B),
                MemoryEvent.Provenance.NPC_TOLD,
                List.of(id(100))
        );
        SemanticMemoryEntry second = belief(
                id(11),
                "the bridge is OPEN",
                List.of(PLAYER_A, PLAYER_B),
                MemoryEvent.Provenance.NPC_TOLD,
                List.of(id(101), id(102))
        );

        assertEquals(SemanticMemoryIdentity.key(first), SemanticMemoryIdentity.key(second));
        assertEquals(SemanticMemoryIdentity.logicalClaimId(first), SemanticMemoryIdentity.logicalClaimId(second));
        assertEquals("the bridge is open", SemanticMemoryIdentity.canonicalStatement(first.statement()));
        assertEquals(sorted(PLAYER_A, PLAYER_B), SemanticMemoryIdentity.canonicalIds(first.relatedEntities()));
    }

    @Test
    void truthKindAndProvenanceRemainPartOfLogicalIdentity() {
        SemanticMemoryEntry npcBelief = belief(
                id(20), "Gate closed", List.of(PLAYER_A), MemoryEvent.Provenance.NPC_TOLD, List.of(id(120)));
        SemanticMemoryEntry playerBelief = belief(
                id(21), "Gate closed", List.of(PLAYER_A), MemoryEvent.Provenance.PLAYER_TOLD, List.of(id(121)));
        SemanticMemoryEntry fact = new SemanticMemoryEntry(
                id(22), NPC, SemanticMemoryEntry.Kind.FACT, "Gate closed", List.of(PLAYER_A),
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 10L, 0L, 80, 100, List.of(id(122)));

        assertNotEquals(SemanticMemoryIdentity.logicalClaimId(npcBelief),
                SemanticMemoryIdentity.logicalClaimId(playerBelief));
        assertNotEquals(SemanticMemoryIdentity.logicalClaimId(npcBelief),
                SemanticMemoryIdentity.logicalClaimId(fact));
    }

    @Test
    void consolidatorKeepsExistingDeterministicIdContract() {
        SemanticMemoryEntry first = belief(
                id(30), "Bridge Open", List.of(PLAYER_B, PLAYER_A),
                MemoryEvent.Provenance.NPC_TOLD, List.of(id(130)));
        SemanticMemoryEntry second = belief(
                id(31), "bridge open", List.of(PLAYER_A, PLAYER_B),
                MemoryEvent.Provenance.NPC_TOLD, List.of(id(131)));

        SemanticMemoryEntry merged = SemanticMemoryConsolidator.merge(first, second).orElseThrow();
        String canonicalStatement = canonicalStatementPreRefactor(first.statement());
        StringBuilder canonical = new StringBuilder("semantic-consolidated-v1")
                .append('\n').append(NPC)
                .append('\n').append(SemanticMemoryEntry.Kind.BELIEF)
                .append('\n').append(MemoryEvent.Provenance.NPC_TOLD)
                .append('\n').append(canonicalStatement);
        for (UUID related : sorted(PLAYER_A, PLAYER_B)) canonical.append('\n').append(related);
        UUID expected = UUID.nameUUIDFromBytes(canonical.toString().getBytes(StandardCharsets.UTF_8));

        assertEquals(expected, merged.id());
    }

    private static SemanticMemoryEntry belief(
            UUID entryId,
            String statement,
            List<UUID> scope,
            MemoryEvent.Provenance provenance,
            List<UUID> sources
    ) {
        return new SemanticMemoryEntry(
                entryId,
                NPC,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                scope,
                provenance,
                10L,
                0L,
                50,
                50,
                sources
        );
    }

    private static String canonicalStatementPreRefactor(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC);
        StringBuilder output = new StringBuilder(normalized.length());
        boolean previousWhitespace = false;
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            offset += Character.charCount(codePoint);
            boolean whitespace = Character.isWhitespace(codePoint) || Character.isISOControl(codePoint);
            if (whitespace) {
                if (!previousWhitespace && output.length() > 0) output.append(' ');
                previousWhitespace = true;
            } else {
                output.appendCodePoint(codePoint);
                previousWhitespace = false;
            }
        }
        return output.toString().strip().toLowerCase(Locale.ROOT);
    }

    private static List<UUID> sorted(UUID... values) {
        List<UUID> result = new ArrayList<>(List.of(values));
        result.sort(Comparator.comparing(UUID::toString));
        return List.copyOf(result);
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
